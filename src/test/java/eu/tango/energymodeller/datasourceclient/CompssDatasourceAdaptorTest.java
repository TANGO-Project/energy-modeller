/**
 * Copyright 2018 University of Leeds
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

import eu.tango.energymodeller.datasourceclient.compsstype.CompssImplementation;
import eu.tango.energymodeller.datasourceclient.compsstype.CompssResource;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Richard Kavanagh
 */
public class CompssDatasourceAdaptorTest {
    
    public CompssDatasourceAdaptorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getCompssResources method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetCompssResources() {
        System.out.println("getCompssResources");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        List<CompssResource> result = instance.getCompssResources();
        assert(result.size() == 1);
        for (CompssResource item : result) {
            System.out.println(item);
        }
    }

    /**
     * Test of getCompssImplementation method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetCompssImplementation() {
        System.out.println("getCompssImplementation");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");        
        List<CompssImplementation> result = instance.getCompssImplementation();
        assert(result.size() == 2);
        for (CompssImplementation item : result) {
            System.out.println(item);
        }
    }
    
    @Test
    public void testGetCurrentMonitoringJobId() {
        System.out.println("getMonitoringFile");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");            
        String expResult = "example_run_01";
        String result = instance.getCurrentMonitoringJobId();
        assertEquals(expResult, result);        
    }

    /**
     * Test of getMonitoringFile method, of class ProgrammingModelClient.
     */
    @Test
    public void testGetMonitoringFile() {
        System.out.println("getMonitoringFile");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        String expResult = "/monitor/COMPSs_state.xml";
        String result = instance.getMonitoringFile();
        assertEquals(expResult, result);
    }

    /**
     * Test of setMonitoringDirectory method, of class ProgrammingModelClient.
     */
    @Test
    public void testSetMonitoringDirectory() {
        System.out.println("setMonitoringDirectory");
        String monitoringDirectory = System.getProperty("user.home") + "/.COMPSs/";
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory(monitoringDirectory);
    }

    /**
     * Test of setMonitoringFile method, of class ProgrammingModelClient.
     */
    @Test
    public void testSetMonitoringFile() {
        System.out.println("setMonitoringFile");
        String monitoringFile = "/monitor/COMPSs_state.xml";
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringFile(monitoringFile);
    }

    /**
     * Test of getHostByName method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostByName() {
        System.out.println("getHostByName");
        String hostname = "ns50";
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");    
        Host expResult = new Host(50, "ns50");
        Host result = instance.getHostByName(hostname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getGeneralPowerConsumerByName method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetGeneralPowerConsumerByName() {
        System.out.println("getGeneralPowerConsumerByName");
        String hostname = "ns50";
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        GeneralPurposePowerConsumer expResult = null;
        GeneralPurposePowerConsumer result = instance.getGeneralPowerConsumerByName(hostname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmByName method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetVmByName() {
        System.out.println("getVmByName");
        String name = "";
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        VmDeployed expResult = null;
        VmDeployed result = instance.getVmByName(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getHostList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostList() {
        System.out.println("getHostList");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        List<Host> result = instance.getHostList();
        assert(result.size() == 1);
        for (Host item : result) {
            assert(item.getState().equals("IDLE"));            
            System.out.println(item);
        }
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state_running.xml");
        List<Host> result2 = instance.getHostList();
        assert(result2.size() == 1);
        for (Host item : result2) {
            assert(!item.getState().equals("IDLE"));
            System.out.println(item);
        }        
        
    }

    /**
     * Test of getHostAndVmList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostAndVmList() {
        System.out.println("getHostAndVmList");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        List<EnergyUsageSource> result = instance.getHostAndVmList();
        assert(result.size() == 1);
        
    }

    /**
     * Test of getGeneralPowerConsumerList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetGeneralPowerConsumerList() {
        System.out.println("getGeneralPowerConsumerList");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<GeneralPurposePowerConsumer> expResult = new ArrayList<>();
        List<GeneralPurposePowerConsumer> result = instance.getGeneralPowerConsumerList();
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetVmList() {
        System.out.println("getVmList");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<VmDeployed> expResult = new ArrayList<>();;
        List<VmDeployed> result = instance.getVmList();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHostApplicationList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostApplicationList_ApplicationOnHostJOB_STATUS() {
        System.out.println("getHostApplicationList");
        ApplicationOnHost.JOB_STATUS state = ApplicationOnHost.JOB_STATUS.RUNNING;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");    
        List<ApplicationOnHost> expResult = new ArrayList<>();
        List<ApplicationOnHost> result = instance.getHostApplicationList(state);
        assertEquals(expResult, result);
    }

    /**
     * Test of getHostApplicationList method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostApplicationList_0args() {
        System.out.println("getHostApplicationList");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state_running.xml");
        List<ApplicationOnHost> result = instance.getHostApplicationList();
        assert(result.size() == 2);
        for (ApplicationOnHost item : result) {
            System.out.println(item);
        }
    }

    /**
     * Test of getHostData method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostData_Host() {
        System.out.println("getHostData");
        Host host = new Host(50, "ns50");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        HostMeasurement result = instance.getHostData(host);
        System.out.println(result);
    }

    /**
     * Test of getHostData method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostData_0args() {
        System.out.println("getHostData");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<HostMeasurement> expResult = new ArrayList<>();
        List<HostMeasurement> result = instance.getHostData();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHostData method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetHostData_List() {
        System.out.println("getHostData");
        List<Host> hostList = new ArrayList<>();
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<HostMeasurement> expResult = new ArrayList<>();
        List<HostMeasurement> result = instance.getHostData(hostList);
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmData method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetVmData_VmDeployed() {
        System.out.println("getVmData");
        VmDeployed vm = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        VmMeasurement expResult = null;
        VmMeasurement result = instance.getVmData(vm);
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmData method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetVmData_0args() {
        System.out.println("getVmData");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<VmMeasurement> expResult = null;
        List<VmMeasurement> result = instance.getVmData();
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmData method, of class CompssDatasourceAdaptor.
     */
    @Test
    public void testGetVmData_List() {
        System.out.println("getVmData");
        List<VmDeployed> vmList = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        List<VmMeasurement> expResult = new ArrayList<>();
        List<VmMeasurement> result = instance.getVmData(vmList);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCurrentEnergyUsage method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetCurrentEnergyUsage() {
        System.out.println("getCurrentEnergyUsage");
        Host host = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        CurrentUsageRecord expResult = null;
        CurrentUsageRecord result = instance.getCurrentEnergyUsage(host);
        assertEquals(expResult, result);
    }

    /**
     * Test of getLowestHostPowerUsage method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetLowestHostPowerUsage() {
        System.out.println("getLowestHostPowerUsage");
        Host host = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        double expResult = 0.0;
        double result = instance.getLowestHostPowerUsage(host);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getHighestHostPowerUsage method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetHighestHostPowerUsage() {
        System.out.println("getHighestHostPowerUsage");
        Host host = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        double expResult = 0.0;
        double result = instance.getHighestHostPowerUsage(host);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getCpuUtilisation method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetCpuUtilisation() {
        System.out.println("getCpuUtilisation");
        Host host = null;
        int durationSeconds = 0;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");
        double expResult = 0.0;
        double result = instance.getCpuUtilisation(host, durationSeconds);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getApplicationData method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetApplicationData_ApplicationOnHost() {
        System.out.println("getApplicationData");
        ApplicationOnHost application = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        ApplicationMeasurement expResult = null;
        ApplicationMeasurement result = instance.getApplicationData(application);
        assertEquals(expResult, result);
    }

    /**
     * Test of getApplicationData method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetApplicationData_0args() {
        System.out.println("getApplicationData");
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");        
        List<ApplicationMeasurement> expResult = null;
        List<ApplicationMeasurement> result = instance.getApplicationData();
        assertEquals(expResult, result);
    }

    /**
     * Test of getApplicationData method, of class CompssDatasourceAdaptor.
     */
    //@Test //Commented out as this method is not supported by this adaptor
    public void testGetApplicationData_List() {
        System.out.println("getApplicationData");
        List<ApplicationOnHost> appList = null;
        CompssDatasourceAdaptor instance = new CompssDatasourceAdaptor();
        instance.setMonitoringDirectory("./test_example_files");
        instance.setMonitoringFile("/COMPSs_state.xml");            
        List<ApplicationMeasurement> expResult = null;
        List<ApplicationMeasurement> result = instance.getApplicationData(appList);
        assertEquals(expResult, result);
    }
    
}
