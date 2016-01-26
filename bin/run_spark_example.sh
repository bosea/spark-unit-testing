#!/usr/bin/env bash
#
# run an example Spark job
#
# History:
# 09/25/2014 abose "created"

. $HOME/.bash_profile

echo 'Running example task of estimating the value of pi using ***SPARK***'

VAR1=`hostname`
${SPARK_HOME}/bin/spark-submit --class org.apache.spark.examples.SparkPi --deploy-mode client --master spark://${VAR1}:7077 ${SPARK_HOME}/lib/spark-examples-1.2.0.2.2.0.0-82-hadoop2.6.0.2.2.0.0-2041.jar 10
