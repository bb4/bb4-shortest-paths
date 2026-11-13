package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.graph.directed.DirectedGraph

/** Lower bounds on remaining distance to a destination, used as A-star and ALT potentials. */
object Potential {

  val Zero: Int => Double = _ => 0.0

  /**
   * Euclidean distance scaled by the minimum edge weight/length ratio so the
   * potential stays admissible when weights are shorter than geometry.
   */
  def scaledEuclidean(graph: DirectedGraph, destination: Int): Int => Double =
    graph.locations match {
      case None => Zero
      case Some(locations) =>
        val target = locations(destination)
        val scale = minWeightOverLength(graph, locations)
        (v: Int) => scale * locations(v).distance(target)
    }

  /**
   * Landmark triangle-inequality lower bound. Arrays are indexed by landmark,
   * then by vertex. Non-finite distances are skipped.
   */
  def fromLandmarks(
      distFromLandmark: Array[Array[Double]],
      distToLandmark: Array[Array[Double]],
      destination: Int
  ): Int => Double = { (v: Int) =>
    var best = 0.0
    var i = 0
    while (i < distFromLandmark.length) {
      val fromL = distFromLandmark(i)
      val toL = distToLandmark(i)
      val dLv = fromL(v)
      val dLt = fromL(destination)
      if (dLv.isFinite && dLt.isFinite) best = best.max(dLt - dLv)
      val dVl = toL(v)
      val dTl = toL(destination)
      if (dVl.isFinite && dTl.isFinite) best = best.max(dVl - dTl)
      i += 1
    }
    best.max(0.0)
  }

  private def minWeightOverLength(
      graph: DirectedGraph,
      locations: Array[FloatLocation]
  ): Double = {
    var scale = Double.PositiveInfinity
    for (edge <- graph.edges) {
      val length = locations(edge.source).distance(locations(edge.destination))
      if (length > 1e-12) {
        val ratio = edge.weight / length
        if (ratio < scale) scale = ratio
      }
    }
    if (scale.isInfinite) 0.0 else scale
  }
}
