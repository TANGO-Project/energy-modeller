#!/bin/bash

#Note: This script works as part of collectd's exec functionality. This script is run and it scrapes from the output of the energy modeller.

 INTERVAL="${COLLECTD_INTERVAL:-20}"
 LOGFILE="/home_nfs/home_kavanagr/energymodeller/energy-modeller/logs/energy-modeller-app-output.log"

 while sleep "$INTERVAL"; do
   APP_COUNT="${squeue -h | wc -l}"
   tail -n $APP_COUNT $LOGFILE | awk '{split($0,values," "); printf "PUTVAL \""; printf values[1] ".bullx"; printf "/"; printf values[2]; printf "/"; printf values[2]; printf "\" interval=20 N:"; print values[3]}'
 done
