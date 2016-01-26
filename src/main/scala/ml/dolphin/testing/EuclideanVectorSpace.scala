package ml.dolphin.testing

/**
 * Euclidean Vector Space extended from VectorSpace.
 * @note Methods have no side effects
 *
 * @author Abhijit Bose
 * @version 1.0 06/24/2015
 * @since 1.0 06/24/2015
 */

import breeze.numerics.sqrt
import org.apache.spark.mllib.linalg.{Vector, Vectors}

import scala.math.pow

object EuclideanVectorSpace extends VectorSpace[Vector] {

  /**
   * Euclidean Distance between two vectors x and y
   *
   * @param x Input Vector x
   * @param y Input Vector y
   * @return Double
   */
  override def distance(x: Vector, y: Vector): Double = {
    val dSquared = x.toArray.zip(y.toArray).foldLeft(0.0)(
      (r, c) => r + pow(c._1 - c._2, 2)
    )
    sqrt(dSquared)
  }

  /**
   * Centroid of a finite set of points represented as a sequence of Vector's
   *
   * @param points Input set of points
   * @return Vector with the centroid
   */
  override def centroid(points: Seq[Vector]) = {
    val numCols = points(0).size
    val center = points.foldLeft(new Array[Double](numCols))(
      (r, c) => r.toArray.zip(c.toArray).map(t => t._1 + t._2)
    )
    Vectors.dense(center.map(_ / points.size))
  }

  /**
   * Cosine similarity distance measure between two Vector's x and y
   *
   * @param x Input Vector x
   * @param y Input Vector y
   * @return Double
   */
  override def cosine(x: Vector, y: Vector): Double = {
    val normX = sqrt(x.toArray.foldLeft(0.0)(
      (r, c) => r + c * c
    ))
    val normY = sqrt(y.toArray.foldLeft(0.0)(
      (r, c) => r + c * c
    ))
    val inner = x.toArray.zip(y.toArray).foldLeft(0.0)(
      (r, c) => r + c._1 * c._2
    )
    1.0 * inner / (normX * normY)
  }

  /**
   * Finds closest point and shortest distance between a given array of points x, and a given
   * point y. Uses brute-force L2-distance pairwise calculation.
   *
   * @todo Use better algorithm such as triangle inequality to find shortest distance
   * @param x Given Array of points, e.g. centroids in K-means clustering
   * @param y Given point from which distance needs to be calculated
   * @return (index in x, distance) of the closest point to y
   */
  override def closest(x: Array[Vector], y: Vector): (Int, Double) = {
    var shortestDistance = Double.PositiveInfinity
    var closestIndex = 0
    var index = 0
    x.foreach(center => {
      val thisDistance = distance(center, y)
      if (thisDistance < shortestDistance) {
        shortestDistance = thisDistance
        closestIndex = index
      }
      index += 1
    })
    (closestIndex, shortestDistance)
  }

  /**
   * Converts Array[(Vector, hashcode)] data structure of centers and points to an array of Vectors
   * only. Mostly used as a precursor to closest and other operations.
   * @param x Array of (Vector, Int)
   * @return Array[Vector]
   */
  def toVector(x: Array[(Vector, Int)]): Array[Vector] = {
    x.map(_._1)
  }

}
