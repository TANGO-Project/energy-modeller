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


#### Build status from Travis-CI

[![Build Status](https://travis-ci.org/TANGO-Project/energy-modeller.svg?branch=master)](https://travis-ci.org/TANGO-Project/energy-modeller)

#### Sonar Cloud reports:
The Sonar Cloud reports for this project are available at: https://sonarcloud.io/dashboard?id=eu.tango%3Aenergy-modeller

### Installation for running the service

In this case, we are going to detail how to run the application in its standalone mode that is intended to be used for gathering data for the energy models. The second mode of operation the energy modeller is used as a subcomponent of another such as the self-adaptation manager, with the intent of being used as a modeller.

#### Configuring the service

TODO

## Usage Guide

TODO

## Relation to other TANGO components

The energy modeller works 

* **Device Supervisor** - The Energy modeller can directly interface with the device supervisor as a means of using it as a datasource for monitoring the environment.
* **Monitoring Infrastructure** - The Energy modeller can interface with the monitoring infrastructure as a means of using it as a datasource for monitoring the environment.
* **Self-Adaptation Manager** - The energy modeller provides guidance to the self-adaptation manager on how much power is being consumed by an application. It also allows for it to determine the effect on power consumption of proposed changes to the applications configuration.
