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

import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.WattsUpMeterDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.ZabbixDirectDbDataSourceAdaptor;
import eu.tango.energymodeller.datastore.DatabaseConnector;
import eu.tango.energymodeller.datastore.DefaultDatabaseConnector;
import eu.tango.energymodeller.energypredictor.vmenergyshare.DefaultEnergyShareRule;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyDivision;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyShareRule;
import eu.tango.energymodeller.energypredictor.workloadpredictor.AbstractVMHistoryWorkloadEstimator;
import eu.tango.energymodeller.energypredictor.workloadpredictor.CpuRecentHistoryWorkloadPredictor;
import eu.tango.energymodeller.energypredictor.workloadpredictor.WorkloadEstimator;
import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.Accelerator;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This implements the default and utility functions for an energy predictor. It
 * is expected that any energy predictor loaded into the ASCETiC architecture,
 * will override this class.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractEnergyPredictor implements EnergyPredictorInterface {

    protected static final String CONFIG_FILE = "energy-modeller-predictor.properties";
    private static final String DEFAULT_DATA_SOURCE_PACKAGE = "eu.tango.energymodeller.datasourceclient";
    private static final String DEFAULT_WORKLOAD_PREDICTOR_PACKAGE = "eu.tango.energymodeller.energypredictor.workloadpredictor";
    private double defaultAssumedCpuUsage = 0.6; //assumed 60 percent usage, by default
    private double defaultAssumedAcceleratorUsage = 0.0; //assumed 0 percent usage, by default
    private double defaultPowerOverheadPerHost = 0; //Overhead from DFS etc
    private HostDataSource source = null;
    protected DatabaseConnector database = null;
    private boolean considerIdleEnergy = true;
    private WorkloadEstimator workloadEstimator = null;

    private EnergyShareRule energyShareRule = new DefaultEnergyShareRule();
    private static final String DEFAULT_ENERGY_SHARE_RULE_PACKAGE
            = "eu.tango.energymodeller.energypredictor.vmenergyshare";

    /**
     * This creates a new abstract energy predictor.
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
    public AbstractEnergyPredictor() {
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
            Logger.getLogger(CpuOnlyEnergyPredictor.class.getName()).log(Level.SEVERE,
                    "Taking the default load from the settings file did not work", ex);
        }
    }

    /**
     * This creates a new abstract energy predictor.
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
    public AbstractEnergyPredictor(PropertiesConfiguration config) {
        readSettings(config);
    }

    /**
     * This takes the settings and reads them into memory and sets defaults as
     * needed.
     *
     * @param config The settings to read.
     */
    private void readSettings(PropertiesConfiguration config) {
        String workloadPredictorStr = config.getString("energy.modeller.energy.predictor.workload.predictor", "CpuRecentHistoryWorkloadPredictor");
        config.setProperty("energy.modeller.energy.predictor.workload.predictor", workloadPredictorStr);
        setWorkloadPredictor(workloadPredictorStr);
        defaultAssumedCpuUsage = config.getDouble("energy.modeller.energy.predictor.default_load", defaultAssumedCpuUsage);
        config.setProperty("energy.modeller.energy.predictor.default_load", defaultAssumedCpuUsage);
        defaultAssumedAcceleratorUsage = config.getDouble("energy.modeller.energy.predictor.default_load_acc", defaultAssumedAcceleratorUsage);
        config.setProperty("energy.modeller.energy.predictor.default_load_acc", defaultAssumedAcceleratorUsage);        
        String shareRule = config.getString("energy.modeller.energy.predictor.share_rule", "DefaultEnergyShareRule");
        config.setProperty("energy.modeller.energy.predictor.share_rule", shareRule);
        setEnergyShareRule(shareRule);
        considerIdleEnergy = config.getBoolean("energy.modeller.energy.predictor.consider_idle_energy", considerIdleEnergy);
        config.setProperty("energy.modeller.energy.predictor.consider_idle_energy", considerIdleEnergy);
        defaultPowerOverheadPerHost = config.getDouble("energy.modeller.energy.predictor.overheadPerHostInWatts", defaultPowerOverheadPerHost);
        config.setProperty("energy.modeller.energy.predictor.overheadPerHostInWatts", defaultPowerOverheadPerHost);
        if (defaultAssumedCpuUsage == -1) {
            String dataSrcStr = config.getString("energy.modeller.energy.predictor.datasource", "ZabbixDirectDbDataSourceAdaptor");
            config.setProperty("energy.modeller.energy.predictor.datasource", dataSrcStr);
            setDataSource(dataSrcStr);
        }
    }

    /**
     * This allows the energy predictor's data source to be set
     *
     * @param dataSource The name of the data source to use for this energy
     * predictor
     */
    protected void setDataSource(String dataSource) {
        try {
            if (!dataSource.startsWith(DEFAULT_DATA_SOURCE_PACKAGE)) {
                dataSource = DEFAULT_DATA_SOURCE_PACKAGE + "." + dataSource;
            }
            /**
             * This is a special case that requires it to be loaded under the
             * singleton design pattern.
             */
            String wattsUpMeter = DEFAULT_DATA_SOURCE_PACKAGE + ".WattsUpMeterDataSourceAdaptor";
            if (wattsUpMeter.equals(dataSource)) {
                source = WattsUpMeterDataSourceAdaptor.getInstance();
            } else {
                source = (HostDataSource) (Class.forName(dataSource).newInstance());
            }
        } catch (ClassNotFoundException ex) {
            if (source == null) {
                source = new ZabbixDirectDbDataSourceAdaptor();

            }
            Logger.getLogger(AbstractEnergyPredictor.class
                    .getName()).log(Level.WARNING, "The data source specified was not found", ex);
        } catch (InstantiationException | IllegalAccessException ex) {
            if (source == null) {
                source = new ZabbixDirectDbDataSourceAdaptor();

            }
            Logger.getLogger(AbstractEnergyPredictor.class
                    .getName()).log(Level.WARNING, "The data source did not work", ex);
        }
    }

    /**
     * This allows the energy predictor's workload predictor to be set
     *
     * @param workloadPredictor The name of the workload predictor to use for
     * this energy predictor
     */
    private void setWorkloadPredictor(String workloadPredictor) {
        try {
            if (!workloadPredictor.startsWith(DEFAULT_WORKLOAD_PREDICTOR_PACKAGE)) {
                workloadPredictor = DEFAULT_WORKLOAD_PREDICTOR_PACKAGE + "." + workloadPredictor;
            }
            workloadEstimator = (WorkloadEstimator) (Class.forName(workloadPredictor).newInstance());
            workloadEstimator.setDataSource(source);
        } catch (ClassNotFoundException ex) {
            if (workloadEstimator == null) {
                workloadEstimator = new CpuRecentHistoryWorkloadPredictor();
                workloadEstimator.setDataSource(source);

            }
            Logger.getLogger(AbstractEnergyPredictor.class
                    .getName()).log(Level.WARNING, "The workload predictor specified was not found", ex);
        } catch (InstantiationException | IllegalAccessException ex) {
            if (workloadEstimator == null) {
                workloadEstimator = new CpuRecentHistoryWorkloadPredictor();
                workloadEstimator.setDataSource(source);

            }
            Logger.getLogger(AbstractEnergyPredictor.class
                    .getName()).log(Level.WARNING, "The workload predictor did not work", ex);
        }
        //Set the workload estimators database if it requires one.
        if (workloadEstimator.requiresVMInformation()) {
            database = new DefaultDatabaseConnector();
        } else {
            if (database != null) {
                database.closeConnection();
                database = null;
            }
        }
    }

    /**
     * This uses the current energy share rule for the energy predictor allowing
     * for the translation between host energy usage and VMs energy usage.
     *
     * @param host The host to analyse
     * @param vms The VMs that are on/to be on the host
     * @return The fraction of energy or used per host.
     */
    public EnergyDivision getEnergyUsageForVMs(Host host, Collection<VM> vms) {
        ArrayList<EnergyUsageSource> energyUsers = new ArrayList<>();
        energyUsers.addAll(vms);
        return energyShareRule.getEnergyUsage(host, energyUsers);
    }
    
    /**
     * This uses the current energy share rule for the energy predictor allowing
     * for the translation between host energy usage and VMs energy usage.
     *
     * @param host The host to analyse
     * @param apps The apps that are on/to be on the host
     * @return The fraction of energy or used per host.
     */
    public EnergyDivision getEnergyUsageForApps(Host host, Collection<ApplicationOnHost> apps) {
        ArrayList<EnergyUsageSource> energyUsers = new ArrayList<>();
        energyUsers.addAll(apps);
        return energyShareRule.getEnergyUsage(host, energyUsers);
    }    

    /**
     * This uses the current energy share rule for the energy predictor allowing
     * for the translation between host energy usage and VMs energy usage.
     *
     * @param host The host to analyse
     * @param energyUser The VMs that are on/to be on the host
     * @return The fraction of energy or used per host.
     */
    public EnergyDivision getEnergyUsage(Host host, Collection<EnergyUsageSource> energyUser) {
        return energyShareRule.getEnergyUsage(host, energyUser);
    }

    /**
     * This returns the current energy share rule that is in use by the energy
     * predictor.
     *
     * @return the energyShareRule The rule that divides the energy usage of
     * hosts into each VM.
     */
    public EnergyShareRule getEnergyShareRule() {
        return energyShareRule;
    }

    /**
     * This sets the current energy share rule that is in use by the energy
     * predictor.
     *
     * @param energyShareRule The rule that divides the energy usage of hosts
     * into each VM.
     */
    public final void setEnergyShareRule(EnergyShareRule energyShareRule) {
        this.energyShareRule = energyShareRule;
    }

    /**
     * This sets the current energy share rule that is in use by the energy
     * predictor.
     *
     * @param energyShareRule The rule that divides the energy usage of hosts
     * into each VM.
     */
    public final void setEnergyShareRule(String energyShareRule) {
        try {
            if (!energyShareRule.startsWith(DEFAULT_ENERGY_SHARE_RULE_PACKAGE)) {
                energyShareRule = DEFAULT_ENERGY_SHARE_RULE_PACKAGE + "." + energyShareRule;
            }
            this.energyShareRule = (EnergyShareRule) (Class.forName(energyShareRule).newInstance());
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            if (this.energyShareRule == null) {
                this.energyShareRule = new DefaultEnergyShareRule();

            }
            Logger.getLogger(AbstractEnergyPredictor.class
                    .getName()).log(Level.WARNING, "The energy share rule specified was not found", ex);
        }
    }

    /**
     * This indicates if this energy predictor when estimating VM energy usage
     * should consider idle energy or not.
     *
     * @return If idle energy is been considered when allocating energy to VMs
     */
    public boolean isConsiderIdleEnergy() {
        return considerIdleEnergy;
    }

    /**
     * This sets if this energy predictor when estimating VM energy usage should
     * consider idle energy or not.
     *
     * @param considerIdleEnergy If idle energy is been considered when
     * allocating energy to VMs
     */
    public void setConsiderIdleEnergy(boolean considerIdleEnergy) {
        this.considerIdleEnergy = considerIdleEnergy;
    }

    /**
     * This returns the default amount of CPU utilisation that is assumed, if an
     * estimation mechanism is not utilised.
     *
     * @return the default amount of CPU utilisation to be used during energy
     * estimation.
     */
    public double getDefaultAssumedCpuUsage() {
        return defaultAssumedCpuUsage;
    }

    /**
     * This sets the default amount of CPU utilisation that is assumed, if an
     * estimation mechanism is not utilised.
     *
     * @param usageCPU the default amount of CPU utilisation to be used during
     * energy estimation.
     */
    public void setDefaultAssumedCpuUsage(double usageCPU) {
        this.defaultAssumedCpuUsage = usageCPU;
    }
    
    /**
     * This returns the default amount of accelerator utilisation that is assumed, if an
     * estimation mechanism is not utilised.
     *
     * @return the default amount of accelerator utilisation to be used during energy
     * estimation.
     */
    public double getDefaultAssumedAcceleratorUsage() {
        return defaultAssumedAcceleratorUsage;
    }

    /**
     * This sets the default amount of accelerator utilisation that is assumed, if an
     * estimation mechanism is not utilised.
     *
     * @param usageAccelerator the default amount of accelerator utilisation to be used during
     * energy estimation.
     */
    public void setDefaultAssumedAcceleratorUsage(double usageAccelerator) {
        this.defaultAssumedAcceleratorUsage = usageAccelerator;
    }    

    /**
     * This provides a prediction of how much energy is to be used by a VM, over
     * the next hour.
     *
     * @param vm The vm to be deployed
     * @param virtualMachines The virtual machines giving a workload on the host
     * machine
     * @param host The host that the VMs will be running on
     * @return The prediction of the energy to be used.
     */
    @Override
    public EnergyUsagePrediction getVMPredictedEnergy(VM vm, Collection<VM> virtualMachines, Host host) {
        TimePeriod duration = new TimePeriod(new GregorianCalendar(), TimeUnit.HOURS.toSeconds(1));
        return getVMPredictedEnergy(vm, virtualMachines, host, duration);
    }

    /**
     * This provides a prediction of how much energy is to be used by a host in
     * the next hour.
     *
     * @param host The host to get the energy prediction for
     * @param workloadSource The virtual machines or apps providing a workload on the host
     * machine
     * @return The prediction of the energy to be used.
     */
    @Override
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> workloadSource) {
        TimePeriod duration = new TimePeriod(new GregorianCalendar(), 1, TimeUnit.HOURS);
        return getHostPredictedEnergy(host, workloadSource, duration);
    }

    @Override
    /**
     * This provides a prediction of how much energy is to be used by a application, 
     * over the next hour.
     *
     * @param app The application to be deployed
     * @param applications The giving a workload on the host machine
     * @param host The host that the applications will be running on
     * @return The prediction of the energy to be used.
     */
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost app, Collection<ApplicationOnHost> applications, Host host) {
        TimePeriod duration = new TimePeriod(new GregorianCalendar(), TimeUnit.HOURS.toSeconds(1));
        return getApplicationPredictedEnergy(app, applications, host, duration);
    }

    /**
     * This for a set of VMs provides the amount of memory allocated in Mb.
     *
     * @param virtualMachines The VMs to get the memory used.
     * @return The amount of memory allocated to VMs in Mb.
     */
    public static int getAlloacatedMemory(Collection<VM> virtualMachines) {
        int answer = 0;
        for (VM vm : virtualMachines) {
            answer = answer + vm.getRamMb();
        }
        return answer;
    }

    /**
     * This for a set of VMs provides the amount of memory allocated in Mb.
     *
     * @param virtualMachines The VMs to get the memory used.
     * @return The amount of memory allocated to VMs in Mb.
     */
    public static int getAlloacatedCpus(Collection<VM> virtualMachines) {
        int answer = 0;
        for (VM vm : virtualMachines) {
            answer = answer + vm.getCpus();
        }
        return answer;
    }

    /**
     * This for a set of VMs provides the amount of memory allocated in Mb.
     *
     * @param virtualMachines The VMs to get the memory used.
     * @return The amount of memory allocated to VMs in Mb.
     */
    public static double getAlloacatedDiskSpace(Collection<VM> virtualMachines) {
        double answer = 0;
        for (VM vm : virtualMachines) {
            answer = answer + vm.getDiskGb();
        }
        return answer;
    }
    
    /**
     * This provides an average of the recent CPU utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average CPU utilisation over the last
     * n seconds will be calculated for.
     * @param energyUser The source of workload
     * @return The average recent CPU utilisation based upon the energy
     * predictor's configured observation window.
     */
    protected HashMap<Accelerator,HashMap<String, Double>> getAcceleratorUtilisation(Host host, Collection<WorkloadSource> energyUser) {
        return workloadEstimator.getAcceleratorUtilisation(host, energyUser);
    }    

    /**
     * This provides an average of the recent CPU utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average CPU utilisation over the last
     * n seconds will be calculated for.
     * @return The average recent CPU utilisation based upon the energy
     * predictor's configured observation window.
     */
    protected double getCpuUtilisation(Host host) {
        return workloadEstimator.getCpuUtilisation(host, null);
    }

    /**
     * This provides an average of the recent CPU utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average CPU utilisation over the last
     * n seconds will be calculated for.
     * @param energyUser The source of workload
     * @return The average recent CPU utilisation based upon the energy
     * predictor's configured observation window.
     */
    protected double getCpuUtilisation(Host host, Collection<WorkloadSource> energyUser) {
        if (workloadEstimator instanceof AbstractVMHistoryWorkloadEstimator && workloadEstimator.requiresVMInformation()) {
            return workloadEstimator.getCpuUtilisation(host, energyUser);
        } else {
            return workloadEstimator.getCpuUtilisation(host, null);
        }
    }

    /**
     * This is a method that picks the best predictor for a host based upon the
     * root mean square error produced by the predictor.
     *
     * @param host The host the predictors should be assessed against.
     * @param predictors The collection of predictors to assess.
     * @return The predictor with the least calibration error.
     */
    public static EnergyPredictorInterface getBestPredictor(Host host, Collection<EnergyPredictorInterface> predictors) {
        EnergyPredictorInterface answer = null;
        for (EnergyPredictorInterface predictor : predictors) {
            try {
                if (answer == null || predictor.getRootMeanSquareError(host) < answer.getRootMeanSquareError(host)) {
                    answer = predictor;
                }
            } catch (Exception e) {
                /**
                 * Catch any errors from due to the inability to calibrate, such
                 * as not enough data points etc.
                 */
            }
        }
        return answer;
    }

    /**
     * This predicts the total amount of energy used by a general service hosts.
     * The overhead is calculated on a per host basis.
     *
     * @param timePeriod The time period the prediction is for
     * @return The predicted energy usage.
     */
    public EnergyUsagePrediction getGeneralHostPredictedEnergy(TimePeriod timePeriod) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction();
        answer.setAvgPowerUsed(defaultPowerOverheadPerHost);
        if (timePeriod != null) {
            answer.setTotalEnergyUsed(defaultPowerOverheadPerHost * ((double) TimeUnit.SECONDS.toHours(timePeriod.getDuration())));
            answer.setDuration(timePeriod);
        } else {
            answer.setTotalEnergyUsed(defaultPowerOverheadPerHost * 1.0); //W * Hrs
        }
        return answer;

    }

    /**
     * TODO Add utility functions here that may be used by the energy models
     * that are created over the time of the project.
     */
    /**
     * The predictor function class represents a wrap around of a predictor and
     * its estimated error.
     *
     * @param <T> The type of the object that is to be used to generate the
     * prediction.
     */
    public class PredictorFunction<T> {

        T function;
        double sumOfSquareError;
        double rootMeanSquareError;

        /**
         * This creates a new instance of a prediction function.
         *
         * @param function The function that the predictor is to use to estimate
         * power/energy consumption.
         * @param sumOfSquareError The sum of the square error for the
         * prediction function.
         * @param rootMeanSquareError The root mean square error for the
         * prediction function.
         */
        public PredictorFunction(T function, double sumOfSquareError, double rootMeanSquareError) {
            this.function = function;
            this.sumOfSquareError = sumOfSquareError;
            this.rootMeanSquareError = rootMeanSquareError;
        }

        /**
         * This returns the object that provides the prediction function
         *
         * @return The function that the predictor is to use to estimate
         * power/energy consumption.
         */
        public T getFunction() {
            return function;
        }

        /**
         * This returns the sum of the square error for the prediction function.
         *
         * @return The sum of the square error for the prediction function.
         */
        public double getSumOfSquareError() {
            return sumOfSquareError;
        }

        /**
         * This returns the sum of the room mean error for the prediction
         * function.
         *
         * @return The sum of the square error for the prediction function.
         */
        public double getRootMeanSquareError() {
            return rootMeanSquareError;
        }
    }

}
