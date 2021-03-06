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
package eu.tango.energymodeller.types.usage;

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This represents the sources by which energy may be used within the energy modellers
 * framework. Namely a VM, (instantiated or not) and a host machine.
 * @author Richard Kavanagh
 */
public abstract class EnergyUsageRecord {

    private final ArrayList<EnergyUsageSource> energyUser = new ArrayList<>();    

    /**
     * This gets the energy users for the energy usage record.
     * @return the energy users that this record represents.
     */
    public Collection<EnergyUsageSource> getEnergyUser() {
        return energyUser;
    }
    
    /**
     * This adds a energy user to this summary record.
     * @param energyUser The energy user to add.
     */
    protected void addEnergyUser(EnergyUsageSource energyUser) {
        this.energyUser.add(energyUser);
    }        
    
    /**
     * This adds a set of energy user to this summary record. 
     * @param energyUsers The energy users to add.
     */
    protected void addEnergyUser(Collection<EnergyUsageSource> energyUsers) {
        energyUser.addAll(energyUsers);
    }    
    
    /**
     * Indicates if this record details energy usage for a given user.
     * @param energyUser The energy user.
     * @return If the energy user is represented in this record.
     */
    public boolean containsEnergyUser(EnergyUsageSource energyUser) {
        return this.energyUser.contains(energyUser);
    }    
    
}
