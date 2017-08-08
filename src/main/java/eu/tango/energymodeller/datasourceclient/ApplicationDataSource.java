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
 *
 * This is being developed for the TANGO Project: http://tango-project.eu
 *
 * 
 */
package eu.tango.energymodeller.datasourceclient;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import java.util.List;

/**
 *
 * @author Richard Kavanagh
 */
public interface ApplicationDataSource extends HostDataSource {


    /**
     * This provides for the named application all the information that is
     * available.
     *
     * @param application The host to get the measurement data for.
     * @return The host measurement data
     */
    public ApplicationMeasurement getApplicationData(ApplicationOnHost application);

    /**
     * This lists for all applications all the metric data on them.
     *
     * @return A list of application measurements
     */
    public List<ApplicationMeasurement> getApplicationData();

    /**
     * This takes a list of applications and provides all the metric data on
     * them.
     *
     * @param appList The list of applications to get the data from
     * @return A list of application measurements
     */
    public List<ApplicationMeasurement> getApplicationData(List<ApplicationOnHost> appList);   
    
}
