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
package eu.tango.energymodeller.types.energyuser.comparators;

import eu.tango.energymodeller.types.energyuser.Host;
import java.io.Serializable;
import java.util.Comparator;

/**
 * This compares to hosts by its name, this is the natural ordering of Hosts.
 * @author Richard Kavanagh
 */
public class HostName implements Comparator<Host>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Host host1, Host host2) {
        return host1.getHostName().compareTo(host2.getHostName());
    }

    
}
