#!/bin/bash


 INTERVAL="${COLLECTD_INTERVAL:-20}"
 LOGFILE="/home_nfs/home_kavanagr/energymodeller/energy-modeller/energy-modeller-app-output.log"

 while sleep "$INTERVAL"; do
   tail -n $APP_COUNT $LOGFILE | awk '{split($0,values," "); printf "PUTVAL \""; printf values[1] ".bullx"; printf "/"; printf values[2]; printf "/"; printf values[2]; printf "\" interval=20 N:"; print values[3]}'
 done
