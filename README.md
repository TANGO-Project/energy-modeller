# Tango Energy Modeller

&copy; University of Leeds 2016

Tango Energy Modeller (EM) is a component of the European Project TANGO (http://tango-project.eu ).

EM is distributed under a [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Description

The Energy Modeller is responsible for reporting current and historic energy usage of applications. It features the ability to forecast power and energy consumption of applications in the future with the intent of providing information that guides the adaptive behaviour of the Tango architecture.

## Installation Guide

This guide it is divided into two different guides, one specific to compilation of the Energy Modeller and the second on how to run and configure the Energy Modeller.

### Compilation

#### Requirements

The Energy Modeller's primary two prerequisites are:

* Java
* Maven

#### Installation and configuration procedure

To compile the energy modeller, the following steps must be performed:
1.	Generate the Energy Modeller jar using the command: mvn clean package (executed  in the Energy Modeller directory)
2.	Install the database. SQL statements to setup the database are held in the file “energy modeller db.sql” file it is held in the {energy-modeller root directory}\src\main\resources.

#### Build status from Travis-CI

[![Build Status](https://travis-ci.org/TANGO-Project/energy-modeller.svg?branch=master)](https://travis-ci.org/TANGO-Project/energy-modeller)

#### Sonar Cloud reports:
The Sonar Cloud reports for this project are available at: https://sonarcloud.io/dashboard?id=eu.tango%3Aenergy-modeller

### Installation for running the service

In this case, we are going to detail how to run the application in its standalone mode that is intended to be used for gathering data for the energy models. The second mode of operation the energy modeller is used as a subcomponent of another such as the self-adaptation manager, with the intent of being used as a modeller.

## Usage Guide

The energy modeller is also highly configurable and has several files that may be used to change its behaviour. The energy modeller has the following settings files in order to achieve these changes:

*energy-modeller.properties:* holds basic configuration specifying which data source and predictor to use.  
*energy-modeller-db.properties:* Holds database information for the energy modeller.  
*energy-modeller-predictor.properties:* Holds settings relating to the prediction of energy usage.  
*energy-modeller-influx-db-config.properties:* Holds settings on how to connect to ConnectD's influxdb database directly, in the event the CollectDInfluxDbDatasoruceAdaptor, TangoRemoteProcessingDataSourceAdaptor, TangoEnvironmentDataSourceAdaptor are in use. The TangoEnvironmentDataSourceAdaptor is the default datasource in use.  
*energy-modeller-db-zabbix.properties:* Holds information on how to connect to the Zabbix database directly, in the event the ZabbixDirectDbDataSourceAdaptor is in use.  

These settings must be tailored to the specific infrastructure. The settings are described below and an example of the settings is provided for reference.

#### energy-modeller-db.properties

This file specifies various database related settings for the energy modeller. An example is provided below:

```
energy.modeller.db.url = jdbc:mysql://iaas-vm-dev:3306/ascetic-em
energy.modeller.db.driver = org.mariadb.jdbc.Driver
energy.modeller.db.password = XXXXX
energy.modeller.db.user = user-em
```

This includes specifying the database username and password for the energy modeller to connect to its background database. This includes information such as the connection URL, the driver to use and the username and password to use.
The SQL script to setup the database structure is held in the file IaaS energy modeller db.sql. It is held under the directory {energy-modeller root directory}\src\main\resources.

#### energy-modeller.properties

```
energy.modeller.datasource = SlurmDataSourceAdaptor
energy.modeller.predictor = CpuAndAcceleratorEnergyPredictor
```

The data source parameter indicates how the energy modeller's will gain the environment data that it needs. It can be one of the following options:

*CollectDInfluxDbDataSourceAdaptor:* This connector that directly accesses collectd's influxdb database for the information that it requires. This adaptor utilises the configuration file energy-modeller-influx-db-config.properties.  
*SlurmDataSourceAdaptor:* This is an adaptor that connects the energy modeller into a SLURM job management based environment. Allowing access to information about the physical host.  
*TangoEnvironmentDataSourceAdaptor:* This makes use of both the SlurmDataSourceAdaptor and the CollectDInfluxDbDataSourceAdaptor.  
*TangoRemoteProcessingDataSourceAdaptor:* This makes use of the CollectDInfluxDbDataSourceAdaptor and additionally connects into the compss runtime environment, to gain further information about running jobs.
*ZabbixDirectDbDataSourceAdaptor:* This connector that directly accesses the Zabbix database for the information that it requires. This adaptor utilises the configuration file energy-modeller-db-zabbix.properties.   
*WattsUpMeterDataSourceAdaptor:* for local usage of the energy modeller.

It should be noted that the observation window should not be too small, especially during the usage of the Zabbix data source adaptors, which may provide fewer data points than the WattsUpMeterDataSourceAdaptor, the latter been able to report at an interval as low as every second. 

The predictor that is in use can be specified above, the options are:

* CpuAndAcceleratorEnergyPredictor - this is the default option, it is designed for use with accelerators
* CpuAndBiModalAcceleratorEnergyPredictor - this predictor assumes accelerators are either busy or idle and makes estimates based upon the calibration data that is available. 
* CpuOnlyBestFitEenrgyPredictor - This selects between predictors that only consider CPU load as a factor
* CpuOnlyEnergyPredictor - This uses a linear model
* CpuOnlyPolynomialEnergyPredictor - This uses a polynomial function of order 2
* CpuOnlySplinePolynomialEnergyPredictor - This uses spline points with a polynomial function

#### energy-modeller-predictor.properties

This file specifies settings for the energy predictor mechanism, an example of such a file is provided below:

```
energy.modeller.energy.predictor.datasource = ZabbixDirectDbDataSourceAdaptor
energy.modeller.energy.predictor.workload.predictor = CpuRecentHistoryWorkloadPredictor
energy.modeller.energy.predictor.default_load = -1.0
energy.modeller.energy.predictor.cpu.utilisation.observe_time.min = 0
energy.modeller.energy.predictor.cpu.utilisation.observe_time.sec = 15
```
The data source parameter indicates how the energy modeller's predictor will gain the environment data that it needs. It can be one of the options, specified previously. 

The energy predictor can utilise several different workload estimator functions. The default is to use the CpuRecentHistoryWorkloadPredictor. This has the following configuration settings.

The default_load parameter indicates what load the predictor should use as an estimate. It should be specified in the range 0..1. An alternative is to provide it the value -1, in which it will default to using the observed current load.

In the case where the observer current load is being used the observe_time.min and observe_time.sec parameters are used to indicate the size of the observation window for CPU utilisation. The two values are simply added together to make the total observation window time. The default observation window size is 15 minutes. 

The other options for workload prediction which can be used when the energy modeller is configured for virtual machines. These are:
* BasicAverageCpuWorkloadPredictor
* BasicAverageCpuWorkloadPredictorDisk
* BootAverageCpuWorkloadPredictor
* BootAverageCpuWorkloadPredictorDisk
* DoWAverageCpuWorkloadPredictor
* DoWAverageCpuWorkloadPredictorDisk

These predictors work on historical load information. Each VM can be tagged with information basic information about the application the VM is for and the disk image it is based upon. 
Average CPU Workload predictors: give an estimate of the workload based upon the average CPU utilisation for a given application tag or base disk image.
Average Boot Workload predictors: give an estimate of the workload based upon the time from boot of a VM for a given application tag or base disk image.
Day of Week (DoW) Workload predictors: give an estimate of the workload based upon the time and day of the week that a VM is active for a given application tag or base disk image.

#### energy-modeller-influx-db-config.properties:

This configuration file is used to configure the energy modeller when using the CollectDInfluxDbDataSourceAdaptor, TangoRemoteProcessingDataSourceAdaptor or TangoEnvironmentDataSourceAdaptor adaptors. It holds the database connection settings used to connect directly to the collectd influxdb database.

```
energy.modeller.influx.db.hostname = http://ns54.bullx:8086
energy.modeller.influx.db.name = collectd
energy.modeller.influx.db.user = collectd
energy.modeller.influx.db.password = XXXXX

```

This includes specifying the host connection url and database name along with connection details such as the username and password.

#### energy-modeller-db-zabbix.properties

This is the configuration file used to configure the energy modeller when using the ZabbixDirectDBDataSourceAdaptor. It holds the database connection settings used to connect directly to the Zabbix database.

```
energy.modeller.zabbix.db.driver = org.mariadb.jdbc.Driver
energy.modeller.zabbix.db.url = jdbc:mysql://192.168.3.199:3306/zabbix
energy.modeller.zabbix.db.user = zabbix
energy.modeller.zabbix.db.password = XXXXX
energy.modeller.host.group = Hypervisors
energy.modeller.vm.group = Virtual Machines
energy.modeller.dfs.group = DFS
energy.modeller.only.available.hosts = false
```

This includes specifying the database username and password for the energy modeller to connect to directly with the Zabbix database. This also includes information such as the connection URL, the driver to use, the username and password to use.

## Relation to other TANGO components

The energy modeller works with the following components:

* **Device Supervisor** - The Energy modeller can directly interface with the device supervisor as a means of using it as a datasource for monitoring the environment.
* **Monitoring Infrastructure** - The Energy modeller can interface with the monitoring infrastructure as a means of using it as a datasource for monitoring the environment.
* **Self-Adaptation Manager** - The energy modeller provides guidance to the self-adaptation manager on how much power is being consumed by an application. It also allows for it to determine the effect on power consumption of proposed changes to the applications configuration.
