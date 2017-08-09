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
package eu.tango.energymodeller.types.energymodel;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import eu.tango.energymodeller.datastore.AcceleratorCalibrationDataLoader;
import java.util.HashMap;

/**
 * The aim of this function is to apply a multilayer perceptron to calibration
 * data, the trend is expected to be learned which then allows this function to 
 * be applied to estimating power consumption.
 * @author Richard Kavanagh
 */
public class NeuralNetFunction {

    /**
     * The input necessary for training.
     */
    private double[][] input;

    /**
     * The ideal data (i.e. planned output).
     */
    private double[][] ideal_output;

    BasicNetwork network = new BasicNetwork();
    AcceleratorCalibrationDataLoader loader;
    
    private boolean trained = false;
    private int inputSize;
    private static final int OUTPUT_SIZE = 1;

    /**
     * Loads calibration data for a given accelerator
     *
     * @param toLoad The file to load the information for
     */
    public NeuralNetFunction(String toLoad) {
        loader = new AcceleratorCalibrationDataLoader(toLoad);
        loader.load();
        loader.normalise();
        inputSize = loader.size() - OUTPUT_SIZE;
        input = loader.generateInput(inputSize);
        ideal_output = loader.generateIdeal(OUTPUT_SIZE);
        // create a neural network, without using a factory
        network.addLayer(new BasicLayer(null, true, inputSize));
        /**
         * Choosing sum square_root(input + output) as the input size as the size of the hidden layer.
         */
        double hiddenLayerSize = Math.sqrt((double)inputSize + OUTPUT_SIZE);
        network.addLayer(new BasicLayer(new ActivationTANH(), true, (int) hiddenLayerSize)); //ActivationSigmoid()
        network.addLayer(new BasicLayer(new ActivationTANH(), false, 1));
        network.getStructure().finalizeStructure();
        network.reset();

        // create training data
        MLDataSet trainingSet = new BasicMLDataSet(input, ideal_output);

        // train the neural network
        final ResilientPropagation train = new ResilientPropagation(network, trainingSet);

        int epoch = 1;

        do {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());
            epoch++;
            if (epoch > 2000) { //ensure there is no endless loop
                trained = false;
                break;
            }
        } while (train.getError() > 0.01);
        trained = true;
        train.finishTraining();
    }

    /**
     * This obtains from the neural network the output for the power consumption
     *
     * @param utilisationData The utilisation information for the accelerator
     * that this is to map against.
     * @return The output power consumption given the inputs provided.
     */
    public double value(HashMap<String, Double> utilisationData) {
        double[] utilData = new double[inputSize];
        for(int i = 0; i < inputSize;i++) {
            utilData[i] = utilisationData.get(loader.getHeader().get(i));
        }
        return value(utilData);
    }
    
    /**
     * This obtains from the neural network the output for the power consumption
     *
     * @param utilisationData The utilisation information for the accelerator
     * that this is to map against.
     * @return The output power consumption given the inputs provided.
     */
    public double value(double[] utilisationData) {
        utilisationData = loader.castIntoNet(utilisationData);
        //Example input.
        //0	0	28	324	324	324	405	30
        //100	9	37	732	732	2600	540	81    
        MLData data = new BasicMLData(utilisationData);
        double output = network.classify(data);
        return loader.castFromNet(inputSize, output);
    }

    public void shutdown() {
        Encog.getInstance().shutdown();
    }

    /**
     * Indicates if this function managed to train successfully or not.
     * @return If the training that took place during this objects construction
     * worked or not.
     */
    public boolean isTrained() {
        return trained;
    }

}
