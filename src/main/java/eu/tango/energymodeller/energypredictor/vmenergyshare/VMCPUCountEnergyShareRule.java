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
package eu.tango.energymodeller.energypredictor.vmenergyshare;

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This allocates energy used by a host machine into the VMs that run upon it.
 * This takes the count of VMs CPU cores and divides energy out accordingly.
 * @author Richard Kavanagh
 */
public class VMCPUCountEnergyShareRule implements EnergyShareRule {

    /**
     * Translates a hosts energy usage into the VMs energy usage.
     * This takes the count of VMs CPU cores and divides energy out accordingly.
     * @param host The host to analyse
     * @param energyUsers The VMs that are on/to be on the host
     * @return The fraction of energy used per host.
     */
    @Override
    public EnergyDivision getEnergyUsage(Host host, Collection<EnergyUsageSource> energyUsers) {
        ArrayList<VmDeployed> vms = new ArrayList<>();
        for (EnergyUsageSource vm : energyUsers) {
            if (vm.getClass().equals(VmDeployed.class)) {
                vms.add((VmDeployed) vm);
            }
        }
        EnergyDivision answer = new EnergyDivision(host);
        for (VM vm : vms) {
            answer.addWeight(vm, vm.getCpus());
        }
        return answer;
    }
}