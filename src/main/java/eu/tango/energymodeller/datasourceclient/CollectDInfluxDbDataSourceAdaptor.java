/**
 * Copyright 2017 University of Leeds
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This is being developed for the TANGO Project: http://tango-project.eu
 *
 */
package eu.tango.energymodeller.datasourceclient;

import eu.ascetic.ioutils.io.Settings;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;

/**
 * This data source adaptor connects directly into a collectd database.
 *
 * @author Richard Kavanagh
 */
public class CollectDInfluxDbDataSourceAdaptor implements HostDataSource {

    private final Settings settings = new Settings(CONFIG_FILE);
    private static final String CONFIG_FILE = "energy-modeller-influx-db-config.properties";
    private final String hostname;
    private final String user;
    private final String password;
    private final String dbName;

    private final InfluxDB influxDB;

    public CollectDInfluxDbDataSourceAdaptor() {
        dbName = settings.getString("energy.modeller.influx.db.name", "collectd");
        user = settings.getString("energy.modeller.influx.db.user", "");
        password = settings.getString("energy.modeller.influx.db.password", "");
        if (settings.isChanged()) {
            settings.save(CONFIG_FILE);
        }
        hostname = settings.getString("energy.modeller.influx.db.hostname", "http://ns54.bullx:8086");
        influxDB = InfluxDBFactory.connect(hostname, user, password);
    }

    /**
     * Creates a new CollectD (via InfluxDB) data source adaptor).
     * @param hostname The hostname of the database to connect to
     * @param user The username for the database
     * @param password The password
     * @param dbName The database to connect to
     */
    public CollectDInfluxDbDataSourceAdaptor(String hostname, String user, String password, String dbName) {
        this.hostname = hostname;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
        influxDB = InfluxDBFactory.connect(hostname, user, password);
    }

    @Override
    public Host getHostByName(String hostname) {
        HashMap<String, Host> knownHosts = getHostListAsHashMap();
        if (knownHosts.containsKey(hostname)) {
            return knownHosts.get(hostname);
        } else {
            return null;
        }
    }

    @Override
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname) {
        Host host = getHostListAsHashMap().get(hostname);
        if (host == null) {
            return null;
        }
        return new GeneralPurposePowerConsumer(host.getId(), host.getHostName());
    }

    @Override
    public VmDeployed getVmByName(String name) {
        Host host = getHostListAsHashMap().get(name);
        if (host == null) {
            return null;
        }
        return new VmDeployed(host.getId(), host.getHostName());
    }

    @Override
    public List<Host> getHostList() {
        HashMap<String, Host> knownHosts = getHostListAsHashMap();
        return new ArrayList<>(knownHosts.values());
    }

    private HashMap<String, Host> getHostListAsHashMap() {
        HashMap<String, Host> knownHosts = new HashMap<>();
        QueryResult results = runQuery("SHOW TAG VALUES WITH KEY=host;");
        for (QueryResult.Result result : results.getResults()) {
            for (QueryResult.Series series : result.getSeries()) {
                for (List<Object> value : series.getValues()) {
                    if (!knownHosts.containsKey((String) value.get(1))) {
                        String hostId = ((String) value.get(1)).replaceAll("[^0-9]", "");
                        Host host = new Host(Integer.parseInt(hostId), (String) value.get(1));
                        knownHosts.put((String) value.get(1), host);
                    }
                }
            }
        }
        return knownHosts;
    }

    @Override
    public List<EnergyUsageSource> getHostAndVmList() {
        List<EnergyUsageSource> answer = new ArrayList<>();
        for (Host host : getHostListAsHashMap().values()) {
            answer.add(host);
        }
        return answer;
    }

    @Override
    public List<GeneralPurposePowerConsumer> getGeneralPowerConsumerList() {
        return new ArrayList<>();
    }

    @Override
    public List<VmDeployed> getVmList() {
        return new ArrayList<>();
    }

    @Override
    public List<ApplicationOnHost> getHostApplicationList(ApplicationOnHost.JOB_STATUS state) {
        return new ArrayList<>();
    }

    @Override
    public List<ApplicationOnHost> getHostApplicationList() {
        return new ArrayList<>();
    }

    @Override
    public HostMeasurement getHostData(Host host) {
        HostMeasurement answer;
        String listMeasurements = "";
        ArrayList<String> measurements = getMeasurements();
        for (String measurement : measurements) {
            if (listMeasurements.isEmpty()) {
                listMeasurements = measurement;
            } else {
                listMeasurements = listMeasurements + ", " + measurement;
            }
        }
        QueryResult results = runQuery("SELECT last(value),type_instance, instance FROM " + listMeasurements + " WHERE host = '" + host.getHostName() + "'  GROUP BY instance, type_instance;");
        answer = convertToHostMeasurement(host, results);
        return answer;
    }

    /**
     * This takes a query result from the data source and converts it into a
     * host measurement.
     *
     * @param host The host to convert the data for
     * @param results THe result set to convert the data for
     * @return The host measurement
     */
    private HostMeasurement convertToHostMeasurement(Host host, QueryResult results) {
        if (results == null) {
            return null;
        }
        HostMeasurement answer = new HostMeasurement(host);
        double acceleratorPowerUsed = 0.0;
        for (QueryResult.Result result : results.getResults()) {
            if (result == null || result.getSeries() == null) {
                return null;
            }
            for (QueryResult.Series series : result.getSeries()) {
                if (series == null || series.getValues() == null) {
                    return null;
                }
                for (List<Object> value : series.getValues()) {
                    Instant time = Instant.parse((String) value.get(0));
                    String metricName = series.getName() + ":" + value.get(2);
                    if (value.size() == 4) {
                        metricName = metricName + ":" + value.get(3);
                    }
                    if (metricName.equals("power_value:estimated:estimated")) {
                        MetricValue estimatedPower = new MetricValue(KpiList.ESTIMATED_POWER_KPI_NAME, KpiList.ESTIMATED_POWER_KPI_NAME, value.get(1).toString(), time.getEpochSecond());
                        answer.addMetric(estimatedPower);
                    }
                    /**
                     * This counts up all power consumed and reported by the
                     * monitoring infrastructure usually in the format:
                     * monitoring_value:0:nvidia (i.e. card 1)
                     * monitoring_value:1:nvidia (and card 2)
                     */
                    try {
                        if (metricName.contains("monitoring_value:")) {
                            acceleratorPowerUsed = acceleratorPowerUsed + Double.parseDouble(value.get(1).toString());
                        }
                    } catch (NumberFormatException ex) {
                        Logger.getLogger(CollectDInfluxDbDataSourceAdaptor.class.getName()).log(Level.WARNING, "Parsing input from collectd failed", ex);
                    }
                    MetricValue metric = new MetricValue(metricName, metricName, value.get(1).toString(), time.getEpochSecond());
                    answer.addMetric(metric);
                    if (time.getEpochSecond() > answer.getClock()) {
                        answer.setClock(time.getEpochSecond());
                    }
                }
            }
        }
        if (acceleratorPowerUsed > 0) {
            MetricValue metric = new MetricValue(KpiList.ACCELERATOR_POWER_USED, KpiList.ACCELERATOR_POWER_USED, Double.toString(acceleratorPowerUsed), answer.getClock());
            answer.addMetric(metric);
        }
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData() {
        return getHostData(getHostList());
    }

    /**
     * This lists which metrics are available.
     */
    private ArrayList<String> getMeasurements() {
        ArrayList<String> answer = new ArrayList<>();
        QueryResult results = runQuery("show measurements");
        for (QueryResult.Result result : results.getResults()) {
            for (QueryResult.Series series : result.getSeries()) {
                for (List<Object> value : series.getValues()) {
                    answer.add((String) value.get(0));
                }
            }
        }
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData(List<Host> hostList) {
        ArrayList<HostMeasurement> answer = new ArrayList<>();
        for (Host host : hostList) {
            answer.add(getHostData(host));
        }
        return answer;
    }

    @Override
    public VmMeasurement getVmData(VmDeployed vm) {
        return null; //VMs are not currently handled by this data source adaptor.
    }

    @Override
    public List<VmMeasurement> getVmData() {
        return new ArrayList<>(); //VMs are not currently handled by this data source adaptor.
    }

    @Override
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList) {
        return new ArrayList<>(); //VMs are not currently handled by this data source adaptor.
    }

    @Override
    public CurrentUsageRecord getCurrentEnergyUsage(Host host) {
        QueryResult results = runQuery("SELECT last(value) FROM power_value WHERE host = '" + host.getHostName() + "';");
        double currentPower = getSingleValueOut(results);
        return new CurrentUsageRecord(host, currentPower);
    }

    @Override
    public double getLowestHostPowerUsage(Host host) {
        QueryResult results = runQuery("SELECT min(value) FROM power_value WHERE host = '" + host.getHostName() + "'");
        return getSingleValueOut(results);
    }

    @Override
    public double getHighestHostPowerUsage(Host host) {
        QueryResult results = runQuery("SELECT max(value) FROM power_value WHERE host = '" + host.getHostName() + "';");
        return getSingleValueOut(results);
    }

    @Override
    public double getCpuUtilisation(Host host, int durationSeconds) {
        /**
         * An example output of the query result looks like:
         * {"results":[{"series":[{"name":"cpu","columns":["time","value"],
         * "values":[["2015-06-06T14:55:27.195Z",90],["2015-06-06T14:56:24.556Z",90]]}]}]}
         * {"results":[{"series":[{"name":"databases","columns":["name"],"values":[["mydb"]]}]}]}
         */
        QueryResult results = runQuery("SELECT mean(value) FROM cpu_value WHERE host = '" + host.getHostName() + "' AND type_instance = 'idle' AND time > now() - " + durationSeconds + "s");
        if (isQueryResultEmpty(results)) {
            return 0.0; //Not enough data to know therefore assume zero usage.
        }
        BigDecimal answer = BigDecimal.valueOf(1 - ((getAverage(results)) / 100));
        answer.setScale(2, BigDecimal.ROUND_HALF_UP);
        return answer.doubleValue();
    }

    /**
     * This checks to see if the returned result is empty or not
     * @param results The query result to test for emptiness
     * @return If the query result is empty or not
     */
    private boolean isQueryResultEmpty(QueryResult results) {
        if (results.getResults() == null || results.getResults().isEmpty()) {
            return true;
        }
        QueryResult.Result result = results.getResults().get(0);
        if (result.getSeries() == null || result.getSeries().isEmpty()) {
            return true;
        }
        QueryResult.Series series = result.getSeries().get(0);
        return (series.getValues() == null || series.getValues().isEmpty());
    }

    /**
     * This parses the result of a query that provides a single result.
     *
     * @param results The result object to parse
     * @return The single value returned from the query.
     */
    private double getSingleValueOut(QueryResult results) {
        if (results.getResults() == null || results.getResults().isEmpty()) {
            return 0.0;
        }
        QueryResult.Result result = results.getResults().get(0);
        if (result.getSeries() == null || result.getSeries().isEmpty()) {
            return 0.0;
        }
        QueryResult.Series series = result.getSeries().get(0);
        if (series.getValues() == null || series.getValues().isEmpty()) {
            return 0.0;
        }
        List<Object> value = series.getValues().get(0);
        return (Double) value.get(1);
    }

    /**
     * This parses the result of a query that provides the average (such as for
     * CPU utilisation).
     *
     * @param results The result object to parse
     * @return The average value returned from the query.
     */
    private double getAverage(QueryResult results) {
        double total = 0.0;
        double count = 0;
        for (QueryResult.Result result : results.getResults()) {
            if (result.getSeries() == null || result.getSeries().isEmpty()) {
                return 0.0;
            }
            for (QueryResult.Series series : result.getSeries()) {
                for (List<Object> value : series.getValues()) {
                    count = count + 1;
                    total = total + (Double) value.get(1);
                }
            }
        }
        if (count == 0) {
            return 0;
        }
        return total / count;
    }

    /**
     * Runs the query against the influxdb database
     *
     * @param queryStr The string representation of the query
     * @return The query's result set
     */
    private QueryResult runQuery(String queryStr) {
        Query query = new Query(queryStr, dbName);
        return influxDB.query(query);
    }

    /**
     * This writes the log data out directly to influx db
     * @param app The application to write the data out for
     * @param power The power consumption information to write out
     */
    public void writeOutApplicationValuesToInflux(ApplicationOnHost app, double power) {

        BatchPoints batchPoints = BatchPoints
                .database(dbName)
                .tag("async", "true")
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        Point dataPoint = Point.measurement("app_power")
                .tag("type_instance", app.getName())
                .tag("type", app.getId() + "")
                .tag("host", app.getAllocatedTo().getHostName() + ".bullx") //TODO Note fix here copes with name differences between sources.
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("value", power)
                .build();
        batchPoints.point(dataPoint);
        influxDB.write(batchPoints);
    }

}
