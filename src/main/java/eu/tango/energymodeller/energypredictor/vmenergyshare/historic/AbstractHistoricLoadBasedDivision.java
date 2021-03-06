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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The aim of this class is to provide the base methods for any implementation
 * of a Load based division mechanism that examines historical records.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractHistoricLoadBasedDivision implements HistoricLoadBasedDivision {

    protected Host host;
    protected final HashSet<EnergyUsageSource> energyusers = new HashSet<>();
    protected List<HostEnergyRecord> energyUsage;
    protected List<HostEnergyUserLoadFraction> loadFraction;

    /**
     * This creates a load based division mechanism for the specified host, that
     * is yet to be specified.
     */
    public AbstractHistoricLoadBasedDivision() {
    }

    /**
     * This creates a load based division mechanism for the specified host.
     *
     * @param host The host to divide energy for, among its VMs.
     */
    public AbstractHistoricLoadBasedDivision(Host host) {
        this.host = host;
    }

    /**
     * This returns the host who's energy is to be fractioned out.
     *
     * @return the host The host in which the VMs reside/are planned to reside.
     */
    @Override
    public Host getHost() {
        return host;
    }

    /**
     * This sets the host who's energy is to be fractioned out.
     *
     * @param host The host who's energy is to be divided among the VMs
     */
    @Override
    public void setHost(Host host) {
        this.host = host;
    }

    /**
     * This adds a VM to the energy division record.
     *
     * @param vm The VM to add to the energy division
     */
    @Override
    public void addEnergyUser(EnergyUsageSource vm) {
        energyusers.add(vm);
    }

    /**
     * This adds a collection of VM to the energy division record.
     *
     * @param energyUser The VM to add to the energy division
     */
    @Override
    public void addEnergyUser(Collection<EnergyUsageSource> energyUser) {
        energyusers.addAll(energyUser);
    }

    /**
     * This removes a VM from the energy division record.
     *
     * @param vm The VM to remove from the energy division
     */
    @Override
    public void removeEnergyUser(EnergyUsageSource vm) {
        energyusers.remove(vm);
    }

    /**
     * This gets the duration that a VM was running for during
     * the lifetime of the energy record describes.
     *
     * @param energyUser The VM to get the duration it was seen for
     * @return The duration in seconds a VM exists in the energy records.
     */
    @Override
    public long getDuration(EnergyUsageSource energyUser) {
        HostEnergyUserLoadFraction first = null;
        HostEnergyUserLoadFraction last = loadFraction.get(0);
        for (HostEnergyUserLoadFraction current : loadFraction) {
            if (current.getEnergyUsageSources().contains(energyUser) && first == null) {
                first = current;
            }
            if (current.getEnergyUsageSources().contains(energyUser)) {
                last = current;
            }            
        }
        if (first != null) {
            return last.getTime() - first.getTime();
        }
        return 0;
    }

    /**
     * This gets the duration that the energy records describe.
     *
     * @return The duration in seconds the energy records describe.
     */
    @Override
    public long getDuration() {
        if (energyUsage.isEmpty()) {
            return 0;
        }
        HostEnergyRecord first = energyUsage.get(0);
        HostEnergyRecord last = energyUsage.get(energyUsage.size() - 1);
        return last.getTime() - first.getTime();
    }

    /**
     * This returns the time of the first energy usage record that is set.
     *
     * @return The time of the first energy usage record. Null if no records are
     * set.
     */
    @Override
    public Calendar getStart() {
        if (energyUsage == null || energyUsage.isEmpty()) {
            return null;
        }
        GregorianCalendar answer = new GregorianCalendar();
        answer.setTimeInMillis(TimeUnit.SECONDS.toMillis(energyUsage.get(0).getTime()));
        return answer;
    }

    /**
     * This returns the time of the last energy usage record that is set.
     *
     * @return The time of the first energy usage record. Null if no records are
     * set.
     */
    @Override
    public Calendar getEnd() {
        if (energyUsage == null || energyUsage.isEmpty()) {
            return null;
        }
        GregorianCalendar answer = new GregorianCalendar();
        answer.setTimeInMillis(TimeUnit.SECONDS.toMillis(energyUsage.get(energyUsage.size() - 1).getTime()));
        return answer;
    }

    /**
     * This returns the energy usage for a named VM
     *
     * @param energyUser The VM to get energy usage for.
     * @return The energy used by this VM.
     */
    @Override
    public abstract double getEnergyUsage(EnergyUsageSource energyUser);

    /**
     * This lists VMs on the host machine.
     *
     * @return This VMs on the host machine.
     */
    @Override
    public Collection<EnergyUsageSource> getEnergyUserList() {
        return energyusers;
    }

    /**
     * The amount of VMs on the host machine.
     *
     * @return This count of how many VMs are on the host machine.
     */
    @Override
    public int getEnergyUserCount() {
        return energyusers.size();
    }

    /**
     * This sets energy usage record for the load based division mechanism. It
     * also places them in sorted order.
     *
     * @param energyUsage The energy usage data to use.
     */
    @Override
    public void setEnergyUsage(List<HostEnergyRecord> energyUsage) {
        /**
         * The base assumption is that the energy usage records match 1:1 in
         * time frame with the load fraction and are in sorted order.
         */
        this.energyUsage = energyUsage;
        Collections.sort(energyUsage);
    }

    /**
     * This sets load fraction records for the load based division mechanism. It
     * also places them in sorted order.
     *
     * @param loadFraction The load fraction data to use.
     */
    @Override
    public void setLoadFraction(List<HostEnergyUserLoadFraction> loadFraction) {
        /**
         * The base assumption is that the energy usage records match 1:1 in
         * time frame with the load fraction and are in sorted order.
         */
        this.loadFraction = loadFraction;
        Collections.sort(loadFraction);
    }

    /**
     * This compares the vm/application resource utilisation dataset and the host energy
     * data and ensures that they have a 1:1 mapping
     */
    public void cleanData() {

        if (energyUsage.size() != loadFraction.size()) {
            LinkedHashMap<HostEnergyRecord, HostEnergyUserLoadFraction> cleanedData = cleanData(loadFraction, energyUsage);
            energyUsage.clear();
            energyUsage.addAll(cleanedData.keySet());
            loadFraction.clear();
            loadFraction.addAll(cleanedData.values());
        }
    }

    /**
     * This compares the vm resource utilisation dataset and the host energy
     * data and ensures that they have a 1:1 mapping
     *
     * @param energyUserLoad The energy user's usage dataset
     * @param hostData The host's energy usage dataset
     * @return The mappings between each dataset elements
     */
    public LinkedHashMap<HostEnergyRecord, HostEnergyUserLoadFraction> cleanData(Collection<HostEnergyUserLoadFraction> energyUserLoad, List<HostEnergyRecord> hostData) {
        LinkedHashMap<HostEnergyRecord, HostEnergyUserLoadFraction> answer = new LinkedHashMap<>();
        //Make a copy and compare times remove them each time.
        LinkedList<HostEnergyUserLoadFraction> vmDataCopy = new LinkedList<>();
        vmDataCopy.addAll(energyUserLoad);
        LinkedList<HostEnergyRecord> hostDataCopy = new LinkedList<>();
        hostDataCopy.addAll(hostData);
        HostEnergyUserLoadFraction vmHead = vmDataCopy.pop();
        HostEnergyRecord hostHead = hostDataCopy.pop();
        while (!vmDataCopy.isEmpty() && !hostDataCopy.isEmpty()) {
            if (vmHead.getTime() == hostHead.getTime()) {
                answer.put(hostHead, vmHead);
                vmHead = vmDataCopy.pop();
                hostHead = hostDataCopy.pop();
            } else {
                //replace the youngest, given this is a sorted list.
                if (vmHead.getTime() < hostHead.getTime()) {
                    vmHead = vmDataCopy.pop();
                } else {
                    hostHead = hostDataCopy.pop();
                }
            }
        }
        return answer;
    }

}
