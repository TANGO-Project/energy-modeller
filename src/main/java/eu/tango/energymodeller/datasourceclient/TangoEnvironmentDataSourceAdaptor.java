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
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This adaptor allows for the gathering of environment information from both
 * CollectD (via InfluxDB) and SLURM, this represents the most information that
 * may be obtained from the Tango testbed.
 * 
 * The reason for this class is the InfluxDB is very good at time series data, but
 * SLURM has information that is useful such as a list of current applications that
 * are running. It is therefore useful to get a hybrid data source that can get
 * information from both.
 *
 * @author Richard Kavanagh
 */
public class TangoEnvironmentDataSourceAdaptor implements HostDataSource {

    private final SlurmDataSourceAdaptor slurm = new SlurmDataSourceAdaptor();
    private final CollectDInfluxDbDataSourceAdaptor collectD = new CollectDInfluxDbDataSourceAdaptor();
    
    private final HashMap<Host, Host> collectdToSlurm = new HashMap<>();
    private final HashMap<Host, Host> slurmToCollectD = new HashMap<>();
    
    /**
     * This creates a new data source adaptor that queries both SLURM and CollectD.
     */
    public TangoEnvironmentDataSourceAdaptor() {
        super();
    }
    
    @Override
    public Host getHostByName(String hostname) {
        /**
         * SLURM provides better information about the adaptors that are present
         * i.e. static information about the host
         */
        return slurm.getHostByName(hostname);
    }

    @Override
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname) {
        return slurm.getGeneralPowerConsumerByName(hostname);
    }

    @Override
    public VmDeployed getVmByName(String name) {
        return slurm.getVmByName(name);
    }

    @Override
    public List<Host> getHostList() {
        /**
         * SLURM provides better information about the adaptors that are present
         * i.e. static information about the host
         */        
        return slurm.getHostList();
    }

    @Override
    public List<EnergyUsageSource> getHostAndVmList() {
        /**
         * SLURM provides better information about the adaptors that are present
         * i.e. static information about the host
         */        
        return slurm.getHostAndVmList();
    }

    @Override
    public List<GeneralPurposePowerConsumer> getGeneralPowerConsumerList() {
        return slurm.getGeneralPowerConsumerList();
    }

    @Override
    public List<VmDeployed> getVmList() {
        return slurm.getVmList();
    }

    @Override
    public List<ApplicationOnHost> getHostApplicationList(ApplicationOnHost.JOB_STATUS state) {
        /**
         * SLURM provides this information while CollectD does not.
         */
        return slurm.getHostApplicationList(state);
    }

    @Override
    public List<ApplicationOnHost> getHostApplicationList() {
        /**
         * SLURM provides this information while CollectD does not.
         */        
        return slurm.getHostApplicationList();
    }

    @Override
    public HostMeasurement getHostData(Host host) {
        HostMeasurement answer = slurm.getHostData(host);
        //Adds various information such as memory usage, including static upper bound values.
        answer.addMetrics(collectD.getHostData(convertNames(host)));
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData() {
        List<HostMeasurement> answer = new ArrayList<>();
        for (Host host : slurm.getHostList()) {
            answer.add(getHostData(host));
        }
        return answer;
    }

    @Override
    public List<HostMeasurement> getHostData(List<Host> hostList) {
        List<HostMeasurement> answer = new ArrayList<>();
        for (Host host : hostList) {
            answer.add(getHostData(host));
        }
        return answer;
    }

    @Override
    public VmMeasurement getVmData(VmDeployed vm) {
        return slurm.getVmData(vm);
    }

    @Override
    public List<VmMeasurement> getVmData() {
        return slurm.getVmData();
    }

    @Override
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList) {
        return slurm.getVmData(vmList);
    }

    @Override
    public CurrentUsageRecord getCurrentEnergyUsage(Host host) {
        return collectD.getCurrentEnergyUsage(convertNames(host));
    }

    @Override
    public double getLowestHostPowerUsage(Host host) {
        return collectD.getLowestHostPowerUsage(convertNames(host));
    }

    @Override
    public double getHighestHostPowerUsage(Host host) {
        return collectD.getHighestHostPowerUsage(convertNames(host));
    }

    @Override
    public double getCpuUtilisation(Host host, int durationSeconds) {
        return collectD.getCpuUtilisation(convertNames(host), durationSeconds);
    }
    
    public Host convertNames(Host host) {
        //If it contains bullx then it is from collectd
        if (host.getHostName().contains(".bullx")) {
            if (collectdToSlurm.containsKey(host)) {
                return collectdToSlurm.get(host);
            } else {
                collectdToSlurm.put(host, slurm.getHostByName(host.getHostName().substring(0, host.getHostName().length() - 6)));
                slurmToCollectD.put(slurm.getHostByName(host.getHostName().substring(0, host.getHostName().length() - 6)), host);
            }
        } else { //Slurm host received to get the collectd host
            if (slurmToCollectD.containsKey(host)) {
                return slurmToCollectD.get(host);
            } else {
                collectdToSlurm.put(collectD.getHostByName(host.getHostName() + ".bullx"), host);
                slurmToCollectD.put(host, collectD.getHostByName(host.getHostName() + ".bullx"));
            }            
        }
        return null;
    }
    
}
