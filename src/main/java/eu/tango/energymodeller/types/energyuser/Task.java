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
 */
package eu.tango.energymodeller.types.energyuser;

/**
 * This class represents an energy user of the Tango project and in particular
 * an application running on a physical host.
 * An application may be made up of many tasks, each of which consumes power. This
 * therefore represents part of this application.
 * 
 * @Deprecated is this needed, or should it be limited at application level 
 * 
 * @author Richard Kavanagh
 */
public class Task extends EnergyUsageSource implements Comparable<Task> {

    @Override
    public int compareTo(Task o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
