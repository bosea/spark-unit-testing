package ml.dolphin.testing

import com.holdenkarau.spark.testing.SharedSparkContext
import breeze.numerics.sqrt
import org.apache.spark.rdd.RDD
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.apache.spark.mllib.linalg.{Vector, Vectors}


/**
 * Unit tests for ml.dolphin.testing.DistanceFromCentroid methods
 *
 * @author Abhijit Bose
 * @version 1.0 11/24/2015
 * @since 1.0 11/24/2015
 */

class DistanceFromCentroidTests extends FunSuite with BeforeAndAfter with  SharedSparkContext {

  var vPoints: Array[Vector] = _
  var centroid: Vector = _
  var vPointsRdd: RDD[Vector] = _

  before {
    vPoints = Array(Vectors.dense(1.0, 2.0, 3.0, 4.0), Vectors.dense(2.0, 3.0, 4.0, 5.0),
                        Vectors.dense(3.0, 9.0, 1.0, 7.0), Vectors.dense(1.0, 5.0, 6.0, 8.0))
    centroid = Vectors.dense(1.0, 1.0, 1.0, 1.0)
    vPointsRdd = sc.parallelize(vPoints, 3)
  }

  test("Testing calcDistance using a shared Spark Context") {
    val sum = DistanceFromCentroid.calcDistance(sc, vPointsRdd, centroid)
    val expected = sqrt(14.0) + sqrt(30.0) + sqrt(104.0) + sqrt(90.0)
    assert(sum === expected)
  }

}
