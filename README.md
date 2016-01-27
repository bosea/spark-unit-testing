# Testing in Apache Spark - A Tutorial

A tutorial on how to write unit tests and do performance testing of Apache Spark code in
 Scala. 

My New Year's resolution: write more tests! May be, this is the year when I finally move over to 
TDD (Test Driven Development) i.e. start any new work by writing tests first! Writing tests is a 
very good idea :) when you plan to use your code for making real-world decisions, e.g. which 
ads to show to which user, or extend how much credit to which customers, etc.  We have 
been using Spark and MLlib increasingly for these types of problems at work. Unfortunately, 
pointers on best practices of testing Spark code are few and scattered, so I wrote this tutorial 
to have a single place for all Spark and MLlib related testing, show example code and relevant 
URLs. I will continue to add to it as I find new material.   

## Example: Vector Operations in Euclidean Space

As an example project, I chose to write some of the basic vector operations in Euclidean space, 
such as distance and cosine between two vectors, centroid of an array of vectors, etc. With these 
basic operations defined, I then wrote a method that takes an RDD of vectors and a centroid as 
input, and calculates the sum of distances between each vector and the centroid. You will 
recognize this as very similar to the convergence criteria calculation step in k-means clustering 
and the cosine operation as the similarity calculation step in a typical collaborative filtering 
algorithm.  We will be writing unit tests and performance tests for these in this tutorial.

The basic vector operations are defined in:
**src/main/scala/ml/dolphin/testing/EuclideanVectorSpace.scala**

Nothing special here. It defines two 2-vector operations: distance and cosine, and two N-vector 
operations: centroid and closest. Notice that these are not defined over RDDs. Rather, we will be
 using these within a partition of the RDD, e.g. using mapPartitions.
 
Next, let's look at: 
**src/main/scala/ml/dolphin/testing/DistanceFromCentroid.scala**

It has only one method: calcDistance(sc: SparkContext, vPoints: RDD[Vector], centroid: Vector)

```
  def calcDistance(sc: SparkContext, vPoints: RDD[Vector], centroid: Vector): Double = {

    // 1. Broadcast centroid to all partitions
    val bcCentroid = sc.broadcast(centroid)

    // 2. For each partition, calculate the sum of distances from centroid to each of 
    // the points in that partition. Then, sum up the partial sums from all the partitions.

    val accmDistance = vPoints.mapPartitions{ points => {
      var sum = 0.0
      points.foreach { point => {
        sum += EuclideanVectorSpace.distance(point, bcCentroid.value)
      }}
      Iterator(sum)
    }}.reduce(_ + _) // 3. Sum up all the partial sums from the partitions
    accmDistance
  }
```

1. Broadcast the centroid to all partitions of the input RDD (i.e. vPoints) so that we can
   calculate the distance from this centroid to all the vectors within each partition **locally**.
2. Inside mapPartitions, we calculate all the distances using EuclideanVectorSpace.distance() and 
   generate a local "sum". Note that we wrap the local "sum" with an Iterator from each partition
   since mapPartitions returns an Iterator.
3. Finally, we sum up all the local contributions using a "reduce". 

We will now write some unit tests for these. Reminder to self: next time, start with the tests!

## Apache Spark Unit Testing

There is no native library for unit testing in Spark as of yet. After researching this topic 
for a while, I feel that the best option is to use two libraries:
 
 - [ScalaTest](http://www.scalatest.org/)
 - [Spark-Testing-Base](https://github.com/holdenk/spark-testing-base)
 
A little bit about **ScalaTest**. For Scala users, this is the most familiar unit testing 
framework (you can also use it for testing Java code and soon for JavaScript). 

it supports a number of different testing styles, each designed to support a specific type of 
testing need. For details, see  [ScalaTest User Guide](http://www.scalatest.org/user_guide/selecting_a_style).

Although ScalaTest supports many styles, I find that the quickest way to get started is to use 
the following ScalaTest traits and write the tests in the [TDD style (Test Driven Development)]
(https://en.wikipedia.org/wiki/Test-driven_development): 

 1. [*FunSuite*](http://doc.scalatest.org/1.8/org/scalatest/FunSuite.html) 
 2. [*Assertions*](http://www.scalatest.org/user_guide/using_assertions) 
 3. [*BeforeAndAfter*](http://doc.scalatest.org/1.8/org/scalatest/BeforeAndAfter.html)

Feel free to browse the above URLs to learn more about these traits - that will make rest of this 
tutorial go smoothly.

FunSuite tests are function values and each test is specified with a strong denoting its name. 
Let's go over the syntax of a basic test:
**src/test/scala/ml/dolphin/testing/EuclideanVectorSpaceTests.scala**

Notice that there are multiple tests defined in this file (corresponding to the methods defined in 
EuclideanVectorSpace.scala and described earlier). We define a "test fixture" consisting of the 
objects and other artifacts such as files, database connections, connections to services, etc 
that the test will utilize to do its work. If you have multiple tests in a test suite that share 
some of the same test fixtures and you need to set them up to the same values before each test 
begins or you need to clean them up after each test finishes, it is best to use BeforeAndAfter 
trait. If an exception occurs during before or after method executes, all tests in the suite are 
abandoned. Our class "EuclideanVectorSpaceTests" defines 4 tests in the suite and two of them 
use the same fixtures: x and y. We initialize them to the same values every time in the method 
"before": 

```
class EuclideanVectorSpaceTests extends FunSuite with BeforeAndAfter {

  // 1. Define the instance variables
  var x: linalg.Vector = _
  var y: linalg.Vector = _

  // 2. Set the values at the beginning of each test
  before {
    x = Vectors.dense(1.0, 2.0, 3.0, 4.0)
    y = Vectors.dense(2.0, 3.0, 4.0, 5.0)
  }

  // 3. Write the actual test
  test("L2 distance between 2 Vector's") {
      assert(EuclideanVectorSpace.distance(x, y) === 2.0)
  }
  
  test("Cosine between 2 Vector's") {
    // expected value = 40.0 / (sqrt(54) * sqrt(30))
    assert(EuclideanVectorSpace.cosine(x, y) === 40.0 / (sqrt(54) * sqrt(30)))
  }

  test("Vectors of 0's will have a zero distance") {
    assertResult(0.0) {
      EuclideanVectorSpace.distance(Vectors.dense(0.0, 0.0, 0.0), Vectors.dense(0.0, 0.0, 0.0))
    }
  }
  
  // 4. Use "pending" to write tests later.
  test ("Centroid of a set of vectors") (pending)

}
```

1. Define the instances of Vectors x and y that will be used in the tests.
2. Initialize x and y before each test
3. Write the actual test. It uses "assert" to enforce whether the expected value (right hand side) 
   is same as the value returned by "distance" method in EuclideanVectorSpace. Note the "===" 
   (three "=" signs) in the assert statement. This syntax provides more detailed error messages.
   I prefer to use this for all my tests.
4. Note the "pending" next to the last test with an empty body. This tells ScalaTest (and reminds us)
   that we will be writing future tests. For example, we have the method centroid defined in
   EuclideanVectorSpace but we are not testing it yet. (It is always a good idea to write all the tests
   in one go though...I may not come back to this again...)

### Making life easier with Spark-Testing-Base

As I mentioned earlier, after trying a few different things, I find spark-testing-base to be the 
easiest and most functional unit testing framework for Spark so far. It surfaces some of the same test suites that the Spark
committers use when testing internal Spark code. What you get out of the box:

1. There are often multiple tests in a test suite, and we use "before" and "after" to set up and reset the 
   test artifacts. That is fine for regular Scala code. When you are trying to do this for Spark code, each test needs to 
   set up and stop a [SparkContext](http://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.SparkContext),
   leading to a lot of boilerplate code replication. Also, in between SparkContext stops and starts, one has to clear
   spark.driver.port variable. When you use spart-testing-base, this is automatically taken care of so you can focus on
   writing the test iteself (i.e. the stuff in the "test(<name>) {}"). If you are writing lots of tests, this makes a 
   big difference. More specifically, spark-testing-base allows you to share the same SparkContext for all the tests defined 
   in the same test suite. We dig deeper into this below.
2. If you are testing Spark Streaming or Spark DataFrames (i.e. not just basic RDD methods), there are other things to 
   worry about, e.g. how long you have to wait for the test to finish. I will be adding more examples on these, especially
   DataFrames since there is not much info on testing currently. For now, you can read [**this excellent article**](http://blog.cloudera.com/blog/2015/09/making-apache-spark-testing-easy-with-spark-testing-base/) by creator
   of spark-testing-base (Holden Karau).

Now let's look at a concrete example. 
**src/test/scala/ml/dolphin/testing/DistanceFromCentroidTests.scala**

```
class DistanceFromCentroidTests extends FunSuite with BeforeAndAfter with SharedSparkContext {

  var vPoints: Array[Vector] = _
  var centroid: Vector = _
  var vPointsRdd: RDD[Vector] = _

  // 1. Instantiate the variables for each test
  before {
    vPoints = Array(Vectors.dense(1.0, 2.0, 3.0, 4.0), Vectors.dense(2.0, 3.0, 4.0, 5.0),
    Vectors.dense(3.0, 9.0, 1.0, 7.0), Vectors.dense(1.0, 5.0, 6.0, 8.0))
    centroid = Vectors.dense(1.0, 1.0, 1.0, 1.0)
    vPointsRdd = sc.parallelize(vPoints, 3)
  }
  
  // 2. an actual test
  test("Testing calcDistance using a shared Spark Context") {
    val sum = DistanceFromCentroid.calcDistance(sc, vPointsRdd, centroid)
    val expected = sqrt(14.0) + sqrt(30.0) + sqrt(104.0) + sqrt(90.0)
    assert(sum === expected)
  }
}
```

1. Note something special here. We are using a SparkContext ("sc") without instantiating it anywhere in this class:
   ```
   vPointsRdd = sc.parallelize(vPoints, 3) 
   ```
   This is done for you within spark-testing-base when you extend your class definition with "SharedSparkContext" trait 
   (you need to import "com.holdenkarau.spark.testing.SharedSparkContext" in the file where you define your test suite). 
   
   To see how it's handled, take a look at the internal of spark-testing-base, specifically [**SharedSparkContext.scala**](https://github.com/holdenk/spark-testing-base/blob/ef199dc9e93cc80376d7289f7504824d0ffa0870/src/main/1.3/scala/com/holdenkarau/spark/testing/SharedSparkContext.scala)
   
   ```
   
   /** Shares a local `SparkContext` between all tests in a suite and closes it at the end. */
   trait SharedSparkContext extends BeforeAndAfterAll with SparkContextProvider {
     self: Suite =>

     @transient private var _sc: SparkContext = _
     
     // 1.1. SparkContext definition
     override def sc: SparkContext = _sc

     val appID = new Date().toString + math.floor(math.random * 10E4).toLong.toString

     override val conf = new SparkConf().
       setMaster("local[*]").
       setAppName("test").
       set("spark.ui.enabled", "false").
       set("spark.app.id", appID)

     // 1.2. Instantiate new SparkContext and set logging level
     override def beforeAll() {
       _sc = new SparkContext(conf)
       _sc.setLogLevel(org.apache.log4j.Level.WARN.toString)
       super.beforeAll()
     }
     
     // 1.3. stop SparkContext
     override def afterAll() {
       try {
         LocalSparkContext.stop(_sc)
         _sc = null
       } finally {
         super.afterAll()
       }
     }
   }
   ```
 
  - 1.1 sc is the SparkContext that you will use for your tests within the same suite. Internally, the trait uses a private 
      variable "_sc" to manage the actual Spark Context so that you cannot (accidentally) modify it. 
  - 1.2 "_sc" is instantiated within a "beforeAll()" method. The difference between "before()" that you have seen before and
      "beforeAll()" is that the latter is executed before executing the suite (difference here before the suite vs before a test in 
       the suite).  You can also have nested suites - slightly more advanced but a very handy approach when testing a large platform.
       You can read more about beforeAll and afterAll here: http://doc.scalatest.org/1.0/org/scalatest/BeforeAndAfterAll.html
  - 1.3 Similarly, Spark Context is stopped at the end of the test suite via a call to afterAll().
 
2. Coming back to our test suite: DistanceFromCentroidTests, let's look at the actual test:
    ```
    // 2. an actual test
    test("Testing calcDistance using a shared Spark Context") {
      val sum = DistanceFromCentroid.calcDistance(sc, vPointsRdd, centroid)
      val expected = sqrt(14.0) + sqrt(30.0) + sqrt(104.0) + sqrt(90.0)
      assert(sum === expected)
    }
    ```
   This is very similar to regular ScalaTest syntax. The only difference is that we are invoking a test on a RDD using the local
   shared SparkContext supplied to us by spark-testing-base. We are testing DistanceFromCentroid.calcDistance(...) method that
   takes a RDD of points, a centroid and calculates the sum of inidividual distances from the centroid. Notice how we expressed
   the expected value. Since there were 4 points defined in vPointsRdd, we put in sqrt(each point's distance from centroid) for
   each point. This is just an individual choice - it helps with code readability by another person as to how the expected value is
   calculated.
 
That's it. Happy testing!

## Spark Performance Testing with spark-perf
Coming

 
# References

Spark testing:
https://spark-summit.org/2014/wp-content/uploads/2014/06/Testing-Spark-Best-Practices-Anupama-Shetty-Neil-Marshall.pdf

Scala Test
http://www.scalatest.org/user_guide/using_assertions

Spark Testing Base
http://blog.cloudera.com/blog/2015/09/making-apache-spark-testing-easy-with-spark-testing-base/
