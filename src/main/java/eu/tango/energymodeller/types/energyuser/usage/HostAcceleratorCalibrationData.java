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
package eu.tango.energymodeller.types.energyuser.usage;

import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * The aim of this class is to store information regarding the energy usage of a
 * host's accelerator at different levels of resource usage.
 * 
 * It is to be recorded as a n-tuple record indicating the accelerators id, plus
 * any additional data that can be used to construct a model.
 *
 * @author Richard Kavanagh
 */
public class HostAcceleratorCalibrationData {
    
    private String identifier;
    private HashMap<String, Double> parameters = new HashMap<>();
    private double power; //the output power given the workload
    
    /**
     * This creates a new record for storing the energy calibration data of a
     * host machine.
     */
    public HostAcceleratorCalibrationData() {
    }
    
    /**
     * This takes a list of host measurements and converts them into calibration
     * data.
     *
     * @param acceleratorId The accelerators main identifier.
     * @param data The host measurements to use to generate the calibration
     * data.
     * @param metrics The metrics that identify the accelerators usage
     * @return A list of calibration data points.
     */
    public static List<HostAcceleratorCalibrationData> getCalibrationData(String acceleratorId, List<HostMeasurement> data, List<String> metrics) {
        List<HostAcceleratorCalibrationData> answer = new ArrayList<>();
        for (HostMeasurement hostMeasurement : data) {
            double power = hostMeasurement.getPower();
            HashMap<String, Double> params = new HashMap<>();
            for (String metric : metrics) {
                params.put(metric, hostMeasurement.getMetric(metric).getValue());
            }            
            HostAcceleratorCalibrationData newItem = new HostAcceleratorCalibrationData(acceleratorId, params, power);
            answer.add(newItem);
        }
        return answer;
    }

    /**
     * This creates a new HostAcceleratorCalibrartionData entry
     * @param indentifier The identifier that indicates which accelerator this 
     * data is for
     */
    public HostAcceleratorCalibrationData(String indentifier) {
        this.identifier = indentifier;
    }

    /**
     * This creates a new record for storing the energy calibration data of a
     * host machine's accelerator.
     * @param identifier The accelerators identifier
     * @param parameters The parameters that cause a given level of power consumption
     * @param power The resultant power consumption given the named parameters
     */
    public HostAcceleratorCalibrationData(String identifier, HashMap<String, Double> parameters, double power) {
        this.identifier = identifier;
        this.parameters = parameters;
        this.power = power;
    }

    /**
     * @return the Identifier 
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the Identifier for the accelerator
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    

    /**
     * This lists the possible parameters that are stored.
     * @return the parameters
     */
    public Set<String> getParameters() {
        return parameters.keySet();
    }
    
     /**
     * This gets a parameter from the calibration data
     * @param parameter This parameter to get from the calibration data
     * @return the value of the parameter
     */
    public double getParameter(String parameter) {
        return parameters.get(parameter);
    }
    
     /**
     * This lists the possible parameters that are stored.
     * @param parameter The parameter to add
     * @param value The value for the parameter
     */
    public void addParameter(String parameter, double value) {
        parameters.put(parameter, value);
    }      

    /**
     * @return the power
     */
    public double getPower() {
        return power;
    }

    /**
     * @param power the power to set
     */
    public void setPower(double power) {
        this.power = power;
    }
    
}
