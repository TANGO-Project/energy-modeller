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

import eu.tango.energymodeller.datasourceclient.VmMeasurement;
import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import eu.tango.energymodeller.datasourceclient.CeilometerDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * This is the test class for the Ceilometer Data source adaptor.
 *
 * @author Richard
 */
public class CeilometerDataSourceAdaptorTest {

    private final List<Host> hostList = new ArrayList<>();
    private final Host CHOSEN_HOST = new Host(1, "asok10.cit.tu-berlin.de");
    private final String VM_NAME = "IaaS_VM_Dev";
    private final CeilometerDataSourceAdaptor instance = new CeilometerDataSourceAdaptor();

    public CeilometerDataSourceAdaptorTest() {
        hostList.add(new Host(2, "asok09.cit.tu-berlin.de"));
        hostList.add(new Host(1, "asok10.cit.tu-berlin.de"));
//        hostList.add(new Host(10107, "asok11"));
        hostList.add(new Host(3, "asok12.cit.tu-berlin.de"));
    }

    /**
     * Test of getHostByName method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostByName() {
        System.out.println("getHostByName");
        String hostname = "asok10.cit.tu-berlin.de";
        Host expResult = CHOSEN_HOST;
        Host result = instance.getHostByName(hostname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmByName method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetVmByName() {
        System.out.println("getVmByName");
        String name = VM_NAME;
        VmDeployed result = instance.getVmByName(name);
        assert (result != null);
        System.out.println("Name: " + result.getName());
        System.out.println("Id: " + result.getId());
        System.out.println("Created: " + result.getCreated());
        System.out.println("CPU Count: " + result.getCpus());
        System.out.println("Disks GB: " + result.getDiskGb());
        System.out.println("Memory Mb: " + result.getRamMb());
        System.out.println("State: " + result.getState());
    }

    /**
     * Test of getHostList method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostList() {
        System.out.println("getHostList");
        List<Host> expResult = new ArrayList<>();
        expResult.addAll(hostList);
        List<Host> result = instance.getHostList();
        for (Host host : result) {
            assert (result.contains(host));
            System.out.println("Host name: " + host.getHostName());
            System.out.println("Host id: " + host.getId());
            assert (host.getRamMb() > 0);
            assert (host.getDiskGb() > 0);
            System.out.println("Host ram: " + host.getRamMb());
            System.out.println("Host disk: " + host.getDiskGb());
        }
        assertEquals(expResult.size(), result.size());
    }

    /**
     * Test of getHostAndVmList method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostAndVmList() {
        System.out.println("getHostAndVmList");
        List<EnergyUsageSource> result = instance.getHostAndVmList();
        assert(result != null);
        assert(result.size() >= 3);
    }

    /**
     * Test of getVmList method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetVmList() {
        System.out.println("getVmList");
        List<VmDeployed> result = instance.getVmList();
        assert (!result.isEmpty());
        for (VmDeployed vmDeployed : result) {
            System.out.println("Name: " + vmDeployed.getName());
            System.out.println("Id: " + vmDeployed.getId());
            System.out.println("Created: " + vmDeployed.getCreated());
            System.out.println("CPU Count: " + vmDeployed.getCpus());
            System.out.println("Disks GB: " + vmDeployed.getDiskGb());
            System.out.println("Memory Mb: " + vmDeployed.getRamMb());
            System.out.println("State: " + vmDeployed.getState());
        }
    }

    /**
     * Test of getHostData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostData_Host() {
        System.out.println("getHostData");
        Host host = CHOSEN_HOST;
        HostMeasurement result = instance.getHostData(host);
        assert(result != null);
        assert(result.getMetricCount() > 0);
    }

    /**
     * Test of getHostData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostData_0args() {
        System.out.println("getHostData");
        List<HostMeasurement> result = instance.getHostData();
        assert (result != null);
        for (HostMeasurement hostMeasurement : result) {
            assert (hostMeasurement.getMetricCount() != 0);
        }
    }

    /**
     * Test of getHostData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHostData_List() {
        System.out.println("getHostData");
        List<HostMeasurement> result = instance.getHostData(hostList);
        assert (result != null);
        for (HostMeasurement hostMeasurement : result) {
            assert (hostMeasurement.getMetricCount() != 0);
        }
    }

    /**
     * Test of getVmData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetVmData_VmDeployed() {
        System.out.println("getVmData");
        VmDeployed vm;
        vm = instance.getVmByName(VM_NAME);
        VmMeasurement result = instance.getVmData(vm);
        assert (result != null);
        assert (result.getMetricCount() != 0);
        System.out.println("VM Metric List");
        for (String name : result.getMetricNameList()) {
            System.out.println(name);
        }
    }

    /**
     * Test of getVmData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetVmData_0args() {
        System.out.println("getVmData");
        List<VmMeasurement> result = instance.getVmData();
        assert (result != null);
        assert (!result.isEmpty());
    }

    /**
     * Test of getVmData method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetVmData_List() {
        System.out.println("getVmData");
        List<VmDeployed> vmList = new ArrayList<>();
        vmList.add(instance.getVmByName(VM_NAME));
        List<VmMeasurement> result = instance.getVmData(vmList);
        assert (result != null);
        for (VmMeasurement vmMeasurement : result) {
            System.out.println("Name: " + vmMeasurement.getVm().getName());
        }
    }

    /**
     * Test of getCurrentEnergyUsage method, of class
     * CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetCurrentEnergyUsage() {
        System.out.println("getCurrentEnergyUsage");
        Host host = CHOSEN_HOST;
        CurrentUsageRecord result = instance.getCurrentEnergyUsage(host);
        assert (result != null);
        assert (result.getPower() > 0.0);
        System.out.println("Host Name: " + host.getHostName());
        System.out.println("Host Id: " + host.getId());
        System.out.println("Host Current Power Usage: " + result.getPower());
    }

    /**
     * Test of getLowestHostPowerUsage method, of class
     * CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetLowestHostPowerUsage() {
        System.out.println("getLowestHostPowerUsage");
        Host host = CHOSEN_HOST;
        double result = instance.getLowestHostPowerUsage(host);
        assert (result > 0.0);
        System.out.println("Host Name: " + host.getHostName());
        System.out.println("Host Id: " + host.getId());
        System.out.println("Lowest Power: " + result);
    }

    /**
     * Test of getHighestHostPowerUsage method, of class
     * CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetHighestHostPowerUsage() {
        System.out.println("getHighestHostPowerUsage");
        Host host = CHOSEN_HOST;
        double result = instance.getHighestHostPowerUsage(host);
        assert(result > 0.0);
        System.out.println("Highest Host Power Usage: " + result);
    }

    /**
     * Test of getCpuUtilisation method, of class CeilometerDataSourceAdaptor.
     */
    @Test
    public void testGetCpuUtilisation() {
        System.out.println("getCpuUtilisation");
        /**
         * TODO fix this cheat, given this is a VM not a physical host!
         */
        Host host = CHOSEN_HOST; //new Host(1,"IaaS_VM_Dev");
        int duration = (int) TimeUnit.MINUTES.toSeconds(1);
        double result = instance.getCpuUtilisation(host, duration);
        assert(result >= 0);
        assert(result <= 1.0);
        System.out.println("CPU Utilisation Last " + duration + " minutes: " + result + " %");
    }

}
