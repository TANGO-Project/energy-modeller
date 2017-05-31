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
package eu.tango.energymodeller.types.energyuser.usage;

import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import java.util.Objects;

/**
 * The purpose of this class is to give a record of the VMs or applications usage. 
 * In doing so it allows for the energy to be fractioned based upon the record provided.
 * @author Richard Kavanagh
 */
public class UsageRecord implements Comparable<UsageRecord> {

    private final EnergyUsageSource energyUser;
    private final long time;
    private final double load;    

    /**
     * This creates a new energy usage record that indicates how much load
     * has been induced by a given vm or application.
     * @param energyUser The vm or app that induced the load
     * @param time The time when the load was induced.
     * @param load The percentage of full system load that was induced.
     */
    public UsageRecord(EnergyUsageSource energyUser, long time, double load) {
        this.energyUser = energyUser;
        this.time = time;
        this.load = load;
    }

    /**
     * This returns the energy user that induced the load for this usage record.
     * @return The vm or application that induced the load
     */
    public EnergyUsageSource getEnergyUser() {
        return energyUser;
    }

    /**
     * This returns the time when the load was induced.
     * @return The time when the load was induced
     */
    public long getTime() {
        return time;
    }        
    
    /**
     * This returns the load percentage that was induced.
     * @return The returns the CPU load that was induced at the time specified
     * in this usage record.
     */
    public double getLoad() {
        return load;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UsageRecord) {
            UsageRecord compareTo = (UsageRecord) obj; 
            return ((time == compareTo.getTime()) && energyUser.equals(compareTo.getEnergyUser()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.energyUser);
        hash = 59 * hash + (int) (this.time ^ (this.time >>> 32));
        return hash;
    }
    
    /**
     * This comparison orders vm usage records by time.
     * @param usageRecord the usage record to compare to.
     * @return -1 if the before, 0 if at the same time 1 if in the future.
     */
    @Override
    public int compareTo(UsageRecord usageRecord) {
        return Long.valueOf(this.time).compareTo(usageRecord.getTime());
    }    
   
}
