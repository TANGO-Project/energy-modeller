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
package eu.tango.energymodeller.datastore;

import eu.tango.energymodeller.datastore.DefaultDatabaseConnector;
import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.VmDiskImage;
//import eu.ascetic.asceticarchitecture.iaas.energymodeller.types.energyuser.usage.HostVmLoadFraction;
import eu.tango.energymodeller.types.usage.HostEnergyRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryBootRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryWeekRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
//import static org.junit.Assert.*;

/**
 * This is the test class for the default database connector.
 *
 * @author Richard Kavanagh
 */
public class DefaultDatabaseConnectorTest {

    private final List<Host> hostList = new ArrayList<>();
    private final Host CHOSEN_HOST = new Host(10084, "asok10");

    public DefaultDatabaseConnectorTest() {
        hostList.add(new Host(10105, "asok09"));
        hostList.add(new Host(10084, "asok10"));
        hostList.add(new Host(10107, "asok11"));
        hostList.add(new Host(10106, "asok12"));
    }

    /**
     * Test of getHosts method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetHosts() {
        System.out.println("getHosts");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Collection<Host> result = instance.getHosts();
        assert (result != null);
        assert (!result.isEmpty());
    }

    /**
     * Test of getHostCalibrationData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetHostCalibrationData_Collection() {
        System.out.println("getHostCalibrationData");
        Collection<Host> hosts = hostList;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Collection<Host> result = instance.getHostCalibrationData(hosts);
        assert (!result.isEmpty());
    }

    /**
     * Test of getHostCalibrationData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetHostCalibrationData_Host() {
        System.out.println("getHostCalibrationData");
        Host host = CHOSEN_HOST;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Host result = instance.getHostCalibrationData(host);
        assertNotNull(result);
    }

    /**
     * Test of setHosts method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testSetHosts() {
        System.out.println("setHosts");
        Collection<Host> hosts = hostList;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        instance.setHosts(hosts);
    }

    /**
     * Test of setHostCalibrationData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testSetHostCalibrationData() {
        System.out.println("setHostCalibrationData");
        Host host = CHOSEN_HOST;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        //This will run the method but otherwise do very little. A better test case would be good.
        instance.setHostCalibrationData(host);
        assert (host.getCalibrationData().isEmpty());
    }

    /**
     * Test of writeHostHistoricData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testWriteHostHistoricData() {
        System.out.println("writeHostHistoricData");
        Host host = CHOSEN_HOST;
        long time = new GregorianCalendar().getTimeInMillis() / 1000;
        double power = 300;
        double energy = 1500;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        instance.writeHostHistoricData(host, time, power, energy);
    }

    /**
     * Test of getHostHistoryData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetHostHistoryData() {
        System.out.println("getHostHistoryData");
        Host host = CHOSEN_HOST;
        GregorianCalendar time = new GregorianCalendar();
        int deltaTime = (int) TimeUnit.MINUTES.toMillis(60);
        long startTime = time.getTimeInMillis() - deltaTime;
        time.setTimeInMillis(startTime);
        TimePeriod timePeriod = new TimePeriod(time, deltaTime, TimeUnit.MINUTES);
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Collection<HostEnergyRecord> result = instance.getHostHistoryData(host, timePeriod);
        assert (!result.isEmpty());
        System.out.println("Host Records Found: " + result.size());

    }

    /**
     * Test of getVms method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetVms() {
        System.out.println("getVms");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Collection<VmDeployed> result = instance.getVms();
        assert (!result.isEmpty());
        System.out.println("VM Records Found: " + result.size());
        for (VmDeployed vmDeployed : result) {
            assert (vmDeployed.getName() != null);
            assert (vmDeployed.getId() > 0);
            System.out.println("VM Name: " + vmDeployed.getName());
            System.out.println("VM Id: " + vmDeployed.getId());
            System.out.println("VM Deployment ID: " + vmDeployed.getDeploymentID());
            System.out.println("Created: " + vmDeployed.getCreated());
            System.out.println("CPU Count: " + vmDeployed.getCpus());
            System.out.println("Disks GB: " + vmDeployed.getDiskGb());
            System.out.println("Memory Mb: " + vmDeployed.getRamMb());
            System.out.println("State: " + vmDeployed.getState());
            if (vmDeployed.getAllocatedTo() != null) {
                System.out.println("Allocated To: " + vmDeployed.getAllocatedTo().getHostName());
            } else {
                System.out.println("Allocated To: UNKNOWN!!");
            }
        }
    }

    /**
     * Test of getHostProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetHostProfileData() {
        System.out.println("getHostProfileData");
        Host host = CHOSEN_HOST;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Host result = instance.getHostProfileData(host);
        assert (result.getFlopsPerWatt() >= 0);
        assert (result.getProfileData().size() > 0);
    }

//    /**
//     * Test of setVms method, of class DefaultDatabaseConnector.
//     */
//    @Test
//    public void testSetVms() {
//        System.out.println("setVms");
//        Collection<VmDeployed> vms = null;
//        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
//        instance.setVms(vms);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetVMProfileData() {
        System.out.println("getVMProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        Collection<VmDeployed> vms = instance.getVms();
        for (VmDeployed vm : vms) {
            VmDeployed answer = instance.getVMProfileData(vm);
            System.out.println("VM: " + vm.getId() + " VM: " + vm.getName());
            for (String tag : answer.getApplicationTags()) {
                System.out.println("Tag: " + tag);
                assert (!"".equals(tag));
            }
            for (VmDiskImage disk : answer.getDiskImages()) {
                System.out.println("Tag: " + disk.toString());
                assert (!"".equals(disk.toString()));
            }
        }

    }

    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetAverageCPUUtilisationTag() {
        System.out.println("getHostProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        double result = instance.getAverageCPUUtilisationTag("threeTierWebApp").getUtilisation();
        assert (result >= 0);
        System.out.println("Average CPU Utilisation for Tag: " + result);
    }

    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetAverageCPUUtilisationDisk() {
        System.out.println("getHostProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        double result = instance.getAverageCPUUtilisationDisk("JBoss").getUtilisation();
        assert (result >= 0);
        System.out.println("Average CPU Utilisation for Tag: " + result);
    }
    
    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetAverageCPUUtilisationBootTraceForTag() {
        System.out.println("getHostProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        List<VmLoadHistoryBootRecord> result = instance.getAverageCPUUtilisationBootTraceForTag("threeTierWebApp", 3600);
        for (VmLoadHistoryBootRecord row : result) {
            assert (row.getIndex() >= 0);
            assert (row.getUtilisation() >= 0);
            System.out.println("Index: " + row.getIndex() +  " load: " + row.getUtilisation());
        }
    }    

    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetAverageCPUUtilisationWeekTraceForTag() {
        System.out.println("getHostProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        List<VmLoadHistoryWeekRecord> result = instance.getAverageCPUUtilisationWeekTraceForTag("JBoss");
        for (VmLoadHistoryWeekRecord row : result) {
            assert (row.getDayOfWeek() >= 0);
            assert (row.getHourOfDay() >= 0);
            assert (row.getUtilisation()>= 0);
            System.out.println("day: " + row.getDayOfWeek() + " hour: " + row.getHourOfDay()+  " load: " + row.getUtilisation());
        }
    }
    
    /**
     * Test of getVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testGetAverageCPUUtilisationWeekTraceForDisk() {
        System.out.println("getHostProfileData");
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        List<VmLoadHistoryWeekRecord> result = instance.getAverageCPUUtilisationWeekTraceForDisk("JBoss");
        for (VmLoadHistoryWeekRecord row : result) {
            assert (row.getDayOfWeek() >= 0);
            assert (row.getHourOfDay() >= 0);
            assert (row.getUtilisation()>= 0);
            System.out.println("day: " + row.getDayOfWeek() + " hour: " + row.getHourOfDay()+  " load: " + row.getUtilisation());
        }
    }    


    /**
     * Test of setVMProfileData method, of class DefaultDatabaseConnector.
     */
    @Test
    public void testSetVMProfileData() {
        System.out.println("setVMProfileData");
        VmDeployed vm = null;
        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
        instance.setVMProfileData(vm);
    }
//
//    /**
//     * Test of setHostProfileData method, of class DefaultDatabaseConnector.
//     */
//    @Test
//    public void testSetHostProfileData() {
//        System.out.println("setHostProfileData");
//        Host host = null;
//        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
//        instance.setHostProfileData(host);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    /**
//     * Test of writeHostVMHistoricData method, of class DefaultDatabaseConnector.
//     */
//    @Test
//    public void testWriteHostVMHistoricData() {
//        System.out.println("writeHostVMHistoricData");
//        Host host = null;
//        long time = 0L;
//        HostVmLoadFraction load = null;
//        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
//        instance.writeHostVMHistoricData(host, time, load);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getHostVmHistoryLoadData method, of class DefaultDatabaseConnector.
//     */
//    @Test
//    public void testGetHostVmHistoryLoadData() {
//        System.out.println("getHostVmHistoryLoadData");
//        Host host = null;
//        TimePeriod timePeriod = null;
//        DefaultDatabaseConnector instance = new DefaultDatabaseConnector();
//        Collection<HostVmLoadFraction> expResult = null;
//        Collection<HostVmLoadFraction> result = instance.getHostVmHistoryLoadData(host, timePeriod);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
