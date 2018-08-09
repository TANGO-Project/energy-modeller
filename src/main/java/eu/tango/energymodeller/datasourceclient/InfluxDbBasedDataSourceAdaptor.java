/**
 * Copyright 2018 University of Leeds
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
package eu.tango.energymodeller.datasourceclient;

import eu.tango.energymodeller.types.energyuser.Host;

/**
 * This indicates the adaptor can write out values to Influx DB.
 * @author Richard Kavanagh
 */
public interface InfluxDbBasedDataSourceAdaptor {

    /**
     * This writes the log data out directly to influx db
     * @param host The host to write the data out for
     * @param power The power consumption information to write out
     */    
    public void writeOutHostValuesToInflux(Host host, double power);    
    
}
