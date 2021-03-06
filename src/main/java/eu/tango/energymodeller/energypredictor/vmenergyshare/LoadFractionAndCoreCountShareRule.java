/**
 * Copyright 2016 University of Leeds
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

import eu.tango.energymodeller.datasourceclient.ApplicationMeasurement;
import eu.tango.energymodeller.datasourceclient.VmMeasurement;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
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
 * share of energy that is should have based upon this. It also accounts for 
 * the CPU core count so that a VM with more cores takes a greater fraction of
 * the overall power consumption.
 * @author Richard Kavanagh
 */
public class LoadFractionAndCoreCountShareRule implements EnergyShareRule {

    private HashMap<EnergyUsageSource, Double> fractions = new HashMap<>();
    
    @Override
    public EnergyDivision getEnergyUsage(Host host, Collection<EnergyUsageSource> energyUsers) {
        EnergyDivision answer = new EnergyDivision(host);
        for (EnergyUsageSource energyUser : energyUsers) {
            answer.addWeight(energyUser, fractions.get(energyUser));
            if (energyUser instanceof VmDeployed) {
                Logger.getLogger(LoadFractionAndCoreCountShareRule.class.getName()).log(Level.FINE, "VM: {0} Ratio: {1}", new Object[]{((VmDeployed)energyUser).getName(), fractions.get(energyUser)});
            } else if (energyUser instanceof ApplicationOnHost) {
                Logger.getLogger(LoadFractionAndCoreCountShareRule.class.getName()).log(Level.FINE, "APP: {0} Ratio: {1}", new Object[]{((ApplicationOnHost)energyUser).getName(), fractions.get(energyUser)});
            }
        }
        return answer;
    }

    /**
     * This sets the load fractions to use in this energy share rule.
     * @param vmMeasurements The Vm measurements that are used to set this load
     * fraction data.
     */
    public void setVmMeasurements(List<VmMeasurement> vmMeasurements) {
        fractions = HostEnergyUserLoadFraction.getFraction(vmMeasurements, true);
    }   

    /**
     * This sets the load fractions to use in this energy share rule.
     * @param appMeasurements The application measurements that are used to set this load
     * fraction data.
     */
    public void setApplicationMeasurements(List<ApplicationMeasurement> appMeasurements) {
        fractions = HostEnergyUserLoadFraction.getApplicationFraction(appMeasurements);
    }       
    
    /**
     * This returns the data that indicates which VMs/apps should take which fraction
     * of the overall energy.
     * @return the fractioning of the host energy data.
     */
    public HashMap<EnergyUsageSource, Double> getFractions() {
        return fractions;
    }

    /**
     * This allows the data that indicates which VMs/apps should take which fraction
     * of the overall energy to be directly set.
     * @param fractions the fractioning of the host energy data to set.
     */
    public void setFractions(HashMap<EnergyUsageSource, Double> fractions) {
        this.fractions = fractions;
    }

}
