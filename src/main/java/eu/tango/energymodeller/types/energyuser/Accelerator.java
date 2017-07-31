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
package eu.tango.energymodeller.types.energyuser;

import eu.tango.energymodeller.types.energyuser.usage.HostAcceleratorCalibrationData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This represents an accelerator on a physical host
 *
 * @author Richard Kavanagh
 */
public class Accelerator {

    private String name = "";
    private AcceleratorType type;
    private int count = 0;
    private ArrayList<HostAcceleratorCalibrationData> acceleratorCalibrationData = new ArrayList<>();
    
    
    public enum AcceleratorType {

        GPU, MIC, FPGA
    }

    /**
     * This creates a new accelerator
     * @param name The name of the accelerator
     * @param count The count of accelerators on the physical host
     * @param type The type of accelerator on the host
     */
    public Accelerator(String name, int count, AcceleratorType type) {
        this.type = type;
        this.name = name;
        this.count = count;
    }
    
    /**
     * This gets the name of the accelerator
     * @return The name of the accelerator
     */
    public String getName() {
        return name;
    }

    /**
     * This sets the name of the accelerator
     * @param name The name of the accelerator
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This indicates the type of accelerator
     * @return the accelerator
     */
    public AcceleratorType getType() {
        return type;
    }

    /**
     * This sets the type of accelerator
     * @param type the accelerator to set
     */
    public void setType(AcceleratorType type) {
        this.type = type;
    }
    
    /**
     * This lists the metric names that are stored as part of the accelerators
     * calibration data. i.e. the ones that define the workload and power consumption
     * @return the parameters that define the workload
     */
    public Set<String> getMetricsInCalibrationData() {
        Set<String> answer = new HashSet<>();
        for (HostAcceleratorCalibrationData dataItem : acceleratorCalibrationData) {
            answer.addAll(dataItem.getParameters());
        }
        return answer;
    }

    /**
     * This gets the count of accelerators on the physical host
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * This sets count of accelerators on the physical host
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * Indicates if this accelerator has calibration data for it.
     * @return 
     */
    public boolean isCalibrated() {
        return !acceleratorCalibrationData.isEmpty();
    }
    
    /**
     * This returns a list of all the calibration data that is held on the host
     * for its accelerators.
     *
     * @return the calibration data of the host.
     */
    public ArrayList<HostAcceleratorCalibrationData> getAcceleratorCalibrationData() {
        return acceleratorCalibrationData;
    }
    
    /**
     * This allows the calibration data of a host to be set.
     *
     * @param calibrationData the calibrationData to set
     */
    public void setAcceleratorCalibrationData(ArrayList<HostAcceleratorCalibrationData> calibrationData) {
        this.acceleratorCalibrationData = calibrationData;
    }       

}
