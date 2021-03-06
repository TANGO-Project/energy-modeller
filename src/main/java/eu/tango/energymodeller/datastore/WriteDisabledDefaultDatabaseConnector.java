/**
 * Copyright 2014 University of Leeds
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
package eu.tango.energymodeller.datastore;

import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import java.util.Collection;

/**
 * This provides a default database connector that can only work in a read only mode.
 * @author Richard Kavanagh
 */
public class WriteDisabledDefaultDatabaseConnector extends DefaultDatabaseConnector {

    @Override
    public void setHosts(Collection<Host> hosts) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */
    }    
    
    @Override
    public void setVms(Collection<VmDeployed> vms) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */
    }
    
    @Override
    public void setHostCalibrationData(Host host) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */    
    }
    
    @Override
    public void setHostProfileData(Host host) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */    
    }    
    
    @Override
    public void writeHostHistoricData(Host host, long time, double power, double energy) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */          
    }
    
    @Override
    public void writeHostVMHistoricData(Host host, long time, HostEnergyUserLoadFraction load) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */              
    } 

    @Override
    public void setVMProfileData(VmDeployed vm) {
        /**
         * Do nothing, the aim is to be able to run another data gatherer along
         * side the main one, thus only one should be allowed to write to the
         * database.
         */ 
    }
    
}
