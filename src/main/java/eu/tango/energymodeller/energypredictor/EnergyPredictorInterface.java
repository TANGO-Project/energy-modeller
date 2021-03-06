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
package eu.tango.energymodeller.energypredictor;

import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.util.Collection;

/**
 * This is the standard interface for any energy predictor module to be loaded
 * into the Tango architecture.
 *
 * @author Richard Kavanagh
 */
public interface EnergyPredictorInterface {

    /**
     * This provides a prediction of how much energy is to be used by a host in
     * the next hour.
     *
     * @param host The host to get the energy prediction for
     * @param workloadsource The virtual machines or apps giving a workload on the host
     * machine
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> workloadsource);

    /**
     * This provides a prediction of how much energy is to be used by a host in
     * a specified period of time.
     *
     * @param host The host to get the energy prediction for
     * @param virtualMachines The virtual machines giving a workload on the host
     * machine
     * @param timePeriod The time period to run the prediction for
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> virtualMachines, TimePeriod timePeriod);

    /**
     * This provides a prediction of how much energy is to be used by a VM in
     * the next hour.
     *
     * @param vm The vm to be deployed
     * @param virtualMachines The virtual machines that are expected to be on
     * the physical host that therefore induce a workload on the host
     * @param host The host that the VMs will be running on
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getVMPredictedEnergy(VM vm, Collection<VM> virtualMachines, Host host);

    /**
     * This provides a prediction of how much energy is to be used by a VM, in a
     * specified period of time.
     *
     * @param vm The vm to be deployed
     * @param virtualMachines The virtual machines giving a workload on the host
     * machine
     * @param host The host that the VMs will be running on
     * @param timePeriod The time period to run the prediction for
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getVMPredictedEnergy(VM vm, Collection<VM> virtualMachines, Host host, TimePeriod timePeriod);

    /**
     * This provides a prediction of how much energy is to be used by a application 
     * in the next hour.
     *
     * @param app The application to be deployed
     * @param applications The applications that are expected to be on
     * the physical host that therefore induce a workload on the host
     * @param host The host that the applications will be running on
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost app, Collection<ApplicationOnHost> applications, Host host);

    /**
     * This provides a prediction of how much energy is to be used by a application, 
     * in a specified period of time.
     *
     * @param application The application to be deployed
     * @param applications The applications giving a workload on the host
     * machine
     * @param host The host that the applications will be running on
     * @param timePeriod The time period to run the prediction for
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost application, Collection<ApplicationOnHost> applications, Host host, TimePeriod timePeriod);    
    
    /**
     * This estimates the power used by a host, given its load characteristics.
     *
     * @param host The host to get the energy prediction for.
     * @return The predicted power usage.
     */
    public double predictPowerUsed(Host host);

    /**
     * This estimates the power used by a host, given its CPU load.
     *
     * @param host The host to get the energy prediction for
     * @param usageCPU The amount of CPU load placed on the host
     * @return The predicted power usage.
     */
    public double predictPowerUsed(Host host, double usageCPU);    
    
    /**
     * This determines how good the fit of the model is in regards to a
     * particular named host
     *
     * @param host The host that the energy predictions are for
     * @return The sum of the square error
     */
    public double getSumOfSquareError(Host host);

    /**
     * This determines how good the fit of the model is in regards to a
     * particular named host
     *
     * @param host The host that the energy predictions are for
     * @return The root mean square error
     */
    public double getRootMeanSquareError(Host host);
    
    /**
     * This outputs information about how good a fit is provided the predictor.
     * @param host The host to check the fit for.
     */    
    public void printFitInformation(Host host);
}
