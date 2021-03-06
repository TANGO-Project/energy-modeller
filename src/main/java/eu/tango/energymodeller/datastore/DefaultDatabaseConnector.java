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

import eu.tango.energymodeller.types.TimePeriod;
import eu.tango.energymodeller.types.energyuser.ApplicationOnHost;
import eu.tango.energymodeller.types.energyuser.Host;
import eu.tango.energymodeller.types.energyuser.VmDeployed;
import eu.tango.energymodeller.types.energyuser.VmDiskImage;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyCalibrationData;
import eu.tango.energymodeller.types.energyuser.usage.HostProfileData;
import eu.tango.energymodeller.types.energyuser.usage.HostEnergyUserLoadFraction;
import eu.tango.energymodeller.types.usage.HostEnergyRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryBootRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryRecord;
import eu.tango.energymodeller.types.usage.VmLoadHistoryWeekRecord;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * This connects to the background database to return historical information and
 * host calibration data.
 *
 * @author Richard Kavanagh
 */
public class DefaultDatabaseConnector extends MySqlDatabaseConnector implements DatabaseConnector {

    /**
     * The url to contact the database.
     */
    private String databaseURL = "jdbc:mysql://localhost:3306/energymodeller";
    /**
     * The driver to be used to contact the database.
     */
    private String databaseDriver = "com.mysql.jdbc.Driver";
    /**
     * The user details to contact the database.
     */
    private String databaseUser = "energymodeller";
    /**
     * The user's password to contact the database.
     */
    private String databasePassword;
    private static final String CONFIG_FILE = "energy-modeller-db.properties";
    
    private Connection connection;

    /**
     * This creates a new database connector for use. It establishes a database
     * connection immediately ready for use.
     */
    public DefaultDatabaseConnector() {
        try {
            loadSettings();
            connection = getConnection();
        } catch (IOException | SQLException | ClassNotFoundException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * This reads the settings for the database connection from file.
     */
    protected final void loadSettings() {
        try {
            PropertiesConfiguration config;
            if (new File(CONFIG_FILE).exists()) {
                config = new PropertiesConfiguration(CONFIG_FILE);
            } else {
                config = new PropertiesConfiguration();
                config.setFile(new File(CONFIG_FILE));
            }
            config.setAutoSave(true); //This will save the configuration file back to disk. In case the defaults need setting.
            databaseURL = config.getString("energy.modeller.db.url", databaseURL);
            config.setProperty("energy.modeller.db.url", databaseURL);
            databaseDriver = config.getString("energy.modeller.db.driver", databaseDriver);
            try {
                Class.forName(databaseDriver);
            } catch (ClassNotFoundException ex) {
                //If the driver is not found on the class path revert to mysql connector.
                databaseDriver = "com.mysql.jdbc.Driver";
            }     
            config.setProperty("energy.modeller.db.driver", databaseDriver);
            databasePassword = config.getString("energy.modeller.db.password", "");
            config.setProperty("energy.modeller.db.password", databasePassword);
            databaseUser = config.getString("energy.modeller.db.user", databaseUser);
            config.setProperty("energy.modeller.db.user", databaseUser);
        } catch (ConfigurationException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.INFO, "Error loading database configuration information", ex);
        }
    }

    /**
     * Establishes a connection to the database.
     *
     * @return Connection object representing the connection
     * @throws IOException if properties file cannot be accessed
     * @throws SQLException if connection fails
     * @throws ClassNotFoundException if the database driver class is not found
     */
    @Override
    protected final Connection getConnection() throws IOException, SQLException, ClassNotFoundException {
        // Define JDBC driver
        System.setProperty("jdbc.drivers", databaseDriver);
        //Ensure that the driver has been loaded
        Class.forName(databaseDriver);
        if (databaseUser.isEmpty()) {
            return DriverManager.getConnection(databaseURL);
        }
        return DriverManager.getConnection(databaseURL,
                databaseUser,
                databasePassword);
    }

    /**
     * This list all the hosts the energy modeller has data for in its backing
     * store.
     *
     * @return The list of hosts
     */
    @Override
    public Collection<Host> getHosts() {
        Collection<Host> answer = new HashSet<>();
        connection = getConnection(connection);
        if (connection == null) {
            return null;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT host_id , host_name  FROM host");
                ResultSet resultSet = preparedStatement.executeQuery()) {
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> hostData : results) {
                Host host = new Host((Integer) hostData.get(0), (String) hostData.get(1));
                host = getHostCalibrationData(host);
                answer.add(host);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This list all the vms the energy modeller has data for in its backing
     * store.
     *
     * @return The list of hosts
     */
    @Override
    public Collection<VmDeployed> getVms() {
        Collection<VmDeployed> answer = new HashSet<>();
        connection = getConnection(connection);
        if (connection == null) {
            return null;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT vm_id , vm_name, deployment_id FROM vm");
                ResultSet resultSet = preparedStatement.executeQuery()) {
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> vmData : results) {
                VmDeployed vm = new VmDeployed((Integer) vmData.get(0), (String) vmData.get(1));
                vm.setDeploymentID((String) vmData.get(2));
                answer.add(vm);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This gets the calibration data that indicates the performance properties
     * of a given set of host machines.
     *
     * @param hosts The set of hosts to get the data for.
     * @return The calibration data for the named hosts.
     */
    @Override
    public Collection<Host> getHostCalibrationData(Collection<Host> hosts) {
        for (Host host : hosts) {
            host = getHostCalibrationData(host);
        }
        return hosts;
    }

    /**
     * This gets the calibration data that indicates the performance properties
     * of a given host machine.
     *
     * @param host The host to get the data for.
     * @return The host with its calibration data defined.
     */
    @Override
    public Host getHostCalibrationData(Host host) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return host;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT calibration_id, host_id, cpu, memory, power FROM host_calibration_data WHERE host_id = ?")) {
            preparedStatement.setInt(1, host.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<ArrayList<Object>> result = resultSetToArray(resultSet);
                for (ArrayList<Object> calibrationData : result) {
                    host.addCalibrationData(new HostEnergyCalibrationData(
                            (Double) calibrationData.get(2), //cpu
                            (Double) calibrationData.get(3), //memory
                            (Double) calibrationData.get(4))); //power
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return host;
    }

    @Override
    public Host getHostProfileData(Host host) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return host;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT host_profile_id, host_id, type, value FROM host_profile_data WHERE host_id = ?")) {
            preparedStatement.setInt(1, host.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<ArrayList<Object>> result = resultSetToArray(resultSet);
                for (ArrayList<Object> profileData : result) {
                    host.addProfileData(new HostProfileData(
                            (String) profileData.get(2),
                            (Double) profileData.get(3)));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return host;
    }

    /**
     * This adds set of host machines to the database. If the host already
     * exists the values contained will be overwritten.
     *
     * @param hosts The set of hosts to write to the database.
     */
    @Override
    public void setHosts(Collection<Host> hosts) {
        connection = getConnection(connection);
        if (connection == null || hosts == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO host (host_id, host_name) VALUES (?,?) ON DUPLICATE KEY UPDATE host_name=VALUES(`host_name`);")) {
            for (Host host : hosts) {
                preparedStatement.setInt(1, host.getId());
                preparedStatement.setString(2, host.getHostName());
                preparedStatement.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This adds set of vms to the database. If the vm already exists the values
     * contained will be overwritten.
     *
     * @param vms The set of vms to write to the database.
     */
    @Override
    public void setVms(Collection<VmDeployed> vms) {
        connection = getConnection(connection);
        if (connection == null || vms == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm (vm_id, vm_name, deployment_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE vm_name=VALUES(`vm_name`), deployment_id=COALESCE(VALUES(`deployment_id`), deployment_id);")) {
            for (VmDeployed vm : vms) {
                preparedStatement.setInt(1, vm.getId());
                preparedStatement.setString(2, vm.getName());
                preparedStatement.setString(3, vm.getDeploymentID());
                preparedStatement.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public VmDeployed getVMProfileData(VmDeployed vm) {
        //get the app tag data
        vm = getVmAppTags(vm);
        //get the disk data
        vm = getVmDisks(vm);
        return vm;
    }

    @Override
    public Collection<VmDeployed> getVMProfileData(Collection<VmDeployed> vms) {
        for (VmDeployed vm : vms) {
            getVMProfileData(vm);
        }
        return vms;
    }

    /**
     * For the named vm this populates the app tags list of the VM.
     *
     * @param vm The VM to get the tags into the database for
     * @return The VM with its application tags set
     */
    private VmDeployed getVmAppTags(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return vm;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT vm.vm_id, vm_app_tag.tag_name "
                + "FROM vm, vm_app_tag, vm_app_tag_arr "
                + "WHERE vm.vm_id = ? AND "
                + "vm.vm_id = vm_app_tag_arr.vm_id AND "
                + "vm_app_tag_arr.vm_app_tag_id = vm_app_tag.vm_app_tag_id")) {
            preparedStatement.setInt(1, vm.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> vmData : results) {
                vm.addApplicationTag((String) vmData.get(1));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vm;
    }

    /**
     * For the named vm this populates the disk list of the VM.
     *
     * @param vm The VM to get the disk list from the database
     * @return The VM with its disk values set
     */
    private VmDeployed getVmDisks(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return vm;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT vm.vm_id, vm_disk.disk_name "
                + "FROM vm, vm_disk, vm_disk_arr "
                + "WHERE vm.vm_id = ? AND "
                + "vm.vm_id = vm_disk_arr.vm_id AND "
                + "vm_disk_arr.vm_disk_id = vm_disk.vm_disk_id")) {
            preparedStatement.setInt(1, vm.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> vmData : results) {
                vm.addApplicationTag((String) vmData.get(1));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vm;
    }

    @Override
    public void setVMProfileData(VmDeployed vm) {
        //set the list of app tags
        setVMAppTags(vm);
        //set the list of disks
        setDiskInformation(vm);
        //assign the tags to the given vm
        setVMAppTagArray(vm);
        //set the references to each disk for the VM
        setDiskInformationArray(vm);
    }

    /**
     * This sets the association between a VM and its app tags.
     *
     * @param vm The VM to save the tags into the database for
     */
    private void setVMAppTagArray(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm_app_tag_arr (vm_app_tag_arr.vm_id, vm_app_tag_arr.vm_app_tag_id) "
                + "SELECT ? as vm_id, vm_app_tag.vm_app_tag_id "
                + "FROM vm_app_tag WHERE vm_app_tag.tag_name = ? ON DUPLICATE KEY UPDATE vm_app_tag_arr.vm_id=vm_app_tag_arr.vm_id")) {
            for (String appTag : vm.getApplicationTags()) {
                preparedStatement.setInt(1, vm.getId());
                preparedStatement.setString(2, appTag);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This sets the association between a VM and its disks.
     *
     * @param vm The VM to save the disk information into the database for
     */
    private void setDiskInformationArray(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm_disk_arr (vm_disk_arr.vm_id, vm_disk_arr.vm_disk_id) "
                + "SELECT ? as vm_id, vm_disk.vm_disk_id "
                + "FROM vm_disk WHERE vm_disk.disk_name = ? ON DUPLICATE KEY UPDATE vm_disk_arr.vm_id=vm_disk_arr.vm_id")) {
            for (VmDiskImage diskImage : vm.getDiskImages()) {
                preparedStatement.setInt(1, vm.getId());
                preparedStatement.setString(2, diskImage.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * For a given VM this records all the identified tags into the database.
     *
     * @param vm The VM to save the tags into the database for
     */
    private void setVMAppTags(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm_app_tag (tag_name) VALUES (?) ON DUPLICATE KEY UPDATE tag_name=VALUES(tag_name)")) {
            for (String appTag : vm.getApplicationTags()) {
                preparedStatement.setString(1, appTag);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * For a given VM this records all the references to disks into the
     * database.
     *
     * @param vm The VM to save the disk information into the database for
     */
    private void setDiskInformation(VmDeployed vm) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm_disk (disk_name) VALUES (?) ON DUPLICATE KEY UPDATE disk_name=VALUES(disk_name)")) {
            for (VmDiskImage diskImage : vm.getDiskImages()) {
                preparedStatement.setString(1, diskImage.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This writes to the database for a named host its calibration data
     *
     * @param host The host to set the calibration data for.
     */
    @Override
    public void setHostCalibrationData(Host host) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO host_calibration_data (host_id, cpu, memory, power) VALUES (?, ?, ?, ?) "
                + " ON DUPLICATE KEY UPDATE host_id=VALUES(`host_id`), cpu=VALUES(`cpu`), memory=VALUES(`memory`), power=VALUES(`power`);")) {
            preparedStatement.setInt(1, host.getId());
            for (HostEnergyCalibrationData data : host.getCalibrationData()) {
                preparedStatement.setInt(1, host.getId());
                preparedStatement.setDouble(2, data.getCpuUsage());
                preparedStatement.setDouble(3, data.getMemoryUsage());
                preparedStatement.setDouble(4, data.getWattsUsed());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void setHostProfileData(Host host) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO host_profile_data (host_id, type, value) VALUES (?, ?, ?);")) {
            preparedStatement.setInt(1, host.getId());
            for (HostProfileData data : host.getProfileData()) {
                preparedStatement.setString(2, data.getType());
                preparedStatement.setDouble(3, data.getValue());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This writes historic data for a given host to the database.
     *
     * @param host The host to write the data for
     * @param time The time when the measurement was taken.
     * @param power The power reading for the host.
     * @param energy The current reading for the energy used. Note: this value
     * is to be treated like a meter reading for an energy firm. The point at
     * which 0 energy usage occurred is an arbritrary point in the past. Two
     * historical values can therefore be used to indicate the energy used
     * between the two points in time.
     */
    @Override
    public void writeHostHistoricData(Host host, long time, double power, double energy) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO host_measurement (host_id, clock, energy, power) VALUES (?, ?, ? , ?);")) {
            preparedStatement.setInt(1, host.getId());
            preparedStatement.setLong(2, time);
            preparedStatement.setDouble(3, energy);
            preparedStatement.setDouble(4, power);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This returns the historic data for a given host, in a specified time
     * period.
     *
     * @param host The host machine to get the data for.
     * @param timePeriod The start and end period for which to query for. If
     * null all records will be returned.
     * @return The energy readings taken for a given host.
     */
    @Override
    public List<HostEnergyRecord> getHostHistoryData(Host host, TimePeriod timePeriod) {
        connection = getConnection(connection);
        List<HostEnergyRecord> answer = new ArrayList<>();
        if (connection == null || host == null) {
            return answer;
        }
        PreparedStatement preparedStatement = null;
        try {
            if (timePeriod != null) {
                long start = timePeriod.getStartTimeInSeconds();
                long end = timePeriod.getEndTimeInSeconds();
                preparedStatement = connection.prepareStatement(
                        "SELECT host_id, clock, energy, power FROM host_measurement WHERE host_id = ? "
                        + " AND clock >= ? AND clock <= ?;");
                preparedStatement.setLong(2, start);
                preparedStatement.setLong(3, end);
            } else {
                preparedStatement = connection.prepareStatement(
                        "SELECT host_id, clock, energy, power FROM host_measurement WHERE host_id = ?;");
            }
            preparedStatement.setInt(1, host.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
                for (ArrayList<Object> hostMeasurement : results) {
                    answer.add(new HostEnergyRecord(
                            host,
                            (long) hostMeasurement.get(1), //clock is the 1st item
                            (double) hostMeasurement.get(3), //power 3rd item
                            (double) hostMeasurement.get(2))); //energy is 2nd item
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return answer;
    }

    @Override
    public void writeHostVMHistoricData(Host host, long time, HostEnergyUserLoadFraction load) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO vm_measurement (host_id, vm_id, clock, cpu_load, power_overhead) VALUES (?, ?, ? , ?, ?);")) {
            preparedStatement.setInt(1, host.getId());
            double averageOverhead = load.getHostPowerOffset() / load.getEnergyUsageSources().size();
            for (VmDeployed vm : load.getEnergyUsageSourcesAsVMs()) {
                preparedStatement.setInt(2, vm.getId());
                preparedStatement.setLong(3, time);
                preparedStatement.setDouble(4, load.getFraction(vm));
                preparedStatement.setDouble(5, averageOverhead);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void writeApplicationHistoricData(Host host, long time, HostEnergyUserLoadFraction load) {
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO app_measurement (host_id, app_id, clock, cpu_load, power_overhead) VALUES (?, ?, ? , ?, ?);")) {
            preparedStatement.setInt(1, host.getId());
            double averageOverhead = load.getHostPowerOffset() / load.getEnergyUsageSources().size();
            for (ApplicationOnHost app : load.getEnergyUsageSourcesAsApps()) {
                preparedStatement.setInt(2, app.getId());
                preparedStatement.setLong(3, time);
                preparedStatement.setDouble(4, load.getFraction(app));
                preparedStatement.setDouble(5, averageOverhead);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This is part of a caching mechanism for Vms when getting historic load
     * data, the aim is to create less vm objects (and thus reduce the footprint
     * of this method).
     *
     * @param id The VM id.
     * @param name The name of the VM
     * @param host The host it is running from.
     * @return The reference to the VM.
     */
    private VmDeployed getVM(int id, String name, Host host, HashMap<String, VmDeployed> vmCache) {
        VmDeployed vm = vmCache.get(name);
        if (vm == null || !vm.getAllocatedTo().equals(host)) {
            vm = new VmDeployed(id, name);
            vm.setAllocatedTo(host);
            vmCache.put(vm.getName(), vm);
        }
        return vm;
    }

    @Override
    public Collection<HostEnergyUserLoadFraction> getHostVmHistoryLoadData(Host host, TimePeriod timePeriod) {
        HashMap<String, VmDeployed> vmCache = new HashMap<>();
        List<HostEnergyUserLoadFraction> answer = new ArrayList<>();
        connection = getConnection(connection);
        if (connection == null || host == null) {
            return answer;
        }
        PreparedStatement preparedStatement = null;
        try {
            if (timePeriod != null) {
                long start = timePeriod.getStartTimeInSeconds();
                long end = timePeriod.getEndTimeInSeconds();
                preparedStatement = connection.prepareStatement(
                        "SELECT host_id, vm_measurement.vm_id, vm_name, clock, cpu_load, power_overhead FROM vm_measurement, vm "
                        + "WHERE vm_measurement.vm_id = vm.vm_id "
                        + "and vm_measurement.host_id = ? "
                        + " AND clock >= ? AND clock <= ?;");
                preparedStatement.setLong(2, start);
                preparedStatement.setLong(3, end);
            } else {
                preparedStatement = connection.prepareStatement(
                        "SELECT host_id, vm_measurement.vm_id, vm_name, clock, cpu_load, power_overhead FROM vm_measurement, vm "
                        + "WHERE vm_measurement.vm_id = vm.vm_id "
                        + "and vm_measurement.host_id = ?;");
            }
            preparedStatement.setInt(1, host.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
                long lastClock = Long.MIN_VALUE;
                long currentClock;
                HostEnergyUserLoadFraction currentHostLoadFraction = null;
                for (ArrayList<Object> measurement : results) {
                    currentClock = (long) measurement.get(3); //clock is the 3rd item)
                    if (currentClock != lastClock || currentHostLoadFraction == null) {
                        currentHostLoadFraction = new HostEnergyUserLoadFraction(host, currentClock);
                        VmDeployed vm = getVM((int) measurement.get(1), (String) measurement.get(2), host, vmCache);
                        currentHostLoadFraction.addFraction(vm, (double) measurement.get(4)); //load is the fourth item
                        currentHostLoadFraction.setHostPowerOffset((double) measurement.get(5)); //power overhead is fifth item
                        answer.add(currentHostLoadFraction);
                    } else {
                        VmDeployed vm = getVM((int) measurement.get(1), (String) measurement.get(2), host, vmCache);
                        currentHostLoadFraction.addFraction(vm, (double) measurement.get(4)); //load is the fourth item
                        currentHostLoadFraction.setHostPowerOffset((double) measurement.get(5)); //overhead power is fifth item
                    }
                    lastClock = currentClock;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return answer;
    }

    /**
     * This runs a query that returns the average CPU utilisation for either an
     * app tag or a vm disk reference.
     *
     * @param query The query to run either for disks or app tags.
     * @param queryItem The item to search for
     * @return The average CPU utilisation for the term.
     */
    private VmLoadHistoryRecord getAverageCPUUtilisation(String query, String queryItem) {
        double answer = 0.0;
        double stdDev = 0.0;
        connection = getConnection(connection);
        if (connection == null) {
            return new VmLoadHistoryRecord(answer, stdDev);
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                query)) {
            preparedStatement.setString(1, queryItem);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return new VmLoadHistoryRecord(resultSet.getDouble(1), resultSet.getDouble(2)); //return the single value from the query
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new VmLoadHistoryRecord(answer, stdDev);
    }

    /**
     * This returns the overall average CPU utilisation for a given tag.
     *
     * @param tagName The application tag to get the cpu usage for.
     * @return The average CPU usage generated by VMs with a given application
     * tag.
     */
    @Override
    public VmLoadHistoryRecord getAverageCPUUtilisationTag(String tagName) {
        return getAverageCPUUtilisation("SELECT avg(cpu_load), "
                + "STDDEV_POP(cpu_load) as standardDev, "
                + "FROM vm_measurement as mesu, "
                + "vm_app_tag_arr AS arr, "
                + "vm_app_tag AS tag "
                + "WHERE tag.vm_app_tag_id = arr.vm_app_tag_id "
                + "AND arr.vm_id = mesu.vm_id "
                + "AND tag.tag_name = ? "
                + "GROUP BY tag.tag_name", tagName);
    }

    /**
     * This returns the overall average CPU utilisation for a given disk
     * reference.
     *
     * @param diskRefStr The disk reference to get the cpu usage for.
     * @return The average CPU usage generated by VMs with a given vm disk
     * reference
     */
    @Override
    public VmLoadHistoryRecord getAverageCPUUtilisationDisk(String diskRefStr) {
        return getAverageCPUUtilisation("SELECT avg(cpu_load) "
                + "STDDEV_POP(cpu_load) as standardDev, "
                + "FROM vm_measurement as mesu, "
                + "vm_disk_arr AS arr, "
                + "vm_disk AS disk "
                + "WHERE disk.vm_disk_id = arr.vm_disk_id "
                + "AND arr.vm_id = mesu.vm_id "
                + "AND disk.disk_name = ? "
                + "GROUP BY disk.disk_name", diskRefStr);
    }

    /**
     * This returns the overall average CPU utilisation for a given tag or disk.
     *
     * @param queryItem The application tag or disk reference to get the cpu
     * usage for.
     * @return The cpu utilisation trace data.
     */
    private List<VmLoadHistoryWeekRecord> getAverageCPUUtilisationWeekTrace(String query, String queryItem) {
        List<VmLoadHistoryWeekRecord> answer = new ArrayList<>();
        connection = getConnection(connection);
        if (connection == null) {
            return answer;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                query)) {
            preparedStatement.setString(1, queryItem);
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> avgRecord : results) {
                answer.add(new VmLoadHistoryWeekRecord(
                        (int) avgRecord.get(2), //week day 3rd item
                        (int) avgRecord.get(3), //day of week 4th item
                        (double) avgRecord.get(0), // avg load is 1st item
                        (double) avgRecord.get(1))); //standardDev is 2nd item
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This returns the overall average CPU utilisation for a given tag.
     *
     * @param tagName The application tag to get the cpu usage for.
     * @return The cpu utilisation trace data.
     */
    @Override
    public List<VmLoadHistoryWeekRecord> getAverageCPUUtilisationWeekTraceForTag(String tagName) {
        return getAverageCPUUtilisationWeekTrace("SELECT avg(cpu_load), "
                + "STDDEV_POP(cpu_load) as standardDev, "
                + "Weekday(FROM_UNIXTIME(clock)) as Day_of_Week, "
                + "Hour(FROM_UNIXTIME(clock)) as Hour_in_Day "
                + "FROM vm_measurement as mesu, "
                + "vm_app_tag_arr AS arr, "
                + "vm_app_tag AS tag "
                + "WHERE tag.vm_app_tag_id = arr.vm_app_tag_id AND "
                + "arr.vm_id = mesu.vm_id AND "
                + "tag.tag_name = ? "
                + "GROUP BY Day_of_Week, Hour_in_Day", tagName);
    }

    /**
     * This returns the overall average CPU utilisation for a given tag.
     *
     * @param diskRef The disk reference to get the cpu usage for.
     * @return The cpu utilisation trace data.
     */
    @Override
    public List<VmLoadHistoryWeekRecord> getAverageCPUUtilisationWeekTraceForDisk(String diskRef) {
        return getAverageCPUUtilisationWeekTrace("SELECT avg(cpu_load), "
                + "STDDEV_POP(cpu_load) as standardDev, "
                + "Weekday(FROM_UNIXTIME(clock)) as Day_of_Week, "
                + "Hour(FROM_UNIXTIME(clock)) as Hour_in_Day "
                + "FROM vm_measurement as mesu, "
                + "vm_disk_arr AS arr, "
                + "vm_disk AS disk "
                + "WHERE disk.vm_disk_id = arr.vm_disk_id AND "
                + "arr.vm_id = mesu.vm_id AND "
                + "disk.disk_name = ? "
                + "GROUP BY Day_of_Week, Hour_in_Day", diskRef);
    }

    /**
     * This returns for a given VM the current index value for discrete time of
     * a a set size.
     *
     * @param vm The VM to get the current index for
     * @param windowSize The size of the time window to be used, in seconds.
     * @return The current index value.
     */
    @Override
    public double getVMCurrentBootTraceIndex(VmDeployed vm, int windowSize) {
        connection = getConnection(connection);
        if (connection == null || vm == null) {
            return Double.NaN;
        }
        try (Statement statement = connection.createStatement();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT floor((max(vm_measurement.clock) - "
                        + "min(vm_measurement.clock)) / ?) "
                        + "as slot FROM vm_measurement "
                        + "WHERE vm_id = ? GROUP BY vm_id")) {
            statement.executeUpdate("set @row_number =0");
            statement.executeUpdate("set @vm_id =0");
            preparedStatement.setInt(2, vm.getId());
            preparedStatement.setInt(1, windowSize);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return resultSet.getDouble(1); //return the single value from the query
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Double.NaN;
    }

    /**
     * This returns the boot time trace for the average CPU utilisation for
     * either a given tag, or disk reference for VMs since boot. It is reported
     * in a set of discrete time intervals as specified by a window size
     * parameter.
     *
     * @param query The query to run, either for application tags or disks.
     * @param queryItem The item to query.
     * @param windowSize The size of the time window to be used, in seconds.
     * @return the boot time trace for the average CPU utilisation
     */
    private List<VmLoadHistoryBootRecord> getAverageCPUUtilisationBootTrace(String query, String queryItem, int windowSize) {
        List<VmLoadHistoryBootRecord> answer = new ArrayList<>();
        connection = getConnection(connection);
        if (connection == null) {
            return answer;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                query)) {
            preparedStatement.setString(1, queryItem);
            preparedStatement.setInt(2, windowSize);
            ResultSet resultSet = preparedStatement.executeQuery();
            ArrayList<ArrayList<Object>> results = resultSetToArray(resultSet);
            for (ArrayList<Object> avgRecord : results) {
                answer.add(new VmLoadHistoryBootRecord(
                        ((Long) avgRecord.get(0)).intValue(), //boot index
                        (double) avgRecord.get(1), // avg load is 2nd item
                        (double) avgRecord.get(2))); //Std Dev is 3rd item
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    /**
     * This returns the boot time trace for the average CPU utilisation for a
     * given application tag for VMs since boot. It is reported in a set of
     * discrete time intervals as specified by a window size parameter.
     *
     * @param tagName The application tag to get the cpu usage for.
     * @param windowSize The time in seconds to group each discrete time block
     * by.
     * @return The cpu utilisation trace data.
     */
    @Override
    public List<VmLoadHistoryBootRecord> getAverageCPUUtilisationBootTraceForTag(String tagName, int windowSize) {

        return getAverageCPUUtilisationBootTrace(
            "SELECT (vm_data.clock - start_time) as start_clock, avg(vm_data.cpu_load) as cpu_load, STDDEV_POP(cpu_load) as standard_deviation " + 
            "FROM vm_measurement as vm_data, " + 
                "(SELECT valid_vm.vm_id as valid_vm, min(valid_vm.clock) as start_time " + 
                "FROM vm_measurement as valid_vm, vm_app_tag_arr AS arr, vm_app_tag AS tag " +
                "WHERE tag.vm_app_tag_id = arr.vm_app_tag_id " + 
                "AND arr.vm_id = valid_vm.vm_id AND tag.tag_name = ? " + 
                "GROUP BY valid_vm.vm_id) as valid_vms " + 
                "WHERE vm_data.vm_id = valid_vm " +
                "GROUP BY start_clock DIV ?", 
                tagName, windowSize);
    }

    /**
     * This returns the boot time trace for the average CPU utilisation for a
     * disk reference for VMs since boot. It is reported in a set of discrete
     * time intervals as specified by a window size parameter.
     *
     * @param diskName The application tag to get the cpu usage for.
     * @param windowSize The size of the time window to be used, in seconds.
     * @return The cpu utilisation trace data.
     */
    @Override
    public List<VmLoadHistoryBootRecord> getAverageCPUUtilisationBootTraceForDisk(String diskName, int windowSize) {
        return getAverageCPUUtilisationBootTrace(
            "SELECT (vm_data.clock - start_time) as start_clock, avg(vm_data.cpu_load) as cpu_load, STDDEV_POP(cpu_load) as standard_deviation " + 
            "FROM vm_measurement as vm_data, " + 
                "(SELECT valid_vm.vm_id  as valid_vm, min(valid_vm.clock) as start_time " + 
                "FROM vm_measurement as valid_vm, vm_disk_arr AS arr, vm_disk AS disk " +
                "WHERE disk.vm_disk_id = arr.vm_disk_id " + 
                "AND arr.vm_id = valid_vm.vm_id AND disk.disk_name = ? " + 
                "GROUP BY valid_vm.vm_id) as valid_vms " + 
                "WHERE vm_data.vm_id = valid_vm " +
                "GROUP BY start_clock DIV ?", diskName, windowSize);
    }
    
    /**
     * This tests to see if the database connection is still live or not
     * @return If the database connection is live or not
     */
    @Override
    public boolean isConnectionValid() {
        try {
            if (connection != null) {
                return connection.isValid(20);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, "The connection was invalid.", ex);
        }
        return false;
    }

    /**
     * This closes the database connection. It will be reopened if a query is
     * called.
     */
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DefaultDatabaseConnector.class.getName()).log(Level.SEVERE, "The connection close operation failed.", ex);
        }
    }
}
