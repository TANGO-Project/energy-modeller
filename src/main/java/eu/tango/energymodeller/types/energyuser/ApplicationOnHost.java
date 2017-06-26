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
package eu.tango.energymodeller.types.energyuser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class represents an energy user of the Tango project and in particular
 * an application running on a physical host.
 *
 * @author Richard Kavanagh
 */
public class ApplicationOnHost extends EnergyUsageSource implements WorkloadSource, Comparable<ApplicationOnHost> {

    public enum JOB_STATUS {PENDING, RUNNING, SUSPENDED, COMPLETING, COMPLETED };
    
    private int id;
    private String name;
    private Host allocatedTo;
    private Calendar created;
    private JOB_STATUS status;

    /**
     * Creates an instance of an application which is to be allocated power 
     * consumption
     * @param id The id of the application
     * @param name The name of the application
     * @param allocatedTo The host the application is allocated to
     */
    public ApplicationOnHost(int id, String name, Host allocatedTo) {
        this.id = id;
        this.name = name;
        this.allocatedTo = allocatedTo;
    }

    /**
     * Creates an instance of an application which is to be allocated power 
     * consumption
     * @param id The id of the application
     * @param name The name of the application
     * @param allocatedTo The host the application is allocated to
     * @param created The time the application was started
     */
    public ApplicationOnHost(int id, String name, Host allocatedTo, Calendar created) {
        this.id = id;
        this.name = name;
        this.allocatedTo = allocatedTo;
        this.created = created;
    }
    
    /**
     * This gets the id associated with this application (process id number).
     *
     * @return The process id number of the application.
     */
    public int getId() {
        return id;
    }    
    
    /**
     * This sets the process id associated with this application.
     *
     * @param id The application.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * This gets the name this application is known by.
     *
     * @return The applications name
     */
    public String getName() {
        return name;
    }

    /**
     * This sets the name this application is known by
     *
     * @param name The applications name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This sets which host this application is allocated to.
     *
     * @param allocatedTo The host this app is allocated to
     */
    public void setAllocatedTo(Host allocatedTo) {
        this.allocatedTo = allocatedTo;
    }

    /**
     * This gets the date the application was started.
     *
     * @return The start time of the application.
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * This sets the date the application was instantiated.
     *
     * @param created The boot time of the application.
     */
    public void setCreated(Calendar created) {
        this.created = created;
    }

    /**
     * This indicates which host this application is allocated to.
     *
     * @return the allocatedTo The host this app is allocated to
     */
    public Host getAllocatedTo() {
        return allocatedTo;
    }

    /**
     * This gets the jobs status
     * @return The status of this job when it was last measured.
     */
    public JOB_STATUS getStatus() {
        return status;
    }

    /**
     * This sets the status of this job
     * @param status The new status of this job
     */
    public void setStatus(JOB_STATUS status) {
        this.status = status;
    }

    /**
     * This returns the idle power consumption of this application given it's
     * host's idle power consumption and the amount of applications that are on
     * the same host.
     *
     * @param appCount The count of applications to share the energy between
     * @return The idle power consumption of a application given the count of
     * other applications present.
     */
    public double getIdlePowerConsumption(int appCount) {
        if (getAllocatedTo() == null) {
            return 0;
        }
        return getAllocatedTo().getIdlePowerConsumption() / ((double) appCount);
    }

    /**
     * This returns the time in seconds that have passed since this application 
     * was started.
     *
     * @return The time in seconds since boot. This returns -1 if the created
     * date is unknown.
     */
    public long getTimeFromStart() {
        if (created == null) {
            return -1;
        }
        long currentTime = new GregorianCalendar().getTimeInMillis();
        long bootTimeMilliSecs = created.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toSeconds(currentTime - bootTimeMilliSecs);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApplicationOnHost) {
            ApplicationOnHost app = (ApplicationOnHost) obj;
            return name.equals(app.getName()) && id == app.getId();
        }
        return false;

    }    

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + this.id;
        hash = 47 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public int compareTo(ApplicationOnHost application) {
        return this.getName().compareTo(application.getName());
    }

    @Override
    public String toString() {
        return "name: " + name + 
        " id: " + id +
        "host : " + allocatedTo +
        "started: " + created;
    }   

    /**
     * This casts a application on host collection into a energy usage source 
     * collection.
     *
     * @param applications The application collection to cast into its
     * parent type.
     * @return The collection of applications in its super type. This is backed by a
     * hashset.
     */
    public static Collection<EnergyUsageSource> castToEnergyUser(Collection<ApplicationOnHost> applications) {
        Collection<EnergyUsageSource> answer = new HashSet<>();
        answer.addAll(applications);
        return answer;
    }

    /**
     * This casts a application list into a energy user list.
     *
     * @param applications The application on host list to cast into its parent
     * type.
     * @return The list of applications in its super type. This is backed by an array
     * list.
     */
    public static List<EnergyUsageSource> castToEnergyUser(List<ApplicationOnHost> applications) {
        List<EnergyUsageSource> answer = new ArrayList<>();
        answer.addAll(applications);
        return answer;
    }
    
    /**
     * This casts a application on host collection into a workload source 
     * collection.
     *
     * @param applications The application collection to cast into its
     * parent type.
     * @return The collection of applications in its super type. This is backed by a
     * hashset.
     */
    public static Collection<WorkloadSource> castToWorkloadSource(Collection<ApplicationOnHost> applications) {
        Collection<WorkloadSource> answer = new HashSet<>();
        answer.addAll(applications);
        return answer;
    }

    /**
     * This casts a application list into a WorkloadSource list.
     *
     * @param applications The application list to cast into its parent
     * type.
     * @return The list of applications in its super type. This is backed by an array
     * list.
     */
    public static List<WorkloadSource> castToWorkloadSource(List<ApplicationOnHost> applications) {
        List<WorkloadSource> answer = new ArrayList<>();
        answer.addAll(applications);
        return answer;
    }       

    /**
     * This takes a list of applications and returns only the list on a named host.
     * @param applications The list of all applications
     * @param host The host to find its list of applications for
     * @return The list of applications on the named host
     */
    public static List<ApplicationOnHost> filter(List<ApplicationOnHost> applications, Host host) {
                List<ApplicationOnHost> answer = new ArrayList<>();
        for (ApplicationOnHost application : applications) {
            if (application.allocatedTo.equals(host)) {
                answer.add(application);
            }
        }
        return answer;
    }    
    
}