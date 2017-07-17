/**
 * Copyright 2015 University of Leeds
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

import eu.tango.energymodeller.datasourceclient.HostDataSource;
import eu.tango.energymodeller.datasourceclient.HostMeasurement;
import eu.tango.energymodeller.datasourceclient.MetricValue;
import eu.tango.energymodeller.datastore.DatabaseConnector;
import eu.tango.energymodeller.types.energyuser.Accelerator;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This produces workload estimates for the purpose of providing better 
 * energy estimations.
 *
 * @author Richard Kavanagh
 * @param <AT> This specifies the type of workload source that the predictor is for
 */
public abstract class AbstractWorkloadEstimator<AT extends WorkloadSource> implements WorkloadEstimator<AT> {

    protected DatabaseConnector database = null;
    protected HostDataSource datasource = null;

    @Override
    public void setDataSource(HostDataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void setDatabaseConnector(DatabaseConnector database) {
        this.database = database;
    }
    
    /**
     * This provides an average of the recent accelerator utilisation for a given host,
     * based upon the CPU utilisation time window set for the energy predictor.
     *
     * @param host The host for which the average accelerator utilisation over the 
     * last n seconds will be calculated for.
     * @param workloadsource The set of virtual machines or applications that
     * are on the physical host in question.
     * @return The average recent accelerator utilisation based upon the energy
     * predictor's configured observation window.
     */
    @Override
    public HashMap<Accelerator,HashMap<String, Double>> getAcceleratorUtilisation(Host host, Collection<WorkloadSource> workloadsource) {
        HashMap<Accelerator,HashMap<String, Double>> answer = new HashMap<>();
        if (!host.hasAccelerator()) {
            return answer;
        }
        HashSet<Accelerator> accelerators = host.getAccelerators();
        for (Accelerator accelerator : accelerators) {
            Set<String> params = accelerator.getMetricsInCalibrationData();
            HostMeasurement measurement = datasource.getHostData(host);
            answer.put(accelerator, filterHostsAcceleratorMeasurements(measurement, params));
        }
        return answer;
    }
    
    /**
     * This filters a host measurement record for information that is contained within
     * the accelerators calibration data
     * @param measurement The host measurement to get the data for
     * @param acceleratorMetrics The names of the metrics for the accelerator
     * @return The list of metrics associated with the utilisation of the accelerator
     */
    private HashMap<String, Double> filterHostsAcceleratorMeasurements(HostMeasurement measurement, Set<String> acceleratorMetrics) {
        HashMap<String, Double> metrics = new HashMap<>();
        for (String param : acceleratorMetrics) {
            MetricValue metric = measurement.getMetric(param);
            if (metric != null) {
                metrics.put(metric.getKey(),metric.getValue());
            }
        }
        return metrics;
    }    

}
