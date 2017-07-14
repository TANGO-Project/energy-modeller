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
package eu.tango.energymodeller.energypredictor.vmenergyshare;

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import java.util.Collection;

/**
 * This allocates energy used by a host machine into the energy users (VMs and applications) that run upon it.
 * This takes the count of VMs/Applications and evenly divides energy out amongst them.
 *
 * @author Richard Kavanagh
 */
public class DefaultEnergyShareRule implements EnergyShareRule {

    /**
     * Translates a hosts energy usage into the VMs or applications energy usage.
     * This takes the count of VMs or applications and evenly divides energy out amongst them.
     * @param host The host to analyse
     * @param energyUsers The VMs that are on/to be on the host
     * @return The fraction of energy used per host.
     */
    @Override
    public EnergyDivision getEnergyUsage(Host host, Collection<EnergyUsageSource> energyUsers) {
        EnergyDivision answer = new EnergyDivision(host);
        for (EnergyUsageSource energyUser : energyUsers) {
            answer.addWeight(energyUser, 1);
        }
        return answer;
    }
}
