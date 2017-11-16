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
import static eu.tango.energymodeller.energypredictor.AbstractEnergyPredictor.CONFIG_FILE;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyDivision;
import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.energyuser.Accelerator;
import eu.tango.energymodeller.types.energyuser.usage.HostAcceleratorCalibrationData;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyCalibrationData;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

/**
 * This implements the CPU polynomial energy predictor with accelerator support
 * for the Tango project. It assumes the accelerator has only 2 states, such as
 * P0 and P8, where the accelerator such as a GPU is either busy or it is idle.
 * The power consumed is therefore based upon this assumption.
 *
 * It performs simple polynomial fitting in order to determine from the CPU load
 * the current power consumption.
 *
 * @author Richard Kavanagh
 *
 */
public class CpuAndBiModalAcceleratorEnergyPredictor extends AbstractEnergyPredictor {

    private final LRUCache<Host, PredictorFunction<PolynomialFunction>> modelCache = new LRUCache<>(5, 50);
    private final LRUCache<Host, PredictorFunction<GroupingFunction>> modelAcceleratorCache = new LRUCache<>(5, 50);
    //A much better definition "clocks.current.sm [MHz]"
    private String groupingParameter = "nvidia_value:null:percent";

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
    public CpuAndBiModalAcceleratorEnergyPredictor() {
        super();
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            readSettings(config);
        } catch (ConfigurationException ex) {
            Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.SEVERE,
                    "Taking the default load from the settings file did not work", ex);
        }        
    }
    
    /**
     * This takes the settings and reads them into memory and sets defaults as
     * needed.
     *
     * @param config The settings to read.
     */
    private void readSettings(PropertiesConfiguration config) {
        String dataSrcStr = config.getString("energy.modeller.grouping.parameter", groupingParameter);
        config.setProperty("energy.modeller.grouping.parameter", dataSrcStr);
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
    public CpuAndBiModalAcceleratorEnergyPredictor(PropertiesConfiguration config) {
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
        powerUsed = powerUsed + getCurrentAcceleratorPowerUsage(host, false);
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
        double power;
        PolynomialFunction cpuModel = retrieveCpuModel(host).getFunction();
        if (getDefaultAssumedCpuUsage() == -1) {
            power = cpuModel.value(getCpuUtilisation(host));
            power = power + getCurrentAcceleratorPowerUsage(host, false);
        } else {
            //TODO consider if this is valid, it assumes the accelerator usage remains constant, given no input source for an alternative estimate
            power = cpuModel.value(getDefaultAssumedCpuUsage());
            power = power + getCurrentAcceleratorPowerUsage(host, true);
        }
        return power;
    }
    
    /**
     * This gets the accelerators current power usage information.
     * @param host The host to get the power usage data for
     * @param useAssumedDefaultUsage Takes the default assumed usage value, otherwise
     * assumes current usage continues.
     * @return The current power consumption of the accelerators.
     */
    private double getCurrentAcceleratorPowerUsage(Host host, boolean useAssumedDefaultUsage) {
        double power = 0;
        double acceleratorClockRate = 0;
        if (useAssumedDefaultUsage) {
            acceleratorClockRate = getDefaultAssumedAcceleratorUsage();
        }
        //This guard ensures the host's accelerator usage data is available
        if (host.isAvailable()) {
            for (Accelerator accelerator : host.getAccelerators()) {
                if (!useAssumedDefaultUsage) {
                    acceleratorClockRate = getAcceleratorClockRate(host, accelerator);
                    System.out.println(host.getHostName() + " Current Clock Rate:" + acceleratorClockRate);
                }
                GroupingFunction acceleratorModel = retrieveAcceleratorModel(host, accelerator.getName()).getFunction();
                power = power + acceleratorModel.value(acceleratorClockRate);
            }
        }
        return power;
    }
 
    /**
     * This provides an average of the recent CPU utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average CPU utilisation over the last
     * n seconds will be calculated for.
     * @param accelerator The accelerator to get the information for
     * @return The average recent CPU utilisation based upon the energy
     * predictor's configured observation window.
     */
    protected double getAcceleratorClockRate(Host host,Accelerator accelerator) {
        return getAcceleratorClockRate(host, accelerator, null);
    }        
    
    /**
     * This provides an average of the recent CPU utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average CPU utilisation over the last
     * n seconds will be calculated for.
     * @param accelerator The accelerator to get the information for
     * @param accUsage The representation of the accelerators workload
     * @return The average recent CPU utilisation based upon the energy
     * predictor's configured observation window.
     */
    protected double getAcceleratorClockRate(Host host,Accelerator accelerator, HashMap<Accelerator,HashMap<String, Double>> accUsage) {
        double answer = 0.0;
        try {
            HashMap<String,Double> values;
            if (accUsage == null) {
                values = getAcceleratorUtilisation(host, null).get(accelerator);
            } else {
                values = accUsage.get(accelerator);
            }
            if (values.isEmpty()) {
                Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.WARNING, "An error occured, no load data was available! Host: {0} : Accelerator {1}", new Object[]{host != null ? host : "null", accelerator != null ? accelerator.getName() : "null"});    
            }
            if (values.containsKey(groupingParameter)) {
                answer = values.get(groupingParameter);
            } else {
                Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.WARNING, "The load data item needed was not available! Host: {0} : Accelerator {1}", new Object[]{host != null ? host : "null", accelerator != null ? accelerator.getName() : "null"});
            }
        } catch (Exception ex) {
            Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.SEVERE, "An error occured! Host: " + (host != null ? host : "null") + " : Accelerator " + (accelerator != null ? accelerator.getName() : "null"), ex);
        }
        return answer;
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
        power = power + getCurrentAcceleratorPowerUsage(host, false);     
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
     * The assumption of this predictor function is that the calibration data
     * fits into two states: working or idle (based upon a single parameter).
     */
    private class GroupingFunction {

        private double lowestGroup;
        private double highestGroup;
        //First field is the group id, second is the power consumption
        private final HashMap<Double, Double> totalPower = new HashMap<>();
        private final HashMap<Double, Double> countPower = new HashMap<>();

        /**
         * This applies a fit to the data, it groups by the first field and
         * averages the second (power). It therefore assumes if the first fields
         * value is seen again the best answer the function should return is the
         * average.
         *
         * @param points The list of data points
         */
        public void fit(WeightedObservedPoints points) {
            for (WeightedObservedPoint point : points.toList()) {
                if (point.getX() < lowestGroup) {
                    lowestGroup = point.getX();
                }
                if (point.getX() > highestGroup) {
                    highestGroup = point.getX();
                }
                Double power = totalPower.get(point.getX());
                double count;
                if (power == null) {
                    power = 0.0;
                    countPower.put(point.getX(), 0.0);
                }
                power = power + point.getY();
                count = countPower.get(point.getX()) + 1.0;
                totalPower.put(point.getX(), power);
                countPower.put(point.getX(), count);
            }
        }

        /**
         * This returns the value associated with a given input to the function.
         * There should only be two outputs of this function. One for the busy
         * case and one for the idle case.
         *
         * @param input The input for workload into the model
         * @return The output power consumption for the model.
         */
        public double value(double input) {
            if (totalPower.isEmpty()) {
                Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.WARNING, "No calibration data found for grouping function.");
                return 0.0;
            }
            if (input < 0) {
                Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.WARNING, "Incorrect input frequency value was: {0}", input);
                return 0.0;
            }            
            if (totalPower.containsKey(input)) {
                return totalPower.get(input) / countPower.get(input);
            }
            double proximityToLower = input - lowestGroup;
            double proximityToHigher = highestGroup - input;
            if (proximityToLower < proximityToHigher) {
                return totalPower.get(lowestGroup) / countPower.get(lowestGroup);
            } else {
                return totalPower.get(highestGroup) / countPower.get(highestGroup);
            }
        }

    }

    /**
     * This calculates the mathematical function that predicts the power
     * consumption given the cpu utilisation.
     *
     * @param host The host to get the function for
     * @return The mathematical function that predicts the power consumption
     * given the cpu utilisation.
     */
    private PredictorFunction<GroupingFunction> retrieveAcceleratorModel(Host host, String accelerator) {
        PredictorFunction<GroupingFunction> answer;
        if (modelAcceleratorCache.containsKey(host)) {
            /**
             * A small cache avoids recalculating the regression so often.
             */
            return modelAcceleratorCache.get(host);
        }
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (Accelerator acc : host.getAccelerators()) {
            for (HostAcceleratorCalibrationData data : acc.getAcceleratorCalibrationData()) {
                if (data.getIdentifier().equals(accelerator) && data.hasParameter(groupingParameter)) {
                    points.add(data.getParameter(groupingParameter), data.getPower());
                } else {
                    String parameterlist = "";
                    for (String dataParam : data.getParameters()) {
                        parameterlist = parameterlist + ":" + dataParam;
                    }
                    Logger.getLogger(CpuAndBiModalAcceleratorEnergyPredictor.class.getName()).log(Level.SEVERE, "Failed to Calibrate: {0} for {1}. Valid Parameters: {2}", new Object[]{accelerator, groupingParameter, parameterlist});                    
                }
            }
        }
        GroupingFunction function = new GroupingFunction();
        function.fit(points);
        double sse = getSumOfSquareError(function, points.toList());
        double rmse = getRootMeanSquareError(sse, points.toList().size());
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
    
    /**
     * This performs a calculation to determine how close the fit is for a given
     * model.
     *
     * @param function The PolynomialFunction to assess
     * @param observed The actual set of observed points
     * @return The sum of the square error.
     */
    private double getSumOfSquareError(GroupingFunction function, List<WeightedObservedPoint> observed) {
        double answer = 0;
        for (WeightedObservedPoint current : observed) {
            double error = current.getY() - function.value(current.getX());
            answer = answer + (error * error);
        }
        return answer;
    }    

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
        return "CPU (polynomial) and Accelerator (bimodal) energy predictor";
    }

}
