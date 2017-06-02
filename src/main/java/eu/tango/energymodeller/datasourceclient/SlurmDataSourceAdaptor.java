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

import eu.ascetic.ioutils.caching.LRUCache;
import eu.ascetic.ioutils.io.Settings;
import static eu.tango.energymodeller.datasourceclient.KpiList.POWER_KPI_NAME;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private final LRUCache<Integer, ApplicationOnHost> appCache = new LRUCache<>(10, 50);

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
    public List<ApplicationOnHost> getHostApplicationList() {
        ArrayList<ApplicationOnHost> answer = new ArrayList<>();
        try {

            /*
             * This queries what jobs are currently running, it outputs
             * "JOBID, NAME, TIME, NODELIST (REASON)"
             * One line per job and space separated.
             * 
             * The output looks like:
             * 
             * 3845 RK-BENCH 0:03 ns57
             */
            String maincmd = "squeue | awk 'NR> 1 {split($0,values,\"[ \\t\\n]+\"); "
                    + "printf values[1] \" \" ; printf values[2] \" \"; "
                    + "printf values[4] \" \";"
                    + "printf values[7] \" \" ; "
                    + "print values[9]}'";
            String cmd[] = {"/bin/sh",
                "-c",
                maincmd};
            ArrayList<String> output = execCmd(cmd);
            for (String line : output) {
                System.out.println("output: " + line);
                if (line != null && !line.isEmpty()) {
                    line = line.trim();
                    String[] items = line.split(" ");
                    try {
                        Host host = getHostByName(items[3]);
                        int appId = Integer.parseInt(items[0]);
                        if (appCache.get(appId) != null) {
                            /**
                             * Get the cached copy, avoids duplicating objects
                             */
                            answer.add(appCache.get(appId));
                            continue;
                        }
                        String name = items[1];
                        String duration = items[2]; //to parse into duration
                        String[] durationSplit = duration.split(":"); //0:03 i.e. mins:seconds
                        long min = Long.parseLong(durationSplit[0]);
                        long seconds = Long.parseLong(durationSplit[1]);
                        seconds = seconds + TimeUnit.MINUTES.toSeconds(min);
                        long currentTime = System.currentTimeMillis();
                        long startTime = currentTime - TimeUnit.SECONDS.toMillis(seconds);
                        GregorianCalendar start = new GregorianCalendar();
                        start.setTimeInMillis(startTime);
                        ApplicationOnHost app = new ApplicationOnHost(appId, name, host);
                        app.setCreated(start);
                        answer.add(app);
                        appCache.put(appId, app);
                    } catch (NumberFormatException ex) {
                        Logger.getLogger(SlurmDataSourceAdaptor.class.getName()).log(Level.SEVERE,
                                "Unexpected number format", ex);
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(SlurmDataSourceAdaptor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This executes a command and returns the output as a line of strings.
     *
     * @param cmd The command to execute
     * @return A list of output broken down by line
     * @throws java.io.IOException
     */
    private static ArrayList<String> execCmd(String[] cmd) throws java.io.IOException {
        ArrayList<String> output = new ArrayList<>();
        Process proc = Runtime.getRuntime().exec(cmd);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String val = "";
        while (s.hasNextLine()) {
            val = s.next();
            output.add(val);
        }
        return output;
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
     * Given a key this returns its parameter's value.
     *
     * @param key The key name for the parameter to return
     * @param parseString The string to split and get the property value from
     * @return The value of the parameter else null.
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
     * Given a key this returns its parameter's value.
     *
     * @param key The key name for the parameter to return
     * @param parseString The string to split and get the property value from
     * @return The value of the parameter else null.
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

        /**
         * This reads in the Gres string, this string represents Generic
         * Resource Scheduling (GRES). Usually either GPUs or Intel Many
         * Integrated Core (MIC) processors
         *
         * @param values The parsed list of metrics.
         * @param measurement The measurement to add the metrics to
         * @param clock The timestamp for the new metric values
         * @return The newly adjusted measurement
         */
        private HostMeasurement readGresString(String[] values, HostMeasurement measurement, long clock) {
            String gresString = getValue("Gres", values);
            /*
             * Example gresString = "gpu:teslak20:2"
             * or "craynetwork:3,hbm:0"
             * or gpus:2*cpu,disk=50G
             * The original string this is parsed from is: "Gres=gpu:teslak20:2"
             */
            boolean hasGraphicsCard = gresString.contains("gpu");
            boolean hasMic = gresString.contains("mic"); //Many Integrated Core
            boolean hasAccelerator = !gresString.equals("(null)");
            measurement.addMetric(new MetricValue(KpiList.HAS_GPU, KpiList.HAS_GPU, hasGraphicsCard + "", clock));
            measurement.addMetric(new MetricValue(KpiList.HAS_MIC, KpiList.HAS_MIC, hasMic + "", clock));
            measurement.addMetric(new MetricValue(KpiList.HAS_ACCELERATOR, KpiList.HAS_ACCELERATOR, hasAccelerator + "", clock));
            String[] gresStringSplit = gresString.split(",");
            for (String dataItem : gresStringSplit) {
                boolean gpu = dataItem.contains("gpu");
                boolean mic = dataItem.contains("mic");
                String[] dataItemSplit = dataItem.split(":");
                String gpuName = dataItemSplit[1];
                int gpuCount = 1;
                if (dataItemSplit.length > 2) {
                    try {
                        gpuCount = Integer.parseInt(dataItemSplit[2].trim());
                    } catch (NumberFormatException ex) {
                        Logger.getLogger(SlurmDataSourceAdaptor.class.getName()).log(Level.SEVERE,
                                "Unexpected number format", ex);
                    }
                }
                if (gpu) {
                    measurement.addMetric(new MetricValue(KpiList.GPU_NAME, KpiList.GPU_NAME, gpuName, clock));
                    measurement.addMetric(new MetricValue(KpiList.GPU_COUNT, KpiList.GPU_COUNT, gpuCount + "", clock));
                } else if (mic) {
                    measurement.addMetric(new MetricValue(KpiList.MIC_NAME, KpiList.MIC_NAME, gpuName, clock));
                    measurement.addMetric(new MetricValue(KpiList.MIC_COUNT, KpiList.MIC_COUNT, gpuCount + "", clock));
                }

            }
            return measurement;
        }

        /**
         * This reads in the GresUsed string, this string represents Generic
         * Resource Scheduling (GRES). Usually either GPUs or Intel Many
         * Integrated Core (MIC) processors
         *
         * @param values The parsed list of metrics.
         * @param measurement The measurement to add the metrics to
         * @param clock The timestamp for the new metric values
         * @return The newly adjusted measurement
         */
        private HostMeasurement readGresUsedString(String[] values, HostMeasurement measurement, long clock) {
            String gresString = getValue("GresUsed", values);
            String[] gresStringSplit = gresString.split(",");
            for (String dataItem : gresStringSplit) {
                boolean gpu = dataItem.contains("gpu");
                boolean mic = dataItem.contains("mic");
                String[] dataItemSplit = dataItem.split(":");
                String gpuUsed = "";
                for (String item : dataItemSplit) {
                    if (!item.isEmpty() && Character.isDigit(item.charAt(0))) {
                        int used = new Scanner(item).useDelimiter("[^\\d]+").nextInt();
                        gpuUsed = used + "";
                        break;
                    }
                }
                if (gpu) {
                    measurement.addMetric(new MetricValue(KpiList.GPU_USED, KpiList.GPU_USED, gpuUsed, clock));
                } else if (mic) {
                    measurement.addMetric(new MetricValue(KpiList.MIC_USED, KpiList.MIC_USED, gpuUsed, clock));
                }

            }
            return measurement;
        }

        /**
         * This reads in generic metrics from SLURM that the data source adaptor
         * is not necessarily expecting.
         *
         * @param values The parsed list of metrics.
         * @param measurement The measurement to add the metrics to
         * @param clock The timestamp for the new metric values
         * @return The newly adjusted measurement
         */
        private HostMeasurement readGenericMetrics(String[] values, HostMeasurement measurement, long clock) {
            //The general case is to add all metric values into the list.
            for (String value : values) {
                String[] valueSplit = value.split("=");
                try {
                    switch (valueSplit.length) {
                        case 2: //Most common case
                            measurement.addMetric(new MetricValue(valueSplit[0].trim(), valueSplit[0].trim(), valueSplit[1].trim(), clock));
                            break;
                        case 1: //In cases such as AllocTRES, leave the metric there but report the empty value
                            measurement.addMetric(new MetricValue(valueSplit[0].trim(), valueSplit[0].trim(), "", clock));
                            break;
                        default: //Cases such as CfgTRES=cpu=32,mem=64408M
                            int params = value.split("=").length;
                            valueSplit = value.split("[=,]");
                            /*
                             * CfgTRES=cpu=32,mem=64408M becomes
                             * [CfgTRES, cpu, 32, mem, 64408M]
                             * Thus end result should be:
                             * [CfgTRES:cpu, 32] i.e. index 0:1 , 2
                             * [CfgTRES:mem, 64408M] i.e. index 0:3 , 5
                             */
                            for (int i = 1; i <= params; i++) {
                                String name = valueSplit[0].trim() + ":" + valueSplit[i * 2 - 1].trim();
                                measurement.addMetric(new MetricValue(name, name, valueSplit[i * 2].trim(), clock));
                            }
                            //System.out.println("Parsing had an issue with : " + value);
                            break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return measurement;
        }

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
                readGresString(values, measurement, clock);
                readGresUsedString(values, measurement, clock);
                readGenericMetrics(values, measurement, clock);
                double cpuUtil = (Double.valueOf(getValue("CPULoad", values))) / (Double.valueOf(getValue("CPUTot", values)));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.CPU_SPOT_USAGE_KPI_NAME, KpiList.CPU_SPOT_USAGE_KPI_NAME, cpuUtil * 100 + "", clock));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.CPU_IDLE_KPI_NAME, KpiList.CPU_IDLE_KPI_NAME, ((1 - cpuUtil)) * 100 + "", clock));
                if (!valid) {
                    System.out.println("The measurement taken was invalid");
                    return;
                }

                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.MEMORY_AVAILABLE_KPI_NAME, KpiList.MEMORY_AVAILABLE_KPI_NAME, (int) (Double.valueOf(getValue("FreeMem", values)) / 1048576) + "", clock));
                valid = valid && validatedAddMetric(measurement, new MetricValue(KpiList.MEMORY_TOTAL_KPI_NAME, KpiList.MEMORY_TOTAL_KPI_NAME, (int) (Double.valueOf(getValue("RealMemory", values)) / 1048576) + "", clock));

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
