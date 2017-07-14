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
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import eu.tango.energymodeller.types.usage.HostEnergyRecord;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Richard Kavanagh
 */
public interface HistoricLoadBasedDivision {

    /**
     * This returns the host who's energy is to be fractioned out.
     *
     * @return the host The host in which the VMs or applications resided.
     */
    public Host getHost();
    /**
     * This sets the host who's energy is to be fractioned out.
     *
     * @param host The host who's energy is to be divided among the VMs or applications
     */
    public void setHost(Host host);
    
/**
     * This adds a VM or application to the energy division record.
     *
     * @param energyUser The VM or application to add to the energy division
     */
    public void addEnergyUser(EnergyUsageSource energyUser);

    /**
     * This adds a collection of VMs or applications to the energy division record.
     *
     * @param energyUsers The VMs or applications to add to the energy division
     */
    public void addEnergyUser(Collection<EnergyUsageSource> energyUsers);

    /**
     * This removes a VM or application from the energy division record.
     *
     * @param energyUser The VM or application to remove from the energy division
     */
    public void removeEnergyUser(EnergyUsageSource energyUser);

    /**
     * This gets the duration that a VM or application was running for during
     * the lifetime of the energy record describes.
     *
     * @param energyUser The VM or application to get the duration it was seen for
     * @return The duration in seconds the energy records describe.
     */  
    public long getDuration(EnergyUsageSource energyUser); 
    
    /**
     * This gets the duration that the energy records describe.
     *
     * @return The duration in seconds the energy records describe.
     */
    public long getDuration();

    /**
     * This returns the time of the first energy usage record that is set.
     *
     * @return The time of the first energy usage record. Null if no records are
     * set.
     */
    public Calendar getStart();

    /**
     * This returns the time of the last energy usage record that is set.
     *
     * @return The time of the first energy usage record. Null if no records are
     * set.
     */
    public Calendar getEnd();

    /**
     * This returns the energy usage for a named VM or application
     *
     * @param energyUser The VM or application to get energy usage for.
     * @return The energy used by the named VM or application.
     */
    public double getEnergyUsage(EnergyUsageSource energyUser);

    /**
     * This lists VMs or applications on the host machine.
     * @return  The VMs or applications on the host machine.
     */
    public Collection<EnergyUsageSource> getEnergyUserList();

    /**
     * The amount of VMs or applications on the host machine.
     * @return This count of how many VMs or applications are on the host machine.
     */
    public int getEnergyUserCount();

    /**
     * This sets energy usage record for the load based division mechanism. It
     * also places them in sorted order.
     *
     * @param energyUsage The energy usage data to use.
     */
    public void setEnergyUsage(List<HostEnergyRecord> energyUsage);

    /**
     * This sets load fraction records for the load based division mechanism. It
     * also places them in sorted order.
     *
     * @param loadFraction The load fraction data to use.
     */
    public void setLoadFraction(List<HostEnergyUserLoadFraction> loadFraction);
    
}
