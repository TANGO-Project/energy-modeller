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

import eu.ascetic.ioutils.caching.LRUCache;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyDivision;
import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energymodel.LinearFunction;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyCalibrationData;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * This implements the CPU only energy predictor for the ASCETiC project.
 *
 * It performs simple linear regression in order to determine from the CPU load
 * the current power consumption.
 *
 * @author Richard Kavanagh
 *
 */
public class CpuOnlyEnergyPredictor extends AbstractEnergyPredictor {

    private final LRUCache<Host, PredictorFunction<LinearFunction>> modelCache = new LRUCache<>(5, 50);

    /**
     * This creates a new CPU only energy predictor that uses a linear fit.
     *
     * It will create a energy-modeller-predictor properties file if it
     * doesn't exist.
     *
     * The main property: energy.modeller.energy.predictor.cpu.default_load
     * should be in the range 0..1 or -1. This indicates the predictor's default
     * assumption on how much load is been induced. -1 measures the CPU's
     * current load and uses that to forecast into the future.
     *
     * In the case of using -1 as a parameter to additional parameters are used:
     * energy.modeller.energy.predictor.cpu.utilisation.observe_time.sec
     * energy.modeller.energy.predictor.cpu.utilisation.observe_time.min
     *
     * These indicate the window of how long the CPU should be monitored for, to
     * determine the current load.
     */
    public CpuOnlyEnergyPredictor() {
        super();
    }
    
    /**
     * This creates a new CPU only energy predictor that uses a linear fit.
     *
     * It will create a energy-modeller-predictor properties file if it
     * doesn't exist.
     *
     * The main property: energy.modeller.energy.predictor.cpu.default_load
     * should be in the range 0..1 or -1. This indicates the predictor's default
     * assumption on how much load is been induced. -1 measures the CPU's
     * current load and uses that to forecast into the future.
     *
     * In the case of using -1 as a parameter to additional parameters are used:
     * energy.modeller.energy.predictor.cpu.utilisation.observe_time.sec
     * energy.modeller.energy.predictor.cpu.utilisation.observe_time.min
     *
     * These indicate the window of how long the CPU should be monitored for, to
     * determine the current load.
     * 
     * @param config The config to use in order to create the abstract energy
     * predictor.    
     * 
     */
    public CpuOnlyEnergyPredictor(PropertiesConfiguration config) {
        super(config);
    }    

    @Override
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> workload, TimePeriod duration) {
        EnergyUsagePrediction energyUsage;
        if (getDefaultAssumedCpuUsage() == -1) {
            energyUsage = predictTotalEnergy(host, getCpuUtilisation(host, workload), duration);
        } else {
            energyUsage = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), duration);
        }
        return energyUsage;
    }
    
    /**
     * This provides a prediction of how much energy is to be used by a VM
     *
     * @param vm The vm to be deployed
     * @param virtualMachines The virtual machines giving a workload on the host
     * machine
     * @param host The host that the VMs will be running on
     * @param timePeriod The time period the query should run for.
     * @return The prediction of the energy to be used.
     */
    @Override
    public EnergyUsagePrediction getVMPredictedEnergy(VM vm, Collection<VM> virtualMachines, Host host, TimePeriod timePeriod) {
        EnergyDivision division = getEnergyUsageForVMs(host, virtualMachines);
        EnergyUsagePrediction hostAnswer;
        if (getDefaultAssumedCpuUsage() == -1) {
            hostAnswer = predictTotalEnergy(host, getCpuUtilisation(host, VM.castToWorkloadSource(virtualMachines)), timePeriod);
        } else {
            hostAnswer = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), timePeriod);
        }
        hostAnswer.setAvgPowerUsed(hostAnswer.getTotalEnergyUsed()
                / ((double) TimeUnit.SECONDS.toHours(timePeriod.getDuration())));
        EnergyUsagePrediction generalHostsAnswer = getGeneralHostPredictedEnergy(timePeriod);
        double generalPower = generalHostsAnswer.getAvgPowerUsed() / (double) virtualMachines.size();
        double generalEnergy = generalHostsAnswer.getTotalEnergyUsed() / (double) virtualMachines.size();
        EnergyUsagePrediction answer = new EnergyUsagePrediction(vm);
        answer.setDuration(hostAnswer.getDuration());
        //Find the fraction to be associated with the VM
        double vmsEnergyFraction = division.getEnergyUsage(hostAnswer.getTotalEnergyUsed(), vm);
        division.setConsiderIdleEnergy(isConsiderIdleEnergy());
        answer.setTotalEnergyUsed(vmsEnergyFraction + generalEnergy);
        double vmsPowerFraction = division.getEnergyUsage(hostAnswer.getAvgPowerUsed(), vm);
        answer.setAvgPowerUsed(vmsPowerFraction + generalPower);
        return answer;
    }

    @Override
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost app, Collection<ApplicationOnHost> applications, Host host, TimePeriod timePeriod) {
        EnergyDivision division = getEnergyUsageForApps(host, applications);
        EnergyUsagePrediction hostAnswer;
        if (getDefaultAssumedCpuUsage() == -1) {
            hostAnswer = predictTotalEnergy(host, getCpuUtilisation(host, ApplicationOnHost.castToWorkloadSource(applications)), timePeriod);
        } else {
            hostAnswer = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), timePeriod);
        }
        hostAnswer.setAvgPowerUsed(hostAnswer.getTotalEnergyUsed()
                / ((double) TimeUnit.SECONDS.toHours(timePeriod.getDuration())));
        EnergyUsagePrediction generalHostsAnswer = getGeneralHostPredictedEnergy(timePeriod);
        double generalPower = generalHostsAnswer.getAvgPowerUsed() / (double) applications.size();
        double generalEnergy = generalHostsAnswer.getTotalEnergyUsed() / (double) applications.size();
        EnergyUsagePrediction answer = new EnergyUsagePrediction(app);
        answer.setDuration(hostAnswer.getDuration());
        //Find the fraction to be associated with the application
        double appEnergyFraction = division.getEnergyUsage(hostAnswer.getTotalEnergyUsed(), app);
        division.setConsiderIdleEnergy(isConsiderIdleEnergy());
        answer.setTotalEnergyUsed(appEnergyFraction + generalEnergy);
        double appPowerFraction = division.getEnergyUsage(hostAnswer.getAvgPowerUsed(), app);
        answer.setAvgPowerUsed(appPowerFraction + generalPower);
        return answer;
    }

    /**
     * This predicts the total amount of energy used by a host.
     *
     * @param host The host to get the energy prediction for
     * @param usageCPU The amount of CPU load placed on the host
     * @param timePeriod The time period the prediction is for
     * @return The predicted energy usage.
     */
    public EnergyUsagePrediction predictTotalEnergy(Host host, double usageCPU, TimePeriod timePeriod) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction(host);
        LinearFunction model = retrieveModel(host).getFunction();
        double powerUsed = model.value(usageCPU);
        answer.setAvgPowerUsed(powerUsed);
        answer.setTotalEnergyUsed(powerUsed * ((double) TimeUnit.SECONDS.toHours(timePeriod.getDuration())));
        answer.setDuration(timePeriod);
        return answer;
    }

    /**
     * This estimates the power used by a host, given its CPU load. The CPU load
     * value is determined from the settings file.
     *
     * @param host The host to get the energy prediction for.
     * @return The predicted power usage.
     */
    @Override
    public double predictPowerUsed(Host host) {
        LinearFunction model = retrieveModel(host).getFunction();
        if (getDefaultAssumedCpuUsage() == -1) {
            return model.value(getCpuUtilisation(host));
        } else {
            return model.value(getDefaultAssumedCpuUsage());
        }
    }

    /**
     * This estimates the power used by a host, given its CPU load.
     *
     * @param host The host to get the energy prediction for
     * @param usageCPU The amount of CPU load placed on the host
     * @return The predicted power usage.
     */
    @Override    
    public double predictPowerUsed(Host host, double usageCPU) {
        LinearFunction model = retrieveModel(host).getFunction();
        return model.value(usageCPU);
    }

    /**
     * This calculates the mathematical function that predicts the power
     * consumption given the cpu utilisation.
     *
     * @param host The host to get the function for
     * @return The mathematical function that predicts the power consumption
     * given the cpu utilisation.
     */
    private PredictorFunction<LinearFunction> retrieveModel(Host host) {
        if (modelCache.containsKey(host)) {
            /**
             * A small cache avoids recalculating the regression so often.
             */
            return modelCache.get(host);
        }
        LinearFunction model = new LinearFunction();
        SimpleRegression regressor = new SimpleRegression(true);
        for (HostEnergyCalibrationData data : host.getCalibrationData()) {
            regressor.addData(data.getCpuUsage(), data.getWattsUsed());
        }
        model.setIntercept(regressor.getIntercept());
        model.setCoefficient(regressor.getSlope());
        PredictorFunction<LinearFunction> answer = new PredictorFunction<>(model,
                regressor.getSumSquaredErrors(),
                Math.sqrt(regressor.getMeanSquareError()));
        modelCache.put(host, answer);
        return answer;
    }

    @Override
    public double getSumOfSquareError(Host host) {
        return retrieveModel(host).getSumOfSquareError();
    }

    @Override
    public double getRootMeanSquareError(Host host) {
        return retrieveModel(host).getRootMeanSquareError();
    }
    
    @Override
    public void printFitInformation(Host host) {
        System.out.println(this.toString() + " - SSE: " + 
                this.retrieveModel(host).getSumOfSquareError() + 
                " RMSE: " + this.retrieveModel(host).getRootMeanSquareError());
    }       
    
    @Override
    public String toString() {
        return "CPU only linear energy predictor";
    }    

}
