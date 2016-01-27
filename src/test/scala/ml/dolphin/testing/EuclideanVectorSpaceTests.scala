package ml.dolphin.testing

import breeze.numerics.sqrt
import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.Vectors
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
 * Tests for EuclideanVectorSpace methods
 *
 * @author Abhijit Bose
 * @version 1.0 06/24/2015
 * @since 1.0 06/24/2015
 */

class EuclideanVectorSpaceTests extends FunSuite with BeforeAndAfter {

  var x: linalg.Vector = _
  var y: linalg.Vector = _

  before {
    x = Vectors.dense(1.0, 2.0, 3.0, 4.0)
    y = Vectors.dense(2.0, 3.0, 4.0, 5.0)
  }

  test("L2 distance between 2 Vector's") {
      assert(EuclideanVectorSpace.distance(x, y) === 2.0)
  }

  test("Cosine between 2 Vector's")   {
    // expected value = 40.0 / (sqrt(54) * sqrt(30))
    assert(EuclideanVectorSpace.cosine(x, y) === 40.0 / (sqrt(54) * sqrt(30)))
  }

  test("Vectors of 0's will have a zero distance") {
    assertResult(0.0) {
      EuclideanVectorSpace.distance(Vectors.dense(0.0, 0.0, 0.0), Vectors.dense(0.0, 0.0, 0.0))
    }
  }

  test ("Centroid of a set of vectors") (pending)

}
