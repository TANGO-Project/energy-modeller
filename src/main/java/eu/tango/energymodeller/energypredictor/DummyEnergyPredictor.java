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
import eu.tango.energymodeller.types.energyuser.CandidateVMHostMapping;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VM;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import eu.tango.energymodeller.types.usage.EnergyUsagePrediction;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class provides dummy data for the energy modeller component. It
 * therefore cannot be used in normal operating of the system but is useful for
 * initial testing purposes. It provides random power and energy values between
 * 0 and 20.
 *
 * These values will remain constant for the life of the energy predictor.
 *
 * @author Richard Kavanagh
 */
public class DummyEnergyPredictor extends AbstractEnergyPredictor {

    HashMap<Host, Double> tempAvgPowerUsed = new HashMap<>();
    HashMap<Host, Double> tempTotalEnergyUsed = new HashMap<>();

    @Override
    public EnergyUsagePrediction getHostPredictedEnergy(Host host, Collection<WorkloadSource> virtualMachines, TimePeriod duration) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction(host);
        answer.setDuration(duration);
        if (tempAvgPowerUsed.containsKey(host)) {
            answer.setAvgPowerUsed(tempAvgPowerUsed.get(host));
            answer.setTotalEnergyUsed(tempTotalEnergyUsed.get(host));
        } else {
            double tempPower = Math.random() * 20;
            double tempEnergy = Math.random() * 20;
            tempAvgPowerUsed.put(host, tempPower);
            tempTotalEnergyUsed.put(host, tempEnergy);
            answer.setAvgPowerUsed(tempPower);
            answer.setTotalEnergyUsed(tempEnergy);
        }
        return answer;
    }

    HashMap<CandidateVMHostMapping, Double> temp2AvgPowerUsed = new HashMap<>();
    HashMap<CandidateVMHostMapping, Double> temp2TotalEnergyUsed = new HashMap<>();

    @Override
    public EnergyUsagePrediction getVMPredictedEnergy(VM vm, Collection<VM> virtualMachines, Host host, TimePeriod timePeriod) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction(vm);
        answer.setDuration(timePeriod);
        if (tempAvgPowerUsed.containsKey(host)) {
            answer.setAvgPowerUsed(temp2AvgPowerUsed.get(new CandidateVMHostMapping(vm, host)));
            answer.setTotalEnergyUsed(temp2TotalEnergyUsed.get(new CandidateVMHostMapping(vm, host)));
        } else {
            double tempPower = Math.random() * 20;
            double tempEnergy = Math.random() * 20;
            temp2AvgPowerUsed.put(new CandidateVMHostMapping(vm, host), tempPower);
            temp2TotalEnergyUsed.put(new CandidateVMHostMapping(vm, host), tempEnergy);
            answer.setAvgPowerUsed(tempPower);
            answer.setTotalEnergyUsed(tempEnergy);
        }
        return answer;
    }
    
    HashMap<ApplicationOnHost, Double> temp3AvgPowerUsed = new HashMap<>();
    HashMap<ApplicationOnHost, Double> temp3TotalEnergyUsed = new HashMap<>();    

    @Override
    public EnergyUsagePrediction getApplicationPredictedEnergy(ApplicationOnHost app, Collection<ApplicationOnHost> applications, Host host, TimePeriod timePeriod) {
        EnergyUsagePrediction answer = new EnergyUsagePrediction(app);
        answer.setDuration(timePeriod);
        if (tempAvgPowerUsed.containsKey(host)) {
            answer.setAvgPowerUsed(temp3AvgPowerUsed.get(app));
            answer.setTotalEnergyUsed(temp3TotalEnergyUsed.get(app));
        } else {
            double tempPower = Math.random() * 20;
            double tempEnergy = Math.random() * 20;
            temp3AvgPowerUsed.put(app, tempPower);
            temp3TotalEnergyUsed.put(app, tempEnergy);
            answer.setAvgPowerUsed(tempPower);
            answer.setTotalEnergyUsed(tempEnergy);
        }
        return answer;
    }
      
    @Override
    public double predictPowerUsed(Host host) {
        return tempAvgPowerUsed.get(host);
    }

    @Override
    public double predictPowerUsed(Host host, double usageCPU) {
        return tempAvgPowerUsed.get(host);
    }    

    @Override
    public double getSumOfSquareError(Host host) {
        return Double.NaN;
    }

    @Override
    public double getRootMeanSquareError(Host host) {
        return Double.NaN;
    }
    
    @Override
    public String toString() {
        return "Dummy energy predictor";
    }

    @Override
    public void printFitInformation(Host host) {
        System.out.println(this.toString() + " - SSE: NaN RMSE: NaN");
    }          

}
