package ml.dolphin.testing

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest.FunSuite

/**
 * Example usage of spark-testing-base library written by Holden Karau
 *
 * @author Abhijit Bose
 * @version 1.0 11/24/2015
 * @since 1.0 11/24/2015
 */

class SampleRddTest extends FunSuite with SharedSparkContext {

  test("Testing RDD transformations using a shared Spark Context") {
    val input = List("Testing", "RDD transformations", "using a shared", "Spark Context")
    val expected = Array(Array("Testing"), Array("RDD", "transformations"), Array("using", "a", "shared"),
                         Array("Spark", "Context"))
    val transformed = SampleRdd.tokenize(sc.parallelize(input))
    assert(transformed === expected)
  }

}
