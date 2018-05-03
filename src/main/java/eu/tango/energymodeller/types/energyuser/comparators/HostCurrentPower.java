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

import eu.tango.energymodeller.EnergyModeller;
import eu.tango.energymodeller.types.energyuser.Host;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This compares to hosts by their current power consumption.
 * @author Richard Kavanagh
 */
public class HostCurrentPower implements Comparator<Host>, Serializable {

    private static final long serialVersionUID = 1L;
    private final EnergyModeller modeller = EnergyModeller.getInstance();
    /**
     * This ensures a host when queried doesn't re-query the current measured
     * power consumption. It also ensures the ordering is static (given power 
     * consumption values could change during the lifetime of the sorting procedure.
     */
    private final HashMap<Host,Double> powerTable = new HashMap<>();

    public HostCurrentPower() {
    }
    
    /**
     * This pre-establishes a list of power values for a given list of hosts 
     * @param hosts The hosts to establish the current power consumption for
     */
    public HostCurrentPower(List<Host> hosts) {
        for (Host host : hosts) {
            powerTable.put(host, modeller.getCurrentEnergyForHost(host).getPower());
        }
    }
    
    /**
     * This pre-establishes a list of power values for a given collection of hosts 
     * @param hosts The hosts to establish the current power consumption for
     */
    public HostCurrentPower(Collection<Host> hosts) {
        for (Host host : hosts) {
            powerTable.put(host, modeller.getCurrentEnergyForHost(host).getPower());
        }
    }    
    
    @Override
    public int compare(Host host1, Host host2) {
        double host1Power = getPower(host1);
        double host2Power = getPower(host2);
        return Double.compare(host1Power, host2Power);
    }
    
    /**
     * This gets the current power of a named host and caches the answer. Caching
     * both improves speed of the sort and ensures the order of each host remains
     * constant.
     * @param host The host to get the current power consumption for
     * @return The power consumption of the host
     */
    private double getPower(Host host) {
        double answer;
        if (powerTable.containsKey(host)) {
            answer = powerTable.get(host);
        } else {
            answer = modeller.getCurrentEnergyForHost(host).getPower();
            powerTable.put(host, answer);
        }
        return answer;
    }

    
}
