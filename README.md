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
}
```

1. Define the instances of Vectors x and y that will be used in the tests.
2. Initialize x and y before each test
3. Write the actual test. It uses "assert" to enforce whether the expected value (right hand side) 
   is same as the value returned by "distance" method in EuclideanVectorSpace. Note the "===" 
   (three = signs) in the assert statement.  


  defined with its name and a body. A typical of many tests is the assert statement. We will see 
  an example of that later. Finally, an "after" section is defined for all post-test operations. 
  Again, it is executed each time following a test. So, the code block looks like:




# References

Spark testing:
https://spark-summit.org/2014/wp-content/uploads/2014/06/Testing-Spark-Best-Practices-Anupama-Shetty-Neil-Marshall.pdf

Scala Test
http://www.scalatest.org/user_guide/using_assertions

Spark Testing Base
http://blog.cloudera.com/blog/2015/09/making-apache-spark-testing-easy-with-spark-testing-base/