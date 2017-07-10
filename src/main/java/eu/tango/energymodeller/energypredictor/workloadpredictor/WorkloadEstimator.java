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
import eu.tango.energymodeller.datastore.DatabaseConnector;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.WorkloadSource;
import java.util.Collection;

/**
 * This produces workload estimates for providing better energy estimations.
 *
 * @author Richard Kavanagh
 * @param <T> Workload estimates takes a source of the workload and estimates 
 * utilisation information
 */
public interface WorkloadEstimator<T extends WorkloadSource> {

    /**
     * This estimates a physical hosts CPU utilisation. It is based upon which
     * VMs are expected to be deployed/are currently deployed.
     * @param host The physical host to get the workload estimation for.
     * @param workloadSource The virtual machines or applications that induce the workload.
     * @return The estimated CPU utilisation of the physical host.
     */
    public double getCpuUtilisation(Host host, Collection<T> workloadSource);
    
    /**
     * This sets the data source that is to be used for querying current data. 
     * @param datasource The data source to use for current information
     */
    public void setDataSource(HostDataSource datasource);

    /**
     * This sets the database that is to be used for querying historical data.
     * @param database The database to use
     */
    public void setDatabaseConnector(DatabaseConnector database);
    
    /**
     * This indicates if the predictor requires VM information or not in order
     * to make its prediction.
     * @return If the predictor requires VM information to make a prediction. 
     * True only if this is the case, otherwise the predictor will utilise 
     * host only information.
     */
    public boolean requiresVMInformation();

}
