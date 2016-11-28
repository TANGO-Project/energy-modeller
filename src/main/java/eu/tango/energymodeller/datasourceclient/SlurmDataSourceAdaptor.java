/**
 * Copyright 2014 University of Leeds
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
 */
package eu.tango.energymodeller.datasourceclient;

import eu.ascetic.ioutils.io.Settings;
import static eu.tango.energymodeller.datasourceclient.KpiList.POWER_KPI_NAME;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * This requests information for SLURM via the command "scontrol show node="
 *
 * @author Richard Kavanagh
 */
public class SlurmDataSourceAdaptor implements HostDataSource {

    private Tailer tailer;
    private final HashMap<String, Host> hosts = new HashMap<>();
    private final HashMap<String, HostMeasurement> lowest = new HashMap<>();
    private final HashMap<String, HostMeasurement> highest = new HashMap<>();
    private final HashMap<String, HostMeasurement> current = new HashMap<>();
    private SlurmDataSourceAdaptor.SlurmTailer fileTailer;
    private final Settings settings = new Settings("energy-modeller-slurm-config.properties");
    private final LinkedList<SlurmDataSourceAdaptor.CPUUtilisation> cpuMeasure = new LinkedList<>();

    public SlurmDataSourceAdaptor() {
        startup(1);
    }

    /**
     * This is the generic startup code for the WattsUp data source adaptor
     *
     * @param interval The interval at which to take logging data.
     */
    public final void startup(int interval) {
        String filename = settings.getString("energy.modeller.slurm.scrape.file", "slurm-host-data.log");
        if (settings.isChanged()) {
            settings.save("energy-modeller-slurm-config.properties");
        }
        File scrapeFile = new File(filename);
        fileTailer = new SlurmDataSourceAdaptor.SlurmTailer();
        tailer = Tailer.create(scrapeFile, fileTailer, (interval * 1000) / 16, true);
        Thread tailerThread = new Thread(tailer);
        tailerThread.setDaemon(true);
        tailerThread.start();
        System.out.println("Scraping from Slurm output file");
    }

    @Override
    public Host getHostByName(String hostname) {
        return hosts.get(hostname);
    }

    @Override
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname) {
        Host host = hosts.get(hostname);
        if (host == null) {
            return null;
        }
        GeneralPurposePowerConsumer answer = new GeneralPurposePowerConsumer(host.getId(), host.getHostName());
        return answer;
    }

    @Override
    public VmDeployed getVmByName(String name) {
        Host host = hosts.get(name);
        if (host == null) {
            return null;
        }
        VmDeployed answer = new VmDeployed(host.getId(), host.getHostName());
        return answer;
    }

    @Override
    public List<Host> getHostList() {
        ArrayList<Host> answer = new ArrayList<>();
        answer.addAll(hosts.values());
        return answer;
    }

    @Override
    public List<EnergyUsageSource> getHostAndVmList() {
        ArrayList<EnergyUsageSource> answer = new ArrayList<>();
        answer.addAll(hosts.values());
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
    public HostMeasurement getHostData(Host host) {
        return current.get(host.getHostName());
    }

    @Override
    public List<HostMeasurement> getHostData() {
        ArrayList<HostMeasurement> answer = new ArrayList<>();
        answer.addAll(current.values());
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData(List<Host> hostList) {
        if (hostList == null) {
            return getHostData();
        }
        ArrayList<HostMeasurement> answer = new ArrayList<>();
        for (Host host : hostList) {
            HostMeasurement value = current.get(host.getHostName());
            if (value != null) {
                answer.add(value);
            }
        }
        return answer;
    }

    @Override
    public VmMeasurement getVmData(VmDeployed vm) {
        for (HostMeasurement measure : current.values()) {
            if (measure.getHost().getHostName().equals(vm.getName())) {
                VmMeasurement answer = new VmMeasurement(vm, measure.getClock());
                answer.setMetrics(measure.getMetrics());
                return answer;
            }
        }
        return null;
    }

    @Override
    public List<VmMeasurement> getVmData() {
        ArrayList<VmMeasurement> answer = new ArrayList<>();
        for (HostMeasurement measure : current.values()) {
            VmMeasurement vmData = new VmMeasurement(
                    getVmByName(measure.getHost().getHostName()),
                    measure.getClock());
            vmData.setMetrics(measure.getMetrics());
            answer.add(vmData);
        }
        return answer;
    }

    @Override
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList) {
        if (vmList == null) {
            return getVmData();
        }
        ArrayList<VmMeasurement> answer = new ArrayList<>();
        for (VmDeployed vm : vmList) {

            HostMeasurement measure = current.get(vm.getName());
            VmMeasurement vmData = new VmMeasurement(
                    getVmByName(measure.getHost().getHostName()),
                    measure.getClock());
            vmData.setMetrics(measure.getMetrics());
            answer.add(vmData);
        }
        return answer;
    }

    @Override
    public CurrentUsageRecord getCurrentEnergyUsage(Host host) {
        CurrentUsageRecord answer = new CurrentUsageRecord(host);
        HostMeasurement measurement = getHostData(host);
        answer.setPower(measurement.getMetric(POWER_KPI_NAME).getValue());
        return answer;
    }

    @Override
    public double getLowestHostPowerUsage(Host host) {
        return lowest.get(host.getHostName()).getPower();
    }

    @Override
    public double getHighestHostPowerUsage(Host host) {
        return highest.get(host.getHostName()).getPower();
    }

    @Override
    public double getCpuUtilisation(Host host, int durationSeconds) {
        //TODO note duration in seconds is ignored, fix this.
        return current.get(host.getHostName()).getCpuUtilisation();
    }

    /**
     * Given the key value of the adaption detail this returns its value.
     *
     * @param key The key name for the actuation parameter
     * @param parseString The string to split and get the property value from
     * @return The value of the adaptation detail else null.
     */
    public static String getValue(String key, String parseString) {
        String[] args = parseString.split(";");
        for (String arg : args) {
            if (arg.split("=")[0].trim().equalsIgnoreCase(key)) {
                return arg.split("=")[1].trim();
            }
        }
        return null;
    }

    /**
     * Given the key value of the adaption detail this returns its value.
     *
     * @param key The key name for the actuation parameter
     * @param parseString The string to split and get the property value from
     * @return The value of the adaptation detail else null.
     */
    public static String getValue(String key, String[] parseString) {
        for (String arg : parseString) {
            if (arg.split("=")[0].trim().equalsIgnoreCase(key)) {
                return arg.split("=")[1].trim();
            }
        }
        return null;
    }

    /**
     * In the event the WattsUp Meter port is captured already and the output is
     * being placed in a log file this can read the file and get the required
     * data.
     */
    private class SlurmTailer extends TailerListenerAdapter {

        @Override
        public void handle(String line) {
            try {
                boolean valid = true;
                GregorianCalendar calander = new GregorianCalendar();
                long clock = TimeUnit.MILLISECONDS.toSeconds(calander.getTimeInMillis());
                String[] values = line.split(";");
                String watts = getValue("CurrentWatts", values);
                String wattskwh = getValue("ConsumedJoules", values);
                String hostname = getValue("NodeName", values);
                String hostId = hostname.replaceAll("[^0-9]", "");
                cpuMeasure.add(new SlurmDataSourceAdaptor.CPUUtilisation(clock, hostname, (Double.valueOf(getValue("CPULoad", values))) * 100));
                Host host = getHostByName(hostname);

                //Check for need to disover host
                if (host == null) {
                    host = new Host(Integer.parseInt(hostId), hostname);
                    hosts.put(hostname, host);
                }

                HostMeasurement measurement = new HostMeasurement(host, clock);
                measurement.addMetric(new MetricValue(KpiList.POWER_KPI_NAME, KpiList.POWER_KPI_NAME, watts, clock));
                measurement.addMetric(new MetricValue(KpiList.ENERGY_KPI_NAME, KpiList.ENERGY_KPI_NAME, wattskwh, clock));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.CPU_SPOT_USAGE_KPI_NAME, KpiList.CPU_SPOT_USAGE_KPI_NAME, (Double.valueOf(getValue("CPULoad", values))) * 100 + "", clock));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.CPU_IDLE_KPI_NAME, KpiList.CPU_IDLE_KPI_NAME, ((1 - Double.valueOf(getValue("CPULoad", values)))) * 100 + "", clock));
                if (!valid) {
                    System.out.println("The measurement taken was invalid");
                    return;
                }

                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.MEMORY_AVAILABLE_KPI_NAME, KpiList.MEMORY_AVAILABLE_KPI_NAME, (int) (Double.valueOf(getValue("FreeMem", values)) / 1048576) + "", clock));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.MEMORY_TOTAL_KPI_NAME, KpiList.MEMORY_TOTAL_KPI_NAME, (int) (Double.valueOf(getValue("RealMemory", values)) / 1048576) + "", clock));

                //The general case is to add all metric values into the list.
                for (String value : values) {
                    try {
                        if (value.split("=").length == 2) {
                            measurement.addMetric(new MetricValue(value.split("=")[0].trim(), value.split("=")[0].trim(), value.split("=")[1].trim(), clock));
                        } else {
                            System.out.println("Parsing had an issue with : " + value);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (!valid) {
                    return;
                }
                current.put(hostname, measurement);
                if (lowest.get(hostname) == null || measurement.getPower() < lowest.get(hostname).getPower()) {
                    lowest.put(hostname, measurement);
                }
                if (highest.get(hostname) == null || measurement.getPower() > highest.get(hostname).getPower()) {
                    highest.put(hostname, measurement);
                }
            } catch (NumberFormatException ex) {
                //Ignore these errors and carry on. It may just be the header line.
                ex.printStackTrace();
            }
        }

        /**
         * This ensures that metric values are not added in cases where NaN etc
         * is given as an output from the data source.
         *
         * @param measurement The measurement to add the value to
         * @param value The value to add.
         * @return The measurement with the added metric only in cases where the
         * values correct.
         */
        private boolean validatedAddMetric(Measurement measurement, MetricValue value) {
            if (Double.isNaN(value.getValue()) || Double.isInfinite(value.getValue())) {
                return false;
            }
            measurement.addMetric(value);
            return true;
        }
    }

    /**
     * This is a CPU utilisation record for the WattsUp Meter data source
     * adaptor.
     */
    private class CPUUtilisation {

        private final String hostname;
        private final long clock;
        private final double cpu;

        /**
         * This creates a new CPU Utilisation record
         *
         * @param clock the time when the CPU Utilisation was taken
         * @param hostname the name of the host the measurement is for
         * @param cpu The CPU utilisation.
         */
        public CPUUtilisation(long clock, String hostname, double cpu) {
            this.clock = clock;
            this.hostname = hostname;
            this.cpu = cpu;
        }

        /**
         * The time when this record was taken
         *
         * @return The UTC time for this record.
         */
        public long getClock() {
            return clock;
        }

        /**
         * This returns the percentage of time the CPU was idle.
         *
         * @return 0..1 for how idle the CPU was at a specified time frame.
         */
        public double getCpuIdle() {
            return 1 - cpu;
        }

        /**
         * This returns the percentage of time the CPU was busy.
         *
         * @return 0..1 for how busy the CPU was at a specified time frame.
         */
        public double getCpuBusy() {
            return cpu;
        }

        public String getHostname() {
            return hostname;
        }

        /**
         * This indicates if this CPU utilisation object is older than a
         * specified time.
         *
         * @param time The UTC time to compare to
         * @return If the current item is older than the date specified.
         */
        public boolean isOlderThan(long time) {
            return clock < time;
        }

    }

}
