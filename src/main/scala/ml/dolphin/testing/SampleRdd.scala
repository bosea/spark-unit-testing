package ml.dolphin.testing

import org.apache.spark.rdd.RDD

/**
 * Some basic operations defined as an introduction to spark-testing-base library
 */

object SampleRdd {

  def tokenize(aL: RDD[String]) = {
    aL.map(x => x.split(' ')).collect()
  }
}