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

import eu.tango.energymodeller.datasourceclient.compsstype.CompssImplementation;
import eu.tango.energymodeller.datasourceclient.compsstype.CompssResource;
import static eu.tango.energymodeller.datasourceclient.compsstype.JsonUtils.readJsonFromXMLFile;
import eu.tango.energymodeller.types.energyuser.Accelerator;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.EnergyUsageSource;
import eu.tango.energymodeller.types.energyuser.GeneralPurposePowerConsumer;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.usage.CurrentUsageRecord;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This adaptor integrates the energy modeller directly into the compss runtime.
 * It is intended as part of a composite adaptor that uses both the compss runtime
 * and collectd as a datasource.
 * @author Richard Kavanagh
 */
public class CompssDatasourceAdaptor implements HostDataSource, ApplicationDataSource {
    
    /**
     * An example of the monitoring file to obtain is: 
     * "~/.COMPSs/EmulateRemote_04/monitor/COMPSs_state.xml"
     * 
     * The 04 in EmulateRemote_04 is part of an incrementing number, it has been 
     * chosen to take the latest folder in the compss monitor directory.
     */
    private String monitoringDirectory = System.getProperty("user.home") + "/.COMPSs/";
    private String monitoringFile = "/monitor/COMPSs_state.xml";
    private static final String COMPSSS_STATE = "COMPSsState";
    private static final String RESOURCE_INFO = "ResourceInfo";
    private static final String TASK_INFO = "TasksInfo";
    
    /**
     * This filter is for directories. The most relevant program to query is the
     * most resent one.
     */
    FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
        };    
    
    /**
     * This provides the client commands through the programming model:
     *
     * https://github.com/TANGO-Project/programming-model-and-runtime When the
     * application is running, the adaptation of the nodes can be performed by
     * means of the adapt_compss_resources command in the following way: $
     * adapt_compss_resources <master_node> <master_job_id> CREATE SLURM-Cluster
     * default <singularity_image>
     * This command will submit another job requesting a new resource of type
     * "default" (the same as the requested in the enqueue_compss) running the
     * COMPSs worker of the singularity_image. $ adapt_compss_resources
     * <master_node> <master_job_id> REMOVE SLURM-Cluster <node_to_delete>
     *
     */
    /**
     * Part of the role of this class will be to parse the output JSON or XML of
     * the programming model runtime in order to provide an application's
     * runtime information.
     */
    /**
     * This gets the compss resources list, indicating which hosts have which
     * worker instances available.
     * @return
     */
    public List<CompssResource> getCompssResources() {
        try {
            JSONObject items = readJsonFromXMLFile(getCurrentMonitoringFile());
            if (!items.has(COMPSSS_STATE)) {
                /**
                 * The file to be parsed might not be fully populated, if it isn't
                 * then this avoids parse errors.
                 */
                return new ArrayList<>();
            }              
            JSONObject compssState = items.getJSONObject(COMPSSS_STATE);  
            JSONObject resourceInfo = compssState.getJSONObject(RESOURCE_INFO);
            return CompssResource.getCompssResouce(resourceInfo);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return new ArrayList<>();
    }
    
    /**
     * Part of the role of this class will be to parse the output JSON or XML of
     * the programming model runtime in order to provide an application's
     * runtime information.
     */
    
    /**
     * This lists the various different versions of workers that are available.
     * @return 
     */
    public List<CompssImplementation> getCompssImplementation() {
        try {
            JSONObject items = readJsonFromXMLFile(getCurrentMonitoringFile());
            if (!items.has(COMPSSS_STATE)) {
                /**
                 * The file to be parsed might not be fully populated, if it isn't
                 * then this avoids parse errors.
                 */
                return new ArrayList<>();
            }            
            JSONObject compssState = items.getJSONObject(COMPSSS_STATE);           
            JSONObject coresInfo = compssState.getJSONObject("CoresInfo");            
            return CompssImplementation.getCompssImplementation(coresInfo);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return new ArrayList<>();
    }
    
    /**
     * This obtains the current monitoring file that is in use. It is represented
     * by the latest folder in the compss directory (~/.COMPSs/).
     * @return 
     */
    private String getCurrentMonitoringFile() {
        String answer = "";
        File[] files = new File(monitoringDirectory).listFiles(filter);

        Arrays.sort(files, new Comparator<File>(){
            @Override
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            } });
        //get the newest folder if it exists
        for (File file : files) {
            File monFile = new File(file.getAbsoluteFile() + monitoringFile);
            if (monFile.exists() && monFile.length() > 0) {
                return file.getAbsoluteFile() + monitoringFile;
            } else {// the monitoring file doesn't exist so wait a few seconds and try again.
                sleep();
            if (monFile.exists() && monFile.length() > 0) {
                    return file.getAbsoluteFile() + monitoringFile;
                } else {
                    Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.WARNING, 
                            "Compss file was not found, I waited but it was still not found. "
                                    + "Trying to read {0}", file.getAbsoluteFile() + monitoringFile);                    
                }
            }
        }
        return answer;
    }
    
    /**
     * This obtains the job id of the application that is current undergoing monitoring 
     * its name forms the latest folder in the compss directory (~/.COMPSs/).
     * @return The master job id of the application been monitored. such as: EmulateRemote_01
     */
    public String getCurrentMonitoringJobId() {
        String answer = "";
        File[] files = new File(monitoringDirectory).listFiles(filter);

        Arrays.sort(files, new Comparator<File>(){
            @Override
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            } });
        //get the newest folder if it exists
        for (File file : files) {
            if (new File(file.getAbsoluteFile() + monitoringFile).exists()) {
                return file.getName();
            } else {// the monitoring file doesn't exist so wait a few seconds and try again.
                sleep();
                if (new File(file.getAbsoluteFile() + monitoringFile).exists()) {
                    return file.getName();
                } else {
                    Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.WARNING, 
                            "Compss file was not found, I waited but it was still not found.");                    
                }
            }
        }
        return answer;
    }    
    
    /**
     * Sleeps the thread for 3 seconds. This allows chance for a monitoring file 
     * that is to be read to be written to disk if needed
     */
    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    public String getMonitoringDirectory() {
        return monitoringDirectory;
    }

    public String getMonitoringFile() {
        return monitoringFile;
    }

    /**
     * This allows the compss home directory to be set. i.e. ~/.COMPSs
     * @param monitoringDirectory The directory to monitor for the correct files
     */
    public void setMonitoringDirectory(String monitoringDirectory) {
        this.monitoringDirectory = monitoringDirectory;
    }

    /**
     * THe monitoring file to obtain usually something like: "/monitor/COMPSs_state.xml"
     * @param monitoringFile The monitoring file to parse for compss output data
     */
    public void setMonitoringFile(String monitoringFile) {
        this.monitoringFile = monitoringFile;
    }
        
    
    @Override
    public Host getHostByName(String hostname) {
        List<Host> Hosts = getHostList();
        for (Host host : Hosts) {
            if (host.getHostName().equals(hostname)) {
                return host;
            }
        }
        return null;
    }

    @Override
    public GeneralPurposePowerConsumer getGeneralPowerConsumerByName(String hostname) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VmDeployed getVmByName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This gets the compss resources list, indicating which hosts have which
     * worker instances available.
     * @return
     */
    @Override
    public List<Host> getHostList() {
        List<Host> answer = new ArrayList<>();
        try {
            JSONObject items = readJsonFromXMLFile(getCurrentMonitoringFile());
            if (!items.has(COMPSSS_STATE)) {
                /**
                 * The file to be parsed might not be fully populated, if it isn't
                 * then this avoids parse errors.
                 */
                return answer;
            }
            JSONObject compssState = items.getJSONObject(COMPSSS_STATE);             
            JSONObject resourceInfo = compssState.getJSONObject(RESOURCE_INFO);
            List<CompssResource> resourceListing = CompssResource.getCompssResouce(resourceInfo);
            for (CompssResource resource : resourceListing) {
                if (resource.getHostname().contains("requested new VM")) {
                    /**
                     * This avoids parsing errors at the point a new resource is 
                     * added to the job, but before the host is fully detected.
                     */ 
                    continue;
                }
                Host host = new Host(Integer.parseInt(
                                resource.getHostname().replaceAll("[^0-9]", "")), 
                                resource.getHostname());
                host.setDiskGb((resource.getDiskSize() < 0 ? 0 : resource.getDiskSize()));
                host.setCoreCount(resource.getCoreCount());
                if (resource.getGpuCount() > 0) {
                    host.addAccelerator(new Accelerator("gpu", resource.getGpuCount(), Accelerator.AcceleratorType.GPU));
                }
                if (resource.getFpgaCount() > 0) {
                    host.addAccelerator(new Accelerator("fpga", resource.getGpuCount(), Accelerator.AcceleratorType.FPGA));
                }
                //compss considers a host in the "running" state to be available
                host.setAvailable(resource.getState().trim().equalsIgnoreCase("Running"));
                host.setState(resource.getState());
                if (resource.isIdle()) {
                    host.setState("IDLE");
                }
                answer.add(host);
            }
            
        } catch (IOException | JSONException ex) {
            Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return answer;
    }

    @Override
    public List<EnergyUsageSource> getHostAndVmList() {
        List<EnergyUsageSource> answer = new ArrayList<>();
        answer.addAll(getHostList());
        return answer;
    }

    @Override
    public List<GeneralPurposePowerConsumer> getGeneralPowerConsumerList() {
        return new ArrayList<>();
    }

    @Override
    public List<VmDeployed> getVmList() {
        return new ArrayList<>();
    }

    @Override
    public List<ApplicationOnHost> getHostApplicationList(ApplicationOnHost.JOB_STATUS state) {
        ArrayList<ApplicationOnHost> answer = new ArrayList<>();
        List<ApplicationOnHost> allApps = getHostApplicationList();
        for (ApplicationOnHost app : allApps) {
            if ((app.getStatus() == null || state == null) || app.getStatus().equals(state)) {
                answer.add(app);
            }
        }
        return answer;
    }

    @Override
    /**
     * This lists the tasks that are currently running in the compss environment
     * @return The list of currently running tasks
     */
    public List<ApplicationOnHost> getHostApplicationList() {
        List<ApplicationOnHost> answer = new ArrayList<>();
        try {
            JSONObject items = readJsonFromXMLFile(getCurrentMonitoringFile());
            if (!items.has(COMPSSS_STATE)) {
                /**
                 * The file to be parsed might not be fully populated, if it isn't
                 * then this avoids parse errors.
                 */
                return answer;
            }            
            JSONObject compssState = items.getJSONObject(COMPSSS_STATE);             
            JSONObject resourceInfo = compssState.getJSONObject(RESOURCE_INFO);
            List<CompssResource> resourceListing = CompssResource.getCompssResouce(resourceInfo);
            //Prevent old completed tasks from appearing as still running
            if (getRunningTaskCount(compssState) == 0) {
                return answer;
            }
            for (CompssResource resource : resourceListing) {
                if (resource.getHostname().contains("requested new VM")) {
                    /**
                     * This avoids parsing errors at the point a new resource is 
                     * added to the job, but before the host is fully detected.
                     */ 
                    continue;
                }                
                Host host = new Host(Integer.parseInt(
                                resource.getHostname().replaceAll("[^0-9]", "")), 
                                resource.getHostname());
                host.setDiskGb((resource.getDiskSize() < 0 ? 0 : resource.getDiskSize()));
                host.setCoreCount(resource.getCoreCount());
                if (resource.getGpuCount() > 0) {
                    host.addAccelerator(new Accelerator("gpu", resource.getGpuCount(), Accelerator.AcceleratorType.GPU));
                }
                if (resource.getFpgaCount() > 0) {
                    host.addAccelerator(new Accelerator("fpga", resource.getGpuCount(), Accelerator.AcceleratorType.FPGA));
                }
                //compss considers a host in the "running" state to be available
                host.setAvailable(resource.getState().trim().equalsIgnoreCase("Running"));
                host.setState(resource.getState());
                if (resource.isIdle()) {
                    host.setState("IDLE");
                }
                for(String action : resource.getCurrentActions()) {
                    String appNameAndId = getCurrentMonitoringJobId();
                    ApplicationOnHost app = new ApplicationOnHost(Integer.parseInt(appNameAndId.replaceAll("[^0-9]", "")), appNameAndId.replaceAll("[_0-9]", ""), host);
                    /**
                     * //action = A string such as: "ExecutionAction ( Task 4, CE name multiplyBlocks)"
                    //"CE name multiplyBlocks" identifies the core implementation of the core element 
                    * but the string as a whole keeps changing so fast events can't be usefully injected into the system.
                     */
                    app.addProperty("ACTION", action);
                    app.addProperty("CE_NAME", action.substring(action.indexOf("CE name ") + 8, action.length() - 1)) ;
                    answer.add(app);
                }
            }
            
        } catch (IOException | JSONException ex) {
            Logger.getLogger(CompssDatasourceAdaptor.class.getName()).log(Level.SEVERE, "parse error", ex);
        }
        return answer;
    }

    /**
     * This gets the list of tasks that are to yet to be processed.
     * @param compssState The compss state object to parse
     * @return The amount of tasks that are in progress.
     */
    private int getRunningTaskCount(JSONObject compssState) {
        JSONObject taskInfo = compssState.getJSONObject(TASK_INFO);
        if (taskInfo != null && taskInfo.has("Application")) {
            JSONObject application = taskInfo.getJSONObject("Application");
            if (application != null && application.has("InProgress")) {
                //Other options are "TotalCount" or "Completed"
                return application.getInt("InProgress");
            }
        }
        return 0;
    }
    
    @Override
    public HostMeasurement getHostData(Host host) {
        return new HostMeasurement(host);
    }

    @Override
    public List<HostMeasurement> getHostData() {
        return new ArrayList<>();
    }

    @Override
    public List<HostMeasurement> getHostData(List<Host> hostList) {
        return new ArrayList<>();
    }

    @Override
    public VmMeasurement getVmData(VmDeployed vm) {
        return new VmMeasurement(vm);
    }

    @Override
    public List<VmMeasurement> getVmData() {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public List<VmMeasurement> getVmData(List<VmDeployed> vmList) {
        return new ArrayList<>();
    }

    @Override
    public CurrentUsageRecord getCurrentEnergyUsage(Host host) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public double getLowestHostPowerUsage(Host host) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public double getHighestHostPowerUsage(Host host) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public double getCpuUtilisation(Host host, int durationSeconds) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public ApplicationMeasurement getApplicationData(ApplicationOnHost application) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public List<ApplicationMeasurement> getApplicationData() {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }

    @Override
    public List<ApplicationMeasurement> getApplicationData(List<ApplicationOnHost> appList) {
        throw new UnsupportedOperationException("Not supported by this adaptor.");
    }
    
}
