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
package eu.tango.energymodeller.types.usage;

import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import java.util.Calendar;
import java.util.HashSet;

/**
 *
 * This stores a record of the historical energy usage of either a VM or a
 * underlying resource. It is expected to indicate the: Avg Watts used over time
 * Avg Current (useful??) Avg Voltage (useful??) kWh of energy used since
 * instantiation/boot
 *
 * @author Richard Kavanagh
 */
public class HistoricUsageRecord extends EnergyUsageRecord {

    private double avgPowerUsed; // units Watts
    private double avgCurrentUsed; //units Amps
    private double avgVoltageUsed; //units Volts
    private double totalEnergyUsed; //units kWh
    private TimePeriod duration; // The time period to which the results correspond.

    /**
     * This creates a historic usage record, for the energy user specified.
     *
     * @param energyUser The energy user.
     */
    public HistoricUsageRecord(EnergyUsageSource energyUser) {
        addEnergyUser(energyUser);
    }

    /**
     * This creates a historic usage record, for the energy user specified. This
     * allows a summary record to be setup for a set of hosts or VMs.
     *
     * @param energyUsers The energy users.
     */
    public HistoricUsageRecord(HashSet<EnergyUsageSource> energyUsers) {
        addEnergyUser(energyUsers);
    }

    /**
     * This creates an historic usage record and allows every value to be
     * specified during its construction.
     *
     * @param energyUser The source of the energy usage.
     * @param avgPowerUsed The average power used.
     * @param avgCurrentUsed The average current.
     * @param avgVoltageUsed The average voltage used.
     * @param totalEnergyUsed The total amount of energy used.
     */
    public HistoricUsageRecord(HashSet<EnergyUsageSource> energyUser, double avgPowerUsed, double avgCurrentUsed, double avgVoltageUsed, double totalEnergyUsed) {
        addEnergyUser(energyUser);
        this.avgPowerUsed = avgPowerUsed;
        this.avgCurrentUsed = avgCurrentUsed;
        this.avgVoltageUsed = avgVoltageUsed;
        this.totalEnergyUsed = totalEnergyUsed;
    }

    /**
     * This provides the average power used.
     *
     * @return The average power used.
     */
    public double getAvgPowerUsed() {
        return avgPowerUsed;
    }

    /**
     * This sets the average power used.
     *
     * @param avgPowerUsed The average power used.
     */
    public void setAvgPowerUsed(double avgPowerUsed) {
        this.avgPowerUsed = avgPowerUsed;
    }

    /**
     * This provides the average current used.
     *
     * @return The average current used.
     */
    public double getAvgCurrentUsed() {
        return avgCurrentUsed;
    }

    /**
     * This sets the average current used.
     *
     * @param avgCurrentUsed The average current used.
     */
    public void setAvgCurrentUsed(double avgCurrentUsed) {
        this.avgCurrentUsed = avgCurrentUsed;
    }

    /**
     * This provides the average voltage used.
     *
     * @return The average voltage used.
     */
    public double getAvgVoltageUsed() {
        return avgVoltageUsed;
    }

    /**
     * This sets the average voltage used.
     *
     * @param avgVoltageUsed The average voltage used.
     */
    public void setAvgVoltageUsed(double avgVoltageUsed) {
        this.avgVoltageUsed = avgVoltageUsed;
    }

    /**
     * This provides the total energy used.
     *
     * @return the total energy used.
     */
    public double getTotalEnergyUsed() {
        return totalEnergyUsed;
    }

    /**
     * This sets the total energy used.
     *
     * @param totalEnergyUsed the new value for the total energy used.
     */
    public void setTotalEnergyUsed(double totalEnergyUsed) {
        this.totalEnergyUsed = totalEnergyUsed;
    }

    /**
     * This provides the start time for this energy record.
     *
     * @return the time of the first data item this record represents.
     */
    public Calendar getRecordStartTime() {
        return getDuration().getStartTime();
    }

    /**
     * This provides the end time for this energy record.
     *
     * @return the time of the last data item this record represents.
     */
    public Calendar getRecordEndTime() {
        return getDuration().getEndTime();
    }

    /**
     * This returns the duration of time this historic record represents.
     *
     * @return The duration of time this record represents.
     */
    public TimePeriod getDuration() {
        return duration;
    }

    /**
     * This sets the duration of time this historic record represents.
     *
     * @param duration The duration of time this record represents.
     */
    public void setDuration(TimePeriod duration) {
        this.duration = duration;
    }

}
