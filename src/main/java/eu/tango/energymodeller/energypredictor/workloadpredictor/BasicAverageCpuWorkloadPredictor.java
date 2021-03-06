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

import eu.tango.energymodeller.datastore.WorkloadStatisticsCache;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.usage.VmLoadHistoryRecord;
import java.util.Collection;

/**
 * This looks at an application tag and returns the average CPU workload induced
 * by VMs as its estimate of CPU workload.
 *
 * @author Richard Kavanagh
 */
public class BasicAverageCpuWorkloadPredictor extends AbstractVMHistoryWorkloadEstimator {

    @Override
    public double getCpuUtilisation(Host host, Collection<VM> virtualMachines) {
        double vmCount = 0; //vms with app tags
        double sumCpuUtilisation = 0;
        if (hasAppTags(virtualMachines)) {
            for (VM vm : virtualMachines) {
                if (!vm.getApplicationTags().isEmpty()) {
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
     * This gets the average CPU utilisation for a VM given the app tags that it
     * has.
     *
     * @param vm The VM to get the average utilisation for.
     * @return The average utilisation of all application tags that a VM has.
     */
    @Override
    public VmLoadHistoryRecord getAverageCpuUtilisation(VM vm) {
        double utilisation = 0.0;
        double stdDev = 0.0;
        if (vm.getApplicationTags().isEmpty()) {
            return new VmLoadHistoryRecord(utilisation, stdDev);
        }
        if (WorkloadStatisticsCache.getInstance().isInUse()) {
            return new VmLoadHistoryRecord(WorkloadStatisticsCache.getInstance().getUtilisationforTags(vm), -1);
        }
        for (String tag : vm.getApplicationTags()) {
            VmLoadHistoryRecord answer = database.getAverageCPUUtilisationTag(tag);
            utilisation = utilisation + answer.getUtilisation();
            stdDev = (stdDev < answer.getStdDev() ? answer.getStdDev() : stdDev);
        }
        return new VmLoadHistoryRecord(utilisation / vm.getApplicationTags().size(), stdDev);
    }

    @Override
    public String getName() {
        return "Average Workload App Tag Predictor";
    }

}
