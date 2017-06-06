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
import eu.tango.energymodeller.types.energyuser.WorkloadSource;

/**
 * This produces workload estimates for the purpose of providing better 
 * energy estimations.
 *
 * @author Richard Kavanagh
 */
public abstract class AbstractWorkloadEstimator<AT extends WorkloadSource> implements WorkloadEstimator<AT> {

    protected DatabaseConnector database = null;
    protected HostDataSource datasource = null;

//    @Override
//    public abstract double getCpuUtilisation(Host host, Collection<T> virtualMachines);

    @Override
    public void setDataSource(HostDataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void setDatabaseConnector(DatabaseConnector database) {
        this.database = database;
    }

}
