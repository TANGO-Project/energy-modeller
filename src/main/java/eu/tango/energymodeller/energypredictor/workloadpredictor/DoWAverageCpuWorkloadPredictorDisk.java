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
 * 
 * This is being developed for the TANGO Project: http://tango-project.eu
 * 
 */
package eu.tango.energymodeller.energypredictor.workloadpredictor;

import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.VmDiskImage;
import eu.tango.energymodeller.types.usage.VmLoadHistoryRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryWeekRecord;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This looks at disk references and returns the average CPU workload induced
 * by VMs as its estimate of CPU workload. It looks at the day and time of week
 * in order to produce this estimate.
 *
 * @author Richard Kavanagh
 */
public class DoWAverageCpuWorkloadPredictorDisk extends AbstractVMHistoryWorkloadEstimator {

    private int bootHistoryBucketSize = 500;

    @Override
    public double getCpuUtilisation(Host host, Collection<VM> virtualMachines) {
        double vmCount = 0; //vms with disk refs
        double sumCpuUtilisation = 0;
        if (hasDiskReferences(virtualMachines)) {
            for (VM vm : virtualMachines) {
                if (!vm.getDiskImages().isEmpty()) {
                    sumCpuUtilisation = sumCpuUtilisation + getAverageCpuUtilisation(vm).getUtilisation();
                    vmCount = vmCount + 1;
                }
            }
            if (vmCount == 0) {
                return 0.0;
            }            
            return sumCpuUtilisation / vmCount;
        } else {
            return 0;
        }
    }

    /**
     * This gets the average CPU utilisation for a VM given the disk references 
     * that it has.
     *
     * @param vm The VM to get the average utilisation for.
     * @return The average utilisation of all disk references that a VM has.
     */
    @Override
    public VmLoadHistoryRecord getAverageCpuUtilisation(VM vm) {
        double utilisation = 0.0;
        double stdDev = 0.0;
        GregorianCalendar cal = new GregorianCalendar();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);        
        if (vm.getDiskImages().isEmpty()) {
            return new VmLoadHistoryRecord(utilisation, stdDev);
        }
        for (VmDiskImage disk : vm.getDiskImages()) {
            if (vm.getClass().equals(VmDeployed.class)) {
                List<VmLoadHistoryWeekRecord> weekRecord = database.getAverageCPUUtilisationWeekTraceForDisk(disk.getDiskImage());
                VmLoadHistoryRecord answer = getUtilisation(weekRecord);
                utilisation = utilisation + answer.getUtilisation();
                stdDev = (stdDev < answer.getStdDev() ? answer.getStdDev() : stdDev);
            } else {
                utilisation = utilisation + database.getAverageCPUUtilisationDisk(disk.getDiskImage()).getUtilisation();
            }
        }
        return new VmLoadHistoryWeekRecord(dayOfWeek, hourOfDay, utilisation / vm.getDiskImages().size(), stdDev);
    }

    /**
     * This gets the load for the current day and hour of the week.
     * @param records The records to search through to get the hour and day of
     * the week.
     * @return The average load for the given day and time of week. If this
     * record is not found the average of all values in the record set is 
     * returned instead.
     */
    public VmLoadHistoryRecord getUtilisation(List<VmLoadHistoryWeekRecord> records) {
        double sumUtil = 0;
        double stdDev = 0.0;
        GregorianCalendar cal = new GregorianCalendar();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        for (VmLoadHistoryWeekRecord record : records) {
            if (record.getDayOfWeek() == dayOfWeek && record.getHourOfDay() == hourOfDay) {
                return new VmLoadHistoryWeekRecord(dayOfWeek, hourOfDay, record.getUtilisation(), record.getStdDev());
            }
            sumUtil = sumUtil + record.getUtilisation();
            stdDev = (stdDev < record.getStdDev() ? record.getStdDev() : stdDev);
        }
        return new VmLoadHistoryWeekRecord(dayOfWeek, hourOfDay, sumUtil / records.size(), stdDev);
    }

    /**
     * This sets the boot history discrete time bucket size.
     *
     * @return the bootHistoryBucketSize
     */
    public int getBootHistoryBucketSize() {
        return bootHistoryBucketSize;
    }

    /**
     * This sets the boot history discrete time bucket size.
     *
     * @param bootHistoryBucketSize The bucket size is the time in seconds that
     * each discrete time bucket represents.
     */
    public void setBootHistoryBucketSize(int bootHistoryBucketSize) {
        this.bootHistoryBucketSize = bootHistoryBucketSize;
    }
    
    @Override
    public String getName() {
        return "Day of Week Workload Disk Predictor";
    }     

}
