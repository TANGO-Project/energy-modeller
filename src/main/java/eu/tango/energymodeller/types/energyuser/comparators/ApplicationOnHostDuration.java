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
package eu.tango.energymodeller.types.energyuser.comparators;

import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import java.io.Serializable;
import java.util.Comparator;

/**
 * This ranks applications on host by their duration. Therefore ranking applications
 * by how long they have been running.
 * @author Richard Kavanagh
 */
public class ApplicationOnHostDuration implements Comparator<ApplicationOnHost>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(ApplicationOnHost o1, ApplicationOnHost o2) {
        return Long.valueOf(o1.getDuration()).compareTo(o2.getDuration());
    }
    
}
