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
 * 
 * This is being developed for the TANGO Project: http://tango-project.eu
 * 
 */
package eu.tango.energymodeller.energypredictor.vmenergyshare.historic;

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import eu.tango.energymodeller.types.usage.HostEnergyRecord;

/**
 * This creates a load based division mechanism for dividing host energy among
 * VMs. It is intended to be used with historic load data.
 *
 * Energy is fractioned out taking into account the amount of load has been
 * placed on each machine. This is done by ratio.
 *
 * @author Richard Kavanagh
 */
public class LoadBasedDivision extends AbstractHistoricLoadBasedDivision {

    /**
     * This creates a load based division mechanism for the specified host, that
     * is yet to be specified.
     */
    public LoadBasedDivision() {
    }

    /**
     * This creates a load based division mechanism for the specified host.
     *
     * @param host The host to divide energy for, among its VMs.
     */
    public LoadBasedDivision(Host host) {
        super(host);
    }

    /**
     * This returns the energy usage for a named VM
     *
     * @param energyUser The VM to get energy usage for.
     * @return The energy used by this VM.
     */
    @Override
    public double getEnergyUsage(EnergyUsageSource energyUser) {
        VmDeployed deployed = (VmDeployed) energyUser;
        cleanData();
        int recordCount = (energyUsage.size() <= loadFraction.size() ? energyUsage.size() : loadFraction.size());

        /**
         * Calculate the energy used by a VM taking into account the work it has
         * performed.
         */
        double vmEnergy = 0;
        //Access two records at once hence ensure size() -2
        for (int i = 0; i <= recordCount - 2; i++) {
            HostEnergyRecord energy1 = energyUsage.get(i);
            HostEnergyRecord energy2 = energyUsage.get(i + 1);
            HostEnergyUserLoadFraction load1 = loadFraction.get(i);
            HostEnergyUserLoadFraction load2 = loadFraction.get(i + 1);
            if (load1.getEnergyUsageSources().contains(deployed) && load2.getEnergyUsageSources().contains(deployed)) {
                long timePeriod = energy2.getTime() - energy1.getTime();
                double deltaEnergy = Math.abs((((double) timePeriod) / 3600d)
                        * (energy1.getPower() + load1.getHostPowerOffset()
                        + energy2.getPower() + load2.getHostPowerOffset()) * 0.5);
                double avgLoadFraction = (load1.getFraction(deployed) + load2.getFraction(deployed)) / 2;
                vmEnergy = vmEnergy + (deltaEnergy * avgLoadFraction);
            }
        }
        return vmEnergy;
    }
}
