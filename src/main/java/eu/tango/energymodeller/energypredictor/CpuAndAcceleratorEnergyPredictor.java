/**
 * Copyright 2017 University of Leeds
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
import eu.tango.energymodeller.types.energymodel.NeuralNetFunction;
import eu.tango.energymodeller.types.energyuser.Accelerator;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyCalibrationData;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

/**
 * This implements the CPU polynomial energy predictor with accelerator support
 * for the Tango project.
 *
 * It performs simple polynomial fitting in order to determine from the CPU load
 * the current power consumption.
 *
 * @author Richard Kavanagh
 *
 */
public class CpuAndAcceleratorEnergyPredictor extends AbstractEnergyPredictor {

    private final LRUCache<Host, PredictorFunction<PolynomialFunction>> modelCache = new LRUCache<>(5, 50);
    private final LRUCache<Host, PredictorFunction<NeuralNetFunction>> modelAcceleratorCache = new LRUCache<>(5, 50);

    /**
     * This creates a new CPU only energy predictor that uses a polynomial fit.
     *
     * It will create a energy-modeller-predictor properties file if it doesn't
     * exist.
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
    public CpuAndAcceleratorEnergyPredictor() {
        super();
    }

    /**
     * This creates a new CPU only energy predictor that uses a polynomial fit.
     *
     * It will create a energy-modeller-predictor properties file if it doesn't
     * exist.
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
     */
    public CpuAndAcceleratorEnergyPredictor(PropertiesConfiguration config) {
        super(config);
    }

    @Override
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> workload, TimePeriod duration) {
        EnergyUsagePrediction wattsUsed;
        if (getDefaultAssumedCpuUsage() == -1) {
            wattsUsed = predictTotalEnergy(host, getCpuUtilisation(host, workload), getAcceleratorUtilisation(host, workload), duration);
        } else {
            wattsUsed = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), getAcceleratorUtilisation(host, workload), duration);
        }
        return wattsUsed;
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
            hostAnswer = predictTotalEnergy(host, getCpuUtilisation(host, VM.castToWorkloadSource(virtualMachines)), getAcceleratorUtilisation(host, VM.castToWorkloadSource(virtualMachines)), timePeriod);
        } else { 
            hostAnswer = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), getAcceleratorUtilisation(host, VM.castToWorkloadSource(virtualMachines)), timePeriod);
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
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost app, Collection<ApplicationOnHost> apps, Host host, TimePeriod timePeriod) {
        EnergyDivision division = getEnergyUsageForApps(host, apps);
        EnergyUsagePrediction hostAnswer;
        if (getDefaultAssumedCpuUsage() == -1) {
            hostAnswer = predictTotalEnergy(host, getCpuUtilisation(host, ApplicationOnHost.castToWorkloadSource(apps)), getAcceleratorUtilisation(host, null), timePeriod);
        } else {
            hostAnswer = predictTotalEnergy(host, getDefaultAssumedCpuUsage(), getAcceleratorUtilisation(host, null), timePeriod);
        }
        hostAnswer.setAvgPowerUsed(hostAnswer.getTotalEnergyUsed()
                / ((double) TimeUnit.SECONDS.toHours(timePeriod.getDuration())));
        EnergyUsagePrediction generalHostsAnswer = getGeneralHostPredictedEnergy(timePeriod);
        double generalPower = generalHostsAnswer.getAvgPowerUsed() / (double) apps.size();
        double generalEnergy = generalHostsAnswer.getTotalEnergyUsed() / (double) apps.size();
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
     * @param accUsage The representation of the accelerators workload
     * @param timePeriod The time period the prediction is for
     * @return The predicted energy usage.
     */
    public EnergyUsagePrediction predictTotalEnergy(Host host, double usageCPU, HashMap<Accelerator,HashMap<String, Double>> accUsage, TimePeriod timePeriod) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction(host);
        PolynomialFunction cpuModel = retrieveCpuModel(host).getFunction();
        double powerUsed = cpuModel.value(usageCPU);
        //TODO fix the accelerator usage to mutliple accelerator mapping issue.
        for (Accelerator accelerator : host.getAccelerators()) {
            NeuralNetFunction acceleratorModel = retrieveAcceleratorModel(host, accelerator.getName()).getFunction();
            powerUsed = powerUsed + acceleratorModel.value(accUsage.get(accelerator));
        }
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
        PolynomialFunction cpuModel = retrieveCpuModel(host).getFunction();
        if (getDefaultAssumedCpuUsage() == -1) {
            double power;
            power = cpuModel.value(getCpuUtilisation(host));
            for (Accelerator accelerator : host.getAccelerators()) {
                NeuralNetFunction accModel = retrieveAcceleratorModel(host, accelerator.getName()).getFunction();
                power = power + accModel.value(getAcceleratorUtilisation(host, null).get(accelerator));
            }
            return power;
        } else {
            //TODO consider if this is valid, it assumes the accelerator usage remains constant, given no input source for an alternative estimate
            double power = cpuModel.value(getDefaultAssumedCpuUsage());
            for (Accelerator accelerator : host.getAccelerators()) {
                NeuralNetFunction accModel = retrieveAcceleratorModel(host, accelerator.getName()).getFunction();
                power = power + accModel.value(getAcceleratorUtilisation(host, null).get(accelerator));
            }
            return power; 
        }
    }

    /**
     * This estimates the power used by a host, given its CPU load. It assumes
     * accelerator load remains the same.
     *
     * @param host The host to get the energy prediction for
     * @param usageCPU The amount of CPU load placed on the host
     * @return The predicted power usage.
     */
    @Override
    public double predictPowerUsed(Host host, double usageCPU) {
        PolynomialFunction model = retrieveCpuModel(host).getFunction();
        double power = model.value(usageCPU);
        for (Accelerator accelerator : host.getAccelerators()) {
            NeuralNetFunction accModel = retrieveAcceleratorModel(host, accelerator.getName()).getFunction();
            power = power + accModel.value(getAcceleratorUtilisation(host, null).get(accelerator));
        }       
        return power;        
    }

    /**
     * This calculates the mathematical function that predicts the power
     * consumption given the cpu utilisation.
     *
     * @param host The host to get the function for
     * @return The mathematical function that predicts the power consumption
     * given the cpu utilisation.
     */
    private PredictorFunction<PolynomialFunction> retrieveCpuModel(Host host) {
        PredictorFunction<PolynomialFunction> answer;
        if (modelCache.containsKey(host)) {
            /**
             * A small cache avoids recalculating the regression so often.
             */
            return modelCache.get(host);
        }
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (HostEnergyCalibrationData data : host.getCalibrationData()) {
            points.add(data.getCpuUsage(), data.getWattsUsed());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
        final double[] best = fitter.fit(points.toList());
        PolynomialFunction function = new PolynomialFunction(best);
        double sse = getSumOfSquareError(function, points.toList());
        double rmse = getRootMeanSquareError(sse, points.toList().size());
        answer = new PredictorFunction<>(function, sse, rmse);
        modelCache.put(host, answer);
        return answer;
    }

    /**
     * This calculates the mathematical function that predicts the power
     * consumption given the cpu utilisation.
     *
     * @param host The host to get the function for
     * @return The mathematical function that predicts the power consumption
     * given the cpu utilisation.
     */
    private PredictorFunction<NeuralNetFunction> retrieveAcceleratorModel(Host host, String accelerator) {
        PredictorFunction<NeuralNetFunction> answer;
        if (modelAcceleratorCache.containsKey(host)) {
            /**
             * A small cache avoids recalculating the regression so often.
             */
            return modelAcceleratorCache.get(host);
        }
        NeuralNetFunction function = new NeuralNetFunction(accelerator + ".csv");
        double sse = Double.NaN;
        double rmse = Double.NaN;
        answer = new PredictorFunction<>(function, sse, rmse);
        modelAcceleratorCache.put(host, answer);
        return answer;
    }

    /**
     * This performs a calculation to determine how close the fit is for a given
     * model.
     *
     * @param function The PolynomialFunction to assess
     * @param observed The actual set of observed points
     * @return The sum of the square error.
     */
    private double getSumOfSquareError(PolynomialFunction function, List<WeightedObservedPoint> observed) {
        double answer = 0;
        for (WeightedObservedPoint current : observed) {
            double error = current.getY() - function.value(current.getX());
            answer = answer + (error * error);
        }
        return answer;
    }
    
    //TODO fix the issue with calculating sum of square error for the model

    /**
     * This performs a calculation to determine how close the fit is for a given
     * model.
     *
     * @param function The PolynomialFunction to assess
     * @param observed The actual set of observed points
     * @return The sum of the square error.
     */
//    private double getSumOfSquareError(NeuralNetFunction function, List<WeightedObservedPoint> observed) {
//        double answer = 0;
//        for (WeightedObservedPoint current : observed) {
//            double error = current.getY() - function.value(current.getX());
//            answer = answer + (error * error);
//        }
//        return answer;
//    }

    /**
     * This calculates the root means square error
     *
     * @param sse The sum of the square error
     * @param count The count of observed points
     * @return the root means square error
     */
    private double getRootMeanSquareError(double sse, int count) {
        return Math.sqrt(sse / ((double) count));
    }

    @Override
    public double getSumOfSquareError(Host host) {
        try {
            return retrieveCpuModel(host).getSumOfSquareError();
        } catch (NumberIsTooSmallException ex) {
            return Double.MAX_VALUE;
        }
    }

    @Override
    public double getRootMeanSquareError(Host host) {
        try {
            return retrieveCpuModel(host).getRootMeanSquareError();
        } catch (NumberIsTooSmallException ex) {
            return Double.MAX_VALUE;
        }
    }

    @Override
    public void printFitInformation(Host host) {
        System.out.println(this.toString() + " - SSE: "
                + this.retrieveCpuModel(host).getSumOfSquareError()
                + " RMSE: " + this.retrieveCpuModel(host).getRootMeanSquareError());
    }

    @Override
    public String toString() {
        return "CPU (polynomial) and Accelerator (neural network) energy predictor";
    }

}
