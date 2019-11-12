#!/bin/bash
source ~/.bashrc

service ssh start

cd /tmp/classifier

start-all.sh
hdfs dfsadmin -safemode leave

java -jar target/mahout-0.0.1-SNAPSHOT.jar