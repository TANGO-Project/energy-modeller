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

import eu.tango.energymodeller.datasourceclient.VmMeasurement;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This looks at the fraction of load placed on each VM and determines the 
 * share of energy that is should have based upon this.
 * @author Richard Kavanagh
 */
public class LoadFractionShareRule implements EnergyShareRule {

    private HashMap<EnergyUsageSource, Double> fractions = new HashMap<>();
    
    @Override
    public EnergyDivision getEnergyUsage(Host host, Collection<EnergyUsageSource> energyUsers) {
        EnergyDivision answer = new EnergyDivision(host);
        for (EnergyUsageSource energyUser : energyUsers) {
            VmDeployed deployed = (VmDeployed) energyUser;
            answer.addWeight(energyUser, fractions.get(deployed));
            Logger.getLogger(LoadFractionShareRule.class.getName()).log(Level.FINE, "VM: {0} Ratio: {1}", new Object[]{deployed.getName(), fractions.get(deployed)});
        }
        return answer;
    }

    /**
     * This sets the load fractions to use in this energy share rule.
     * @param vmMeasurements The Vm measurements that are used to set this load
     * fraction data.
     */
    public void setVmMeasurements(List<VmMeasurement> vmMeasurements) {
        fractions = HostEnergyUserLoadFraction.getFraction(vmMeasurements);
    }

    /**
     * This returns the data that indicates which VMs should take which fraction
     * of the overall energy.
     * @return the fractioning of the host energy data.
     */
    public HashMap<EnergyUsageSource, Double> getFractions() {
        return fractions;
    }

    /**
     * This allows the data that indicates which VMs should take which fraction
     * of the overall energy to be directly set.
     * @param fractions the fractioning of the host energy data to set.
     */
    public void setFractions(HashMap<EnergyUsageSource, Double> fractions) {
        this.fractions = fractions;
    }

}
