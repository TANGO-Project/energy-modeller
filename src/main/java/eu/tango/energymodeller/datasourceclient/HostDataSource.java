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
package eu.tango.energymodeller.datasourceclient;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost.JOB_STATUS;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.util.List;

/**
 * This is the interface for all data sources that the Energy modeller uses,
 * that concerns Hosts.
 *
 * @author Richard Kavanagh
 */
public interface HostDataSource {
    
    /**
     * This returns a host given its unique name.
     *
     * @param hostname The name of the host to get.
     * @return The object representation of a host in the energy modeller.
     */
    public Host getHostByName(String hostname);
    
    /**
     * This returns a host that's dedicated to the data centers general services,
     * such as a distributed file system given its unique name.
     *
     * @param hostname The name of the host to get.
     * @return The object representation of a general purpose host in the energy modeller.
     */    
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname);

    /**
     * This returns a host given its unique name.
     *
     * @param name The name of the host to get.
     * @return The object representation of a host in the energy modeller.
     */
    public VmDeployed getVmByName(String name);

    /**
     * This provides a list of hosts for the energy modeller
     *
     * @return A list of hosts for the energy modeller.
     */
    public List<Host> getHostList();

    /**
     * This provides a list of hosts and VMs for the energy modeller
     * @return A list of energy users for the energy modeller.
     */
    public List<EnergyUsageSource> getHostAndVmList();
    
    /**
     * This provides a list of file general power consumers hosts for the 
     * energy modeller
     *
     * @return A list of power consumers that provide general services for 
     * physical hosts, i.e. distributed file systems etc.
     */    
    public List<GeneralPurposePowerConsumer> getGeneralPowerConsumerList();    
    
    /**
     * This provides a list of VMs for the energy modeller
     *
     * @return A list of vms for the energy modeller.
     */
    public List<VmDeployed> getVmList();

    /**
     * This provides a list of applications running on a particular host
     *
     * @param state The job status, null for all (which is equivalent to the 
     * call getHostApplicationList()
     * @return A list of applications running on the hosts.
     */
    public List<ApplicationOnHost> getHostApplicationList(JOB_STATUS state);    

    /**
     * This provides a list of applications running on a particular host
     *
     * @return A list of applications running on the hosts.
     */
    public List<ApplicationOnHost> getHostApplicationList();    
    
    /**
     * This provides for the named host all the information that is available.
     *
     * @param host The host to get the measurement data for.
     * @return The host measurement data
     */
    public HostMeasurement getHostData(Host host);

    /**
     * This lists for all host all the metric data on them.
     *
     * @return A list of host measurements
     */
    public List<HostMeasurement> getHostData();

    /**
     * This takes a list of hosts and provides all the metric data on them.
     *
     * @param hostList The list of hosts to get the data from
     * @return A list of host measurements
     */
    public List<HostMeasurement> getHostData(List<Host> hostList);

    /**
     * This provides for the named vm all the information that is available.
     *
     * @param vm The vm to get the measurement data for.
     * @return The vm measurement data
     */
    public VmMeasurement getVmData(VmDeployed vm);

    /**
     * This lists for all vms all the metric data on them.
     *
     * @return A list of vm measurements
     */
    public List<VmMeasurement> getVmData();

    /**
     * This takes a list of vms and provides all the metric data on them.
     *
     * @param vmList The list of vms to get the data from
     * @return A list of vm measurements
     */
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList);

    /**
     * This provides the current energy usage for a named host.
     *
     * @param host The host to get the current energy data for.
     * @return The current energy usage data of the named host.
     */
    public CurrentUsageRecord getCurrentEnergyUsage(Host host);

    /**
     * This finds the lowest/resting power usage by a client.
     *
     * @param host The host to get the lowest power usage data for.
     * @return The lowest i.e. resting power usage of a host
     */
    public double getLowestHostPowerUsage(Host host);

    /**
     * This finds the highest power usage by a host.
     *
     * @param host The host to get the highest power usage data for.
     * @return The highest power usage of a host
     */
    public double getHighestHostPowerUsage(eu.tango.energymodeller.types.energyuser.Host host);
    
    /**
     * This finds the cpu utilisation of a host, over the last n seconds.
     * @param host The host to get the cpu utilisation data for.
     * @param durationSeconds The amount of seconds to get the data for
     * @return The average utilisation of the host.
     */
    public double getCpuUtilisation(eu.tango.energymodeller.types.energyuser.Host host, int durationSeconds);    
    
}
