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
 */
package eu.tango.energymodeller.datastore;

import eu.ascetic.ioutils.io.GenericLogger;
import eu.ascetic.ioutils.io.ResultsStore;
import eu.tango.energymodeller.datasourceclient.CollectDInfluxDbDataSourceAdaptor;
import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyDivision;
import eu.tango.energymodeller.energypredictor.vmenergyshare.EnergyShareRule;
import eu.tango.energymodeller.energypredictor.vmenergyshare.LoadFractionShareRule;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This provides logging facilities for Application energy usage data.
 *
 * @author Richard Kavanagh
 */
public class ApplicationEnergyUsageLogger extends GenericLogger<ApplicationEnergyUsageLogger.Pair> {

    private EnergyShareRule rule = new LoadFractionShareRule();
    private boolean considerIdleEnergy = true;
    private final CollectDInfluxDbDataSourceAdaptor writeAdaptor = new CollectDInfluxDbDataSourceAdaptor();

    /**
     * This creates a new application energy user logger
     *
     * @param file The file to write the log out to.
     * @param overwrite If the file should be overwritten on starting the energy
     * modeller.
     */
    public ApplicationEnergyUsageLogger(File file, boolean overwrite) {
        super(file, overwrite);
        saveFile.setDelimeter(" ");
    }

    @Override
    public void writeHeader(ResultsStore store) {
        store.setDelimeter(" ");
        /**
         * No header should be provided. The <Zabbix_Host> <Metric_Key> <Value>
         */
    }

    /**
     * This writes a host energy record and application load fraction data to disk.
     *
     * @param hostMeasurement The host energy record relating to the fraction
     * data.
     * @param applicationLoadFraction The application load fraction data.
     * @param store The storage that this data will be written to disk for.
     */
    public void writebody(HostMeasurement hostMeasurement, HostEnergyUserLoadFraction applicationLoadFraction, ResultsStore store) {
        store.setDelimeter(" ");
        ArrayList<EnergyUsageSource> appsArr = new ArrayList<>();
        appsArr.addAll(applicationLoadFraction.getEnergyUsageSources());
        if (rule.getClass().equals(LoadFractionShareRule.class)) {
            ArrayList<HostEnergyUserLoadFraction> loadFractionData = new ArrayList<>();
            loadFractionData.add(applicationLoadFraction);
            ((LoadFractionShareRule) rule).setFractions(loadFractionData.get(0).getFraction());
            Logger.getLogger(ApplicationEnergyUsageLogger.class.getName()).log(Level.FINE, "Using Load Fraction Share Rule");
        } else {
            Logger.getLogger(ApplicationEnergyUsageLogger.class.getName()).log(Level.FINE, "Using Share Rule Class: {0}", rule.getClass());
        }
        EnergyDivision division = rule.getEnergyUsage(hostMeasurement.getHost(), appsArr);
        division.setConsiderIdleEnergy(considerIdleEnergy);

        for (ApplicationOnHost app : applicationLoadFraction.getEnergyUsageSourcesAsApps()) {
            if (app.getAllocatedTo() == null) {
                app.setAllocatedTo(hostMeasurement.getHost());
            }
            double powerVal = division.getEnergyUsage(formatDouble(hostMeasurement.getPower(true), 1), app);
            powerVal = powerVal + applicationLoadFraction.getHostPowerOffset();
            if (!Double.isNaN(powerVal) && powerVal > 0) {
                store.add("APP:" + app.getName() + ":" + app.getId() + ":" + app.getAllocatedTo().getHostName());
                store.append("power");
                store.append(powerVal);
                writeAdaptor.writeOutApplicationValuesToInflux(app, powerVal);
                
            }
        }
    }

    @Override
    public void writebody(Pair item, ResultsStore store) {
        writebody(item.getHost(), item.getAppLoadFraction(), store);
    }

    /**
     * Indicates if the logged value should include idle energy or not.
     *
     * @return if idle energy is taken account of or not
     */
    public boolean isConsiderIdleEnergy() {
        return considerIdleEnergy;
    }

    /**
     * Sets if the logged value should include idle energy or not.
     *
     * @param considerIdleEnergy if idle energy is taken account of or not
     */
    public void setConsiderIdleEnergy(boolean considerIdleEnergy) {
        this.considerIdleEnergy = considerIdleEnergy;
    }

    /**
     * This formats a double to a set amount of decimal places.
     *
     * @param number The number to format
     * @param decimalPlaces The amount of decimal places to format to
     * @return The number formatted to a given amount of decimal places.
     */
    public static double formatDouble(double number, int decimalPlaces) {
        return BigDecimal.valueOf(number).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * This allows the energy share rule to be set.
     *
     * @param rule the rule to set
     */
    public void setRule(EnergyShareRule rule) {
        this.rule = rule;
    }

    /**
     * This binds a host energy record to application load fraction information. 
     * Thus allowing for the calculations to take place.
     */
    public class Pair {

        private final HostMeasurement host;
        private final HostEnergyUserLoadFraction appLoadFraction;

        /**
         * This creates a new pair object that links host energy records to 
         * application load fraction data.
         *
         * @param host The host energy record
         * @param appLoadFraction The application load fraction data.
         */
        public Pair(HostMeasurement host, HostEnergyUserLoadFraction appLoadFraction) {
            this.host = host;
            this.appLoadFraction = appLoadFraction;
        }

        /**
         * The host of the records that have been paired together.
         *
         * @return the host The host
         */
        public HostMeasurement getHost() {
            return host;
        }

        /**
         * The VM load fraction data of the records that have been paired
         * together.
         *
         * @return the host energy user load fraction data for the application
         */
        public HostEnergyUserLoadFraction getAppLoadFraction() {
            return appLoadFraction;
        }
    }

}
