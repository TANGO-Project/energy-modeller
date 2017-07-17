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
package eu.tango.energymodeller.datastore;

import eu.ascetic.ioutils.io.ResultsStore;
import java.util.HashMap;
import java.io.File;
import java.util.ArrayList;

/**
 * This normalises a csv files input so that it can be used for calibrating a
 * learning algorithm.
 *
 * @author Richard Kavanagh
 */
public class AcceleratorCalibrationDataLoader extends ResultsStore {

    private HashMap<Integer, Double> minValues = new HashMap<>();
    private HashMap<Integer, Double> maxValues = new HashMap<>();
    private HashMap<Integer, Double> rangeValues = new HashMap<>();
    private boolean hasNormalised = false;

    public AcceleratorCalibrationDataLoader(File file) {
        super(file);
    }

    public AcceleratorCalibrationDataLoader(String filename) {
        super(filename);
    }

    public double getMin(int column) {
        if (size() == 0 || getRowSize(0) < column) {
            return Double.NaN;
        }
        //Ignore header rows, use 1 instead of 0
        Double min = Double.parseDouble(getElement(1, column));
        for (int i = 1; i < size(); i++) {
            try {
                double current = Double.parseDouble(getElement(i, column));
                if (current < min) {
                    min = current;
                }
            } catch (NumberFormatException ex) {

            }
        }
        return min;
    }

    public double getMax(int column) {
        if (size() == 0 || getRowSize(0) < column) {
            return Double.NaN;
        }
        //Ignore header rows, use 1 instead of 0
        Double max = Double.parseDouble(getElement(1, column));
        for (int i = 1; i < size(); i++) {
            try {
                double current = Double.parseDouble(getElement(i, column));
                if (current > max) {
                    max = current;
                }
            } catch (NumberFormatException ex) {

            }
        }
        return max;
    }

    public double getRange(int column) {
        double min = getMin(column);
        double max = getMax(column);
        return max - min;
    }

    public void normalise() {
        for (int column = 0; column < getRowSize(1); column++) {
            double min = getMin(column);
            double max = getMax(column);
            double range = getRange(column);
            minValues.put(column, min);
            maxValues.put(column, max);
            rangeValues.put(column, range);
            for (int row = 1; row < size(); row++) {
                double current = Double.parseDouble(getElement(row, column));
                //Normalising is: (((value-min/range)*2)-1 (to the range -1, 1
                setElement(row, column, ((((current - min) / range) * 2) - 1));
            }
        }
        hasNormalised = true;
    }
    
    /**
     * This gets the header for the calibration data
     * @return The header for the calibration data
     */
    public ArrayList<String> getHeader() {
        return this.getRow(0);
    }

    public double[][] generateInput(int inputSize) {
        double[][] answer = new double[this.size() - 2][inputSize];
        for (int row = 1; row < size() - 1; row++) {
            for (int item = 0; item < inputSize; item++) {
                answer[row - 1][item] = Double.parseDouble(getElement(row, item));
            }
        }
        return answer;
    }

    public double[][] generateIdeal(int inputSize) {
        double[][] answer = new double[this.size() - 2][inputSize];
        for (int row = 1; row < size() - 1; row++) {
            for (int item = getRowSize(row) - inputSize; item < getRowSize(row); item++) {
                answer[row - 1][item - (getRowSize(row) - 1)] = Double.parseDouble(getElement(row, item));
            }
        }
        return answer;
    }
    
    public double[] castIntoNet(double[] values) {
        double[] answer = new double[values.length];
        int index = 0;
        for (double item : answer) {
            answer[index] = castIntoNet(index, item);
        }
        return answer;
    }    
    
    public double castIntoNet(int column, double value) {
        double min;
        double range;
        if (hasNormalised) {
            min = minValues.get(column);
            range = rangeValues.get(column);
        } else {
            min = getMin(column);
            double max = getMax(column);
            range = max - min;
        }
        //Normalising is: (((value-min/range)*2)-1 (to the range -1, 1
        return (((value - min) / range) * 2) - 1;        
    }
    
    public double[] castFromNet(double[] values) {
        double[] answer = new double[values.length];
        int index = 0;
        for (double item : answer) {
            answer[index] = castFromNet(index, item);
        }
        return answer;
    }
    
    public double castFromNet(int column, double value) {
        double min;
        double range;
        if (hasNormalised) {
            min = minValues.get(column);
            range = rangeValues.get(column);
        } else {
            min = getMin(column);
            double max = getMax(column);
            range = max - min;
        }
        //denormalising is: (((normalised + 1) / 2) * range) + min = denormalied
        return (((value + 1) / 2) * range) + 1;         
    }

}
