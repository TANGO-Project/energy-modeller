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
package eu.tango.energymodeller.datastore;

import eu.tango.energymodeller.datasourceclient.VmMeasurement;
import eu.tango.energymodeller.datasourceclient.ZabbixDirectDbDataSourceAdaptor;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Richard
 */
public class DataGathererTest {

    public DataGathererTest() {
    }

    private final Host CHOSEN_HOST = new Host(10084, "asok10");
    private final Host CHOSEN_HOST2 = new Host(10106, "asok12");
    private final String VM_NAME = "cloudsuite---data-analytics"; //CloudSuite - Data Analytics
    public static DataGatherer gatherer = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        gatherer = getDataGatherer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        gatherer.stop();
    }

    private static DataGatherer getDataGatherer() {
        if (gatherer == null) {
            ZabbixDirectDbDataSourceAdaptor adaptor = new ZabbixDirectDbDataSourceAdaptor();
            DefaultDatabaseConnector connector = new DefaultDatabaseConnector();
            DataGatherer instance = new DataGatherer(adaptor, connector);
            gatherer = instance;
            Thread dataGatherThread = new Thread(instance);
            dataGatherThread.setDaemon(true);
            dataGatherThread.start();
        }
        return gatherer;
    }

    /**
     * Test of run method, of class DataGatherer.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        DataGatherer instance = getDataGatherer();
        Thread thread = new Thread(instance);
        thread.start();
        try {
            Thread.sleep(20000); //gather 20 seconds worth of data.
        } catch (InterruptedException ex) {
            fail("Something interuppted the thread while waiting for some data to be gathered");
        }
    }

    /**
     * Test of stop method, of class DataGatherer.
     */
    @Test
    public void testStop() {
        System.out.println("stop");
        //Always passing as the stop is called as part of the classes tear down.
    }

    /**
     * Test of getHostList method, of class DataGatherer.
     */
    @Test
    public void testGetHostList() {
        System.out.println("getHostList");
        DataGatherer instance = getDataGatherer();
        HashMap<String, Host> result = instance.getHostList();
        assert (!result.isEmpty());
    }

    /**
     * Test of getHost method, of class DataGatherer.
     */
    @Test
    public void testGetHost() {
        System.out.println("getHost");
        String hostname = "asok10";
        DataGatherer instance = getDataGatherer();
        Host expResult = CHOSEN_HOST;
        Host result = instance.getHost(hostname);
        assertEquals(expResult, result);
    }

    /**
     * Test of getVmList method, of class DataGatherer.
     */
    @Test
    public void testGetVmList() {
        System.out.println("getVmList");
        DataGatherer instance = getDataGatherer();
        HashMap<String, VmDeployed> result = instance.getVmList();
        assert (!result.isEmpty());
        for (VmDeployed vm : result.values()) {
            System.out.println("VM: " + vm.getName());
            //assert (vm.getAllocatedTo() != null);
            if (vm.getAllocatedTo() != null) {
                System.out.println("VM Host: " + vm.getAllocatedTo().getHostName());
            }
        }
    }

    /**
     * Test of getVm method, of class DataGatherer.
     */
    @Test
    public void testGetVm() {
        System.out.println("getVm");
        String name = VM_NAME;
        DataGatherer instance = getDataGatherer();
        Thread dataGatherThread = new Thread(instance);
        dataGatherThread.setDaemon(true);
        dataGatherThread.start();
        VmDeployed result = instance.getVm(name);
        assert (result != null);
        assertEquals(VM_NAME, result.getName());
    }

    /**
     * Test of getVMsOnHost method, of class DataGatherer.
     */
    @Test
    public void testGetVMsOnHost() {
        System.out.println("getVMsOnHost");
        Host host = CHOSEN_HOST2;
        DataGatherer instance = getDataGatherer();
        ArrayList<VmDeployed> result = instance.getVMsOnHost(host);
        for (VmDeployed vm : result) {
            System.out.println("VM: " + vm.getName());
            assert (vm.getAllocatedTo() != null);
            System.out.println("VM Host: " + vm.getAllocatedTo().getHostName());
        }
    }

    @Test
    public void customTest() {
        System.out.println("Custom Test - Code Patch - Line 116- 121");
        Host host = CHOSEN_HOST2;
        DataGatherer instance = getDataGatherer();
        ZabbixDirectDbDataSourceAdaptor datasource = new ZabbixDirectDbDataSourceAdaptor();
        ArrayList<VmDeployed> vms = instance.getVMsOnHost(host);
        if (!vms.isEmpty()) {
            HostEnergyUserLoadFraction fraction = new HostEnergyUserLoadFraction(host, (new GregorianCalendar().getTimeInMillis()) / 1000);
            List<VmMeasurement> vmMeasurements = datasource.getVmData(vms);
            fraction.setFraction(vmMeasurements);
            for (VmMeasurement vmMeasurement : vmMeasurements) {
                System.out.println("VM: " + vmMeasurement.getVm().getName());
                System.out.println("Fraction: " + fraction.getFraction(vmMeasurement.getVm()));
            }
        } else {
            fail("The VM was not found");
        }
    }

}
