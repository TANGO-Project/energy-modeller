/**
 * Copyright 2015 University of Leeds
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

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.telemetry.Sample;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.openstack.OSFactory;

/**
 * The aim of this class is initially to take data from Ceilometer and to place
 * it into a format that is suitable for the Energy modeller.
 *
 * This code uses openstack4j (http://www.openstack4j.com/)
 *
 * For measurement details in Ceilometer
 *
 * http://docs.openstack.org/developer/ceilometer/measurements.html#dynamically-retrieving-the-meters-via-ceilometer-client
 *
 * @author Richard Kavanagh
 */
public class CeilometerDataSourceAdaptor implements HostDataSource {

    OSClient os = null;

    /**
     * This creates a new Ceilometer Data source adaptor.
     */
    public CeilometerDataSourceAdaptor() {
        System.getProperties().setProperty(org.openstack4j.core.transport.internal.HttpLoggingFilter.class.getName(), "true");
        os = OSFactory.builder()
                .endpoint("http://asok10.cit.tu-berlin.de:35357/v2.0")
                .credentials("admin", "UPMe12EQDu")
                .tenantName("admin")
                .authenticate(); //"http://130.149.248.39:5000/v3"

//        OSClient os = OSFactory.builderV3()
//                       .endpoint("http://130.149.248.39:5000/v3")
//                       .credentials("admin","UPMe12EQDu")
//                       .domainName("cit.tu-berlin.de")
//                       .authenticate();
    }

    @Override
    public Host getHostByName(String hostname) {
        List<? extends Hypervisor> hypervisors = os.compute().hypervisors().list();
        for (Hypervisor hypervisor : hypervisors) {
            if (hypervisor.getHypervisorHostname().equals(hostname)) {
                String hostId = hypervisor.getId();
                int hostIdInt = Integer.valueOf(hostId);
                Host answer = new Host(hostIdInt, hypervisor.getHypervisorHostname());
                answer.setDiskGb(hypervisor.getLocalDisk());
                answer.setRamMb(hypervisor.getLocalMemory());
                return answer;
            }
        }
        return null;
    }

    @Override
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VmDeployed getVmByName(String name) {
        List<? extends Server> servers = os.compute().servers().listAll(true);
        for (Server server : servers) {
            if (server.getName().equals(name)) {
                return getVm(server);
            }
        }
        return null;
    }

    @Override
    public List<Host> getHostList() {
        List<Host> answer = new ArrayList<>();
        List<? extends Hypervisor> hypervisors = os.compute().hypervisors().list();
        for (Hypervisor hypervisor : hypervisors) {
            String hostId = hypervisor.getId();
            int hostIdInt = Integer.valueOf(hostId);
            Host toAdd = new Host(hostIdInt, hypervisor.getHypervisorHostname());
            toAdd.setDiskGb(hypervisor.getLocalDisk());
            toAdd.setRamMb(hypervisor.getLocalMemory());
            answer.add(toAdd);
        }
        return answer;
    }

    @Override
    public List<GeneralPurposePowerConsumer> getGeneralPowerConsumerList() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<EnergyUsageSource> getHostAndVmList() {
        List<EnergyUsageSource> answer = new ArrayList<>();
        answer.addAll(getVmList());
        answer.addAll(getHostList());
        return answer;
    }

    @Override
    public List<VmDeployed> getVmList() {
        List<VmDeployed> answer = new ArrayList<>();
        List<? extends Server> servers = os.compute().servers().listAll(true);
        for (Server server : servers) {
            answer.add(getVm(server));
        }
        return answer;
    }

    /**
     * This converts a Ceilometer VM instance (i.e. a server into a VM).
     *
     * @param server The VM instance to convert into a VM
     * @return The VmDeployed the internal representation of a VM to the energy
     * modeller.
     */
    private VmDeployed getVm(Server server) {
        //TODO fix not having a host ID
//        String hostId = server.getHostId();
        int hostIdInt = 0;//Integer.valueOf(hostId);
        VmDeployed answer = new VmDeployed(hostIdInt, server.getName());
        answer.setDiskGb(server.getFlavor().getDisk());
        answer.setRamMb(server.getFlavor().getRam());
        GregorianCalendar created = new GregorianCalendar();
        created.setTime(server.getCreated());
        answer.setCreated(created);
        //Physical Host name
        answer.setCpus(server.getFlavor().getVcpus());
        answer.setIpAddress(server.getAccessIPv4());
        answer.setState(server.getVmState());
        return answer;
    }

    /**
     * This creates a host measurement from a hypervisor instance.
     *
     * @param host The host to get the measurement data for
     * @param hypervisor the hypervisor to get the data from
     * @param clock The time (Unix time) of the measurement
     * @return A new host measurement record.
     */
    private HostMeasurement getHostMeasurement(Host host, Hypervisor hypervisor, long clock) {
        HostMeasurement answer = new HostMeasurement(host, clock);
        //Disk
        answer.addMetric(new MetricValue(KpiList.DISK_FREE_KPI_NAME, KpiList.DISK_FREE_KPI_NAME, hypervisor.getFreeDisk() + "", clock));
        answer.addMetric(new MetricValue(KpiList.DISK_USED_KPI_NAME, KpiList.DISK_USED_KPI_NAME, hypervisor.getLocalDiskUsed() + "", clock));
        answer.addMetric(new MetricValue(KpiList.DISK_TOTAL_KPI_NAME, KpiList.DISK_TOTAL_KPI_NAME, hypervisor.getLocalDisk() + "", clock));
        //Memory
        answer.addMetric(new MetricValue(KpiList.MEMORY_AVAILABLE_KPI_NAME, KpiList.MEMORY_AVAILABLE_KPI_NAME, hypervisor.getFreeRam() + "", clock));
        answer.addMetric(new MetricValue(KpiList.MEMORY_TOTAL_KPI_NAME, KpiList.MEMORY_TOTAL_KPI_NAME, hypervisor.getLocalMemory() + "", clock));
        //CPU Details
        answer.addMetric(new MetricValue("system.cpu.arch", "system.cpu.arch", hypervisor.getCPUInfo().getArch(), clock));
        answer.addMetric(new MetricValue("system.cpu.model", "system.cpu.model", hypervisor.getCPUInfo().getModel(), clock));
        answer.addMetric(new MetricValue("system.cpu.vendor", "system.cpu.vendor", hypervisor.getCPUInfo().getVendor(), clock));
        //CPU Count
        answer.addMetric(new MetricValue(KpiList.CPU_COUNT_KPI_NAME, KpiList.CPU_COUNT_KPI_NAME, hypervisor.getCPUInfo().getTopology().getCores() + "", clock));
        //VCPU Information
        answer.addMetric(new MetricValue("system.vcpu.util", "system.vcpu.util", hypervisor.getVirtualUsedCPU() + "", clock));
        answer.addMetric(new MetricValue("system.vcpu.num", "system.vcpu.num", hypervisor.getVirtualCPU() + "", clock));
        //Workload
        answer.addMetric(new MetricValue("system.workload", "system.workload", hypervisor.getCurrentWorkload() + "", clock));
        //IP Address
        answer.addMetric(new MetricValue("IP_Adress", "IP_Adress", hypervisor.getHostIP(), clock));
        //VMs running count
        answer.addMetric(new MetricValue("system.vms.running", "system.vms.running", hypervisor.getRunningVM() + "", clock));
        //Hypervisor version
        answer.addMetric(new MetricValue("system.hypervisor.version", "system.hypervisor.version", hypervisor.getVersion() + "", clock));
        return answer;
    }

    @Override
    public HostMeasurement getHostData(Host host) {
        List<? extends Hypervisor> hypervisors = os.compute().hypervisors().list();
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Hypervisor hypervisor : hypervisors) {
            if (hypervisor.getHypervisorHostname().equals(host.getHostName())) {
                return getHostMeasurement(host, hypervisor, clock);
            }
        }
        return null;
    }

    @Override
    public List<HostMeasurement> getHostData() {
        List<? extends Hypervisor> hypervisors = os.compute().hypervisors().list();
        List<HostMeasurement> answer = new ArrayList<>();
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Hypervisor hypervisor : hypervisors) {
            Host host = getHostByName(hypervisor.getHypervisorHostname());
            if (hypervisor.getHypervisorHostname().equals(host.getHostName())) {
                answer.add(getHostMeasurement(host, hypervisor, clock));
            }
        }
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData(List<Host> hostList) {
        List<HostMeasurement> answer = new ArrayList<>();
        List<? extends Hypervisor> hypervisors = os.compute().hypervisors().list();
        HashSet<String> hostNames = new HashSet<>();
        for (Host host : hostList) {
            hostNames.add(host.getHostName());
        }
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Hypervisor hypervisor : hypervisors) {
            Host host = getHostByName(hypervisor.getHypervisorHostname());
            if (hostNames.contains(hypervisor.getHypervisorHostname())) {
                answer.add(getHostMeasurement(host, hypervisor, clock));
            }
        }
        return answer;
    }

    /**
     * This creates a host measurement from a hypervisor instance.
     *
     * @param vm The host to get the measurement data for
     * @param server the VM celiometer instance to get the data from
     * @param clock The time (Unix time) of the measurement
     * @return A new host measurement record.
     */
    private VmMeasurement getVmMeasurement(VmDeployed vm, Server server, long clock) {
        //TODO make the VM measurements more extensive!!!
        VmMeasurement answer = new VmMeasurement(vm, clock);
        
//        List<? extends Meter> meters = os.telemetry().meters().list();
//        System.out.println("Id, name , unit, type");
//        for (Meter meter : meters) {
//            System.out.println(meter.getId() + ", " + meter.getName() + ", " + meter.getUnit() + ", " + meter.getType().toString());
//        }
        
        //TODO Remove test code for diagnostics!!
//        Map<String, ? extends Number> diagnostics = os.compute().servers().diagnostics(server.getId());
//        ArrayList<Map.Entry<String, ? extends Number>> items = new ArrayList<>();
//        items.addAll(diagnostics.entrySet());
//        for (Map.Entry<String, ? extends Number> item : items) {
//            System.out.println("Item: " + item.getKey() + " Value: " + item.getValue());
//        }
        //Disk
//        answer.addMetric(new MetricValue(KpiList.DISK_FREE_KPI_NAME, KpiList.DISK_FREE_KPI_NAME, server.getFreeDisk() + "", clock));
//        answer.addMetric(new MetricValue(KpiList.DISK_USED_KPI_NAME, KpiList.DISK_USED_KPI_NAME, server.getLocalDiskUsed() + "", clock));
        answer.addMetric(new MetricValue(KpiList.DISK_TOTAL_KPI_NAME, KpiList.DISK_TOTAL_KPI_NAME, server.getFlavor().getDisk() + "", clock));
        //Memory
//        answer.addMetric(new MetricValue(KpiList.MEMORY_AVAILABLE_KPI_NAME, KpiList.MEMORY_AVAILABLE_KPI_NAME, server.getFreeRam() + "", clock));
        answer.addMetric(new MetricValue(KpiList.MEMORY_TOTAL_KPI_NAME, KpiList.MEMORY_TOTAL_KPI_NAME, server.getFlavor().getRam() + "", clock));
        //CPU Details
//        answer.addMetric(new MetricValue("system.cpu.arch", "system.cpu.arch", server.getCPUInfo().getArch(), clock));
//        answer.addMetric(new MetricValue("system.cpu.model", "system.cpu.model", server.getCPUInfo().getModel(), clock));
//        answer.addMetric(new MetricValue("system.cpu.vendor", "system.cpu.vendor", server.getCPUInfo().getVendor(), clock));
        //CPU Count
        answer.addMetric(new MetricValue(KpiList.CPU_COUNT_KPI_NAME, KpiList.CPU_COUNT_KPI_NAME, server.getFlavor().getVcpus() + "", clock));
        //VCPU Information
        answer.addMetric(new MetricValue("system.vcpu.num", "system.vcpu.num", server.getFlavor().getVcpus() + "", clock));
        //Workload
//        answer.addMetric(new MetricValue("system.workload", "system.workload", server.getCurrentWorkload() + "", clock));
        //IP Address
        answer.addMetric(new MetricValue("IP_Adress", "IP_Adress", server.getAccessIPv4(), clock));
        //Hypervisor version
        return answer;
    }

    @Override
    public VmMeasurement getVmData(VmDeployed Vm) {
        List<? extends Server> servers = os.compute().servers().listAll(true);
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Server server : servers) {
            if (server.getName().equals(Vm.getName())) {
                return getVmMeasurement(Vm, server, clock);
            }
        }
        return null;
    }

    @Override
    public List<VmMeasurement> getVmData() {
        List<? extends Server> servers = os.compute().servers().listAll(true);
        List<VmMeasurement> answer = new ArrayList<>();
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Server server : servers) {
            VmDeployed vm = getVmByName(server.getName());
            answer.add(getVmMeasurement(vm, server, clock));
        }
        return answer;
    }

    @Override
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList) {
        List<VmMeasurement> answer = new ArrayList<>();
        List<? extends Server> servers = os.compute().servers().listAll(true);
        HashSet<String> vmNames = new HashSet<>();
        for (VmDeployed vm : vmList) {
            vmNames.add(vm.getName());
        }
        long clock = TimeUnit.MILLISECONDS.toSeconds(new GregorianCalendar().getTimeInMillis());
        for (Server server : servers) {
            VmDeployed vm = getVmByName(server.getName());
            if (vmNames.contains(server.getHypervisorHostname())) {
                answer.add(getVmMeasurement(vm, server, clock));
            }
        }
        return answer;
    }

    @Override
    public CurrentUsageRecord getCurrentEnergyUsage(Host host) {
        CurrentUsageRecord record = new CurrentUsageRecord(host);
        double power = -1;
        Date timeStamp = new GregorianCalendar().getTime();
//        GregorianCalendar start = new GregorianCalendar();
//        //TODO fix this time stamp issue, between CET and GMT
////        long timeInPast = start.getTimeInMillis() - TimeUnit.SECONDS.toMillis(3600 + 50); //last 50 seconds
////        start.setTimeInMillis(timeInPast);        
////        SampleCriteria criteria = SampleCriteria.create()
////                .timestamp(SampleCriteria.Oper.GT, start.getTime());
//        List<? extends Sample> samples = os.telemetry().meters().samples("power"); //, criteria);
//        for (int i = samples.size() - 1; i >= 0; i--) {
//            Sample sample = samples.get(i);
//            if (sample.getMetadata().get("display_name").equals(host.getHostName())) {
//                timeStamp = sample.getRecordedAt();
//                //TODO Check units ensure that they are W not kW for example!
//                power = sample.getCounterVolume();
//            }
//
//        }
        record.setPower(power);
        GregorianCalendar time = new GregorianCalendar();
        time.setTime(timeStamp);
        record.setTime(time);
        return record;
    }

    @Override
    public double getLowestHostPowerUsage(Host host) {
//        List<? extends Statistics> stats = os.telemetry().meters().statistics("power", 600);
        return 0.0;
    }

    @Override
    public double getHighestHostPowerUsage(Host host) {
//        List<? extends Statistics> stats = os.telemetry().meters().statistics("power");
        return 0.0;
    }

    @Override
    public double getCpuUtilisation(Host host, int durationSeconds) {
        //TODO fix that ther is no usage of the duration information
        for (Hypervisor hypervisor : os.compute().hypervisors().list()) {
           if (hypervisor.getHypervisorHostname().equals(host.getHostName())) {
               //TODO check the value reported changes!! as per code below!!
               return ((double)hypervisor.getCurrentWorkload()) / 100.0;
            }
        }
        return -1.0;
    }

    public double getCpuUtilisation(VmDeployed host, int durationSeconds) {
        GregorianCalendar start = new GregorianCalendar();
        long startTime = start.getTimeInMillis() - TimeUnit.SECONDS.toMillis(durationSeconds);
        start.setTimeInMillis(startTime);
        SampleCriteria criteria = SampleCriteria.create()
                .timestamp(SampleCriteria.Oper.GT, start.getTime());
        criteria.add("metadata.display_name", SampleCriteria.Oper.EQUALS, host.getName());
        List<? extends Sample> samples = os.telemetry().meters().samples("cpu_util", criteria);
        double answer = 0.0;
        double counter = 0;
        for (Sample sample : samples) {
            System.out.println(sample.getMetadata().get("display_name"));
            if (sample.getMetadata().get("display_name").equals(host.getName())) {
                answer = answer + sample.getCounterVolume();
                counter = counter + 1;
            }
        }
        return answer / (counter * 100);
    }

}
