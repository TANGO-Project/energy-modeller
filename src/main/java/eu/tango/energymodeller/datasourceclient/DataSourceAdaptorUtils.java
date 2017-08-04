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
package eu.tango.energymodeller.datasourceclient;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import java.util.ArrayList;
import java.util.List;

/**
 * This holds generic methods that are useful to all data sources.
 *
 * @author Richard Kavanagh
 */
public abstract class DataSourceAdaptorUtils {
    
    /**
     * This takes a list of applications and filters based upon the host that they are on
     * @param applicationList The application list to filter
     * @param host The host to filter upon
     * @return The list of applications on the named host
     */
    public static List<ApplicationOnHost> filterHostApplicationList(List<ApplicationOnHost> applicationList, Host host) {
        List<ApplicationOnHost> answer = new ArrayList<>();
        for (ApplicationOnHost application : applicationList) {
            if (application.getAllocatedTo().equals(host)) {
                answer.add(application);
            }
        }
        return answer;
    }       
    
}
