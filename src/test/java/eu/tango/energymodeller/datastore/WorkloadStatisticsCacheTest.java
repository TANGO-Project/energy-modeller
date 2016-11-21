/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.tango.energymodeller.datastore;

import eu.tango.energymodeller.datastore.WorkloadStatisticsCache;
import eu.tango.energymodeller.datasourceclient.MetricValue;
import eu.tango.energymodeller.datasourceclient.VmMeasurement;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Richard
 */
public class WorkloadStatisticsCacheTest {
    
    public WorkloadStatisticsCacheTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getInstance method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        WorkloadStatisticsCache expResult = WorkloadStatisticsCache.getInstance();
        WorkloadStatisticsCache result = WorkloadStatisticsCache.getInstance();
        assertEquals(expResult, result);
    }

    /**
     * Test of addVMToStatistics method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testAddVMToStatistics() {
        System.out.println("addVMToStatistics");
        VmDeployed vm = new VmDeployed(10105, "MyVM");
        vm.addApplicationTag("test");
        vm.addDiskImage("test");
        GregorianCalendar cal = new GregorianCalendar();
        long clock = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
        VmMeasurement measurement = new VmMeasurement(vm, clock);
        measurement.addMetric(new MetricValue("test", "test", "5", clock));
        measurement.addMetric(new MetricValue("cpu-measured", "cpu-measured", "5", clock));
        List<VmMeasurement> vmMeasurements = new ArrayList<>();
        vmMeasurements.add(measurement);
        WorkloadStatisticsCache instance = WorkloadStatisticsCache.getInstance();
        instance.addVMToStatistics(vmMeasurements);
    }

    /**
     * Test of getUtilisationforDisks method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetUtilisationforDisks() {
        System.out.println("getUtilisationforDisks");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getUtilisationforDisks(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBootUtilisationforDisks method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetBootUtilisationforDisks() {
        System.out.println("getBootUtilisationforDisks");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getBootUtilisationforDisks(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDoWUtilisationforDisks method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetDoWUtilisationforDisks() {
        System.out.println("getDoWUtilisationforDisks");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getDoWUtilisationforDisks(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUtilisationforTags method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetUtilisationforTags() {
        System.out.println("getUtilisationforTags");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getUtilisationforTags(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBootUtilisationforTags method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetBootUtilisationforTags() {
        System.out.println("getBootUtilisationforTags");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getBootUtilisationforTags(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDoWUtilisationforTags method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testGetDoWUtilisationforTags() {
        System.out.println("getDoWUtilisationforTags");
        VM vm = null;
        WorkloadStatisticsCache instance = null;
        double expResult = 0.0;
        double result = instance.getDoWUtilisationforTags(vm);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isInUse method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testIsInUse() {
        System.out.println("isInUse");
        WorkloadStatisticsCache instance = null;
        boolean expResult = false;
        boolean result = instance.isInUse();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setInUse method, of class WorkloadStatisticsCache.
     */
    @Test
    public void testSetInUse() {
        System.out.println("setInUse");
        boolean inUse = false;
        WorkloadStatisticsCache instance = null;
        instance.setInUse(inUse);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
