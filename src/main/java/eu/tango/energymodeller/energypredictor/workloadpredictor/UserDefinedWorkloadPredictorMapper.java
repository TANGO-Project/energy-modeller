/**
 * Copyright 2016 University of Leeds
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
package eu.tango.energymodeller.energypredictor.workloadpredictor;

import eu.ascetic.ioutils.io.ResultsStore;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.VmDiskImage;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.usage.VmLoadHistoryRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is designed to allow on the detection of A VMs property such as
 * disk image association or application association and to select from file the
 * appropriate workload predictor.
 *
 * @author Richard Kavanagh
 */
public class UserDefinedWorkloadPredictorMapper extends AbstractVMHistoryWorkloadEstimator {

    private static final File CONFIG_FILE = new File("WorkloadPredictionMapping.csv");
    private final ArrayList<PredictorUsageRule> predictorRules = new ArrayList<>();
    private final HashSet<String> validAppTags = new HashSet<>();
    private final HashSet<String> validDiskRefs = new HashSet<>();
    private final WorkloadEstimator<WorkloadSource> defaultEstimator = new CpuRecentHistoryWorkloadPredictor();
    private final ArrayList<AbstractVMHistoryWorkloadEstimator> estimatorList = new ArrayList<>();

    public UserDefinedWorkloadPredictorMapper() {
        estimatorList.add(new BasicAverageCpuWorkloadPredictor());
        estimatorList.add(new BasicAverageCpuWorkloadPredictorDisk());
        estimatorList.add(new BootAverageCpuWorkloadPredictor());
        estimatorList.add(new BootAverageCpuWorkloadPredictorDisk());
        estimatorList.add(new DoWAverageCpuWorkloadPredictor());
        estimatorList.add(new DoWAverageCpuWorkloadPredictorDisk());
        
        if (CONFIG_FILE.exists()) {
            populatePredictorRules();
        } else {
            writeDefaultsToFile();
        }
    }

    /**
     * This reads from file the mappings between app tags/disk reference and
     * the predictor to use for the app tag/disk reference.
     */
    private void populatePredictorRules() {
        ResultsStore configFile = new ResultsStore(CONFIG_FILE);
        for (int row = 0; row < configFile.size(); row++) {
            ArrayList<String> current = configFile.getRow(row);
            if (current.size() != 4) {
                continue;
            }
            if (current.get(0).equals("PropertyValue")) {
                continue;
            }
            PredictorUsageRule rule = new PredictorUsageRule(current.get(0), Boolean.getBoolean(current.get(1)), Boolean.getBoolean(current.get(2)), current.get(3));
            predictorRules.add(rule);
            if (rule.isAppTag) {
                validAppTags.add(rule.propertyToMatch);
            }
            if (rule.isDisk) {
                validDiskRefs.add(rule.propertyToMatch);
            }            
        }
    }

    /**
     * This writes the default configuration file to disk.
     */
    private void writeDefaultsToFile() {
        ResultsStore store = new ResultsStore(CONFIG_FILE);
        store.add("PropertyValue");
        store.append("IsRefToBaseImage");
        store.append("IsRefToVMAppUsed");
        store.add("FilterToUse");
        store.saveMemoryConservative();
    }

    @Override
    public double getCpuUtilisation(Host host, Collection<VM> virtualMachines) {
        double vmCount = 0;
        double sumCpuUtilisation = 0;
        if (hasAppTags(virtualMachines, validAppTags)) {
            for (VM vm : virtualMachines) {
                sumCpuUtilisation = sumCpuUtilisation + getAverageCpuUtilisation(vm).getUtilisation();
                vmCount = vmCount + 1;
            }
            if (vmCount == 0) {
                return 0.0;
            }            
            return sumCpuUtilisation / vmCount;
        } else if (hasDiskReferences(virtualMachines, validDiskRefs)) {
            for (VM vm : virtualMachines) {
                sumCpuUtilisation = sumCpuUtilisation + getAverageCpuUtilisastionDisk(vm);
                vmCount = vmCount + 1;
            }
            if (vmCount == 0) {
                return 0.0;
            }            
            return sumCpuUtilisation / vmCount;
        }
        return defaultEstimator.getCpuUtilisation(host, new ArrayList<WorkloadSource>(virtualMachines));
    }

    /**
     * This gets the average CPU utilisation for a VM given the app tags that it
     * has.
     *
     * @param vm The VM to get the average utilisation for.
     * @return The average utilisation of all application tags that a VM has.
     */
    @Override
    public VmLoadHistoryRecord getAverageCpuUtilisation(VM vm) {
        double utilisation = 0.0;
        double stdDev = 0.0;
        if (vm.getApplicationTags().isEmpty()) {
            return new VmLoadHistoryRecord(utilisation, stdDev);
        }
        for (String tag : vm.getApplicationTags()) {
            VmLoadHistoryRecord answer = getEstimator(tag).getAverageCpuUtilisation(vm);
            utilisation = utilisation + answer.getUtilisation();
            stdDev = (stdDev < answer.getStdDev() ? answer.getStdDev() : stdDev);
        }
        return new VmLoadHistoryRecord(utilisation, stdDev);
    }
    
    /**
     * This gets the average CPU utilisation for a VM given the disk reference that it
     * has.
     *
     * @param vm The VM to get the average utilisation for.
     * @return The average utilisation of all disk references that a VM has.
     */    
    public double getAverageCpuUtilisastionDisk(VM vm) {
        double answer = 0.0;
        if (vm.getDiskImages().isEmpty()) {
            return 0;
        }
        for (VmDiskImage image : vm.getDiskImages()) {
            answer = answer + getEstimator(image.getDiskImage()).getAverageCpuUtilisation(vm).getUtilisation();
        }
        return answer / vm.getApplicationTags().size();
    } 
    
    /**
     * This method finds from the lookup property the correct Class to load and
     * perform the query.
     * @param lookupProperty The app tag or disk reference to lookup.
     * @return The workload estimator to use for VM
     */
    private AbstractVMHistoryWorkloadEstimator getEstimator(String lookupProperty) {
        for (PredictorUsageRule rule : predictorRules) {
            if (rule.getPropertyToMatch().equals(lookupProperty)) {
                for (AbstractVMHistoryWorkloadEstimator estimator : estimatorList) {
                    if (estimator.getName().equals(rule.getPredictor())) {
                        return estimator;
                    }
                }
            }
        }
        //No rule was detected, using a basic default
        Logger.getLogger(UserDefinedWorkloadPredictorMapper.class.getName()).log(Level.WARNING, 
                            "Reverting to a basic average of cpu utilisation for workload prediction");           
        return new BasicAverageCpuWorkloadPredictor(); 
    }

    @Override
    public boolean requiresVMInformation() {
        return true;
    }

    /**
     * This class represents the mappings between 
     * VM properties (app tags and VM disk references) and the predictor to use.
     */
    private class PredictorUsageRule {

        private String propertyToMatch;
        private boolean isAppTag;
        private boolean isDisk;
        private String predictor;

        /**
         * This creates a new predictor usage rule
         * @param propertyToMatch The String representation of the app tag or 
         * disk reference
         * @param isAppTag If the property is an app tag or not
         * @param isDisk If the property is an disk reference or not
         * @param predictor The name of the predictor to use
         */
        public PredictorUsageRule(String propertyToMatch, boolean isAppTag, boolean isDisk, String predictor) {
            this.propertyToMatch = propertyToMatch;
            this.isAppTag = isAppTag;
            this.isDisk = isDisk;
            this.predictor = predictor;
        }

        /**
         * This gets the name of the VM property to match
         * @return The name of the VM property to match
         */
        public String getPropertyToMatch() {
            return propertyToMatch;
        }

        /**
         * This sets the name of the VM property to match
         * @param propertyToMatch The name of the VM property to match
         */
        public void setPropertyToMatch(String propertyToMatch) {
            this.propertyToMatch = propertyToMatch;
        }

        /**
         * This gets if the property is for a app tag or not
         * @return If the property is for a app tag or not
         */
        public boolean isIsAppTag() {
            return isAppTag;
        }

        /**
         * This sets if the property is for a app tag or not
         * @param isAppTag If the property is for a app tag or not
         */
        public void setIsAppTag(boolean isAppTag) {
            this.isAppTag = isAppTag;
        }

        /**
         * This gets if the property is for a disk reference or not
         * @return If the property is for a disk reference or not
         */
        public boolean isIsDisk() {
            return isDisk;
        }

        /**
         * This sets if the property is for a disk reference or not
         * @param isDisk If the property is for a disk reference or not
         */
        public void setIsDisk(boolean isDisk) {
            this.isDisk = isDisk;
        }

        /**
         * The gets the name of the predictor to use
         * @return the predictor The name of the predictor to use
         */
        public String getPredictor() {
            return predictor;
        }

        /**
         * This sets the name of the predictor to use
         * @param predictor The name of the predictor to set
         */
        public void setPredictor(String predictor) {
            this.predictor = predictor;
        }
    }
    
    @Override
    public String getName() {
        return "User Defined VM Property Workload Predictor";
    } 
    

}
