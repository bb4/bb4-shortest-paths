package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph

import scala.collection.mutable

/**
 * ALT (A*, landmarks, triangle inequality) for point-to-point queries.
 * Landmark distance labels are built once in the constructor and reused.
 */
class AltAlgorithm(graph: DirectedGraph, landmarkCount: Int = 8) {

  private val size: Int = graph.numVertices
  private val landmarks: Array[Int] = pickLandmarks(landmarkCount.max(0))
  private val distFromLandmark: Array[Array[Double]] =
    landmarks.map(l => distancesFrom(graph, l))
  private val distToLandmark: Array[Array[Double]] = {
    val transposed = graph.transpose
    landmarks.map(l => distancesFrom(transposed, l))
  }

  def getShortestPath(
      source: Int,
      destination: Int,
      observer: SearchObserver = SearchObserver.None
  ): Option[Path] =
    GoalDirectedSearch.search(
      graph,
      source,
      destination,
      Potential.fromLandmarks(distFromLandmark, distToLandmark, destination),
      observer
    )

  private def pickLandmarks(count: Int): Array[Int] = {
    if (count == 0 || size == 0) return Array.empty

    val selected = mutable.ArrayBuffer.empty[Int]
    val distCache = mutable.Map.empty[Int, Array[Double]]

    def distFrom(landmark: Int): Array[Double] =
      distCache.getOrElseUpdate(landmark, distancesFrom(graph, landmark))

    farthestFinite(distFrom(0), Set.empty).foreach(selected += _)

    while (selected.size < count && selected.size < size) {
      val dists = selected.map(distFrom).toArray
      var bestVertex = -1
      var bestScore = Double.NegativeInfinity
      var v = 0
      while (v < size) {
        if (!selected.contains(v)) {
          var minDist = Double.PositiveInfinity
          var i = 0
          while (i < dists.length) {
            val d = dists(i)(v)
            if (d.isFinite && d < minDist) minDist = d
            i += 1
          }
          if (minDist.isFinite && minDist > bestScore) {
            bestScore = minDist
            bestVertex = v
          }
        }
        v += 1
      }
      if (bestVertex < 0) return selected.toArray
      selected += bestVertex
    }
    selected.toArray
  }

  private def farthestFinite(dist: Array[Double], exclude: Set[Int]): Option[Int] = {
    var best = -1
    var bestDist = Double.NegativeInfinity
    var v = 0
    while (v < dist.length) {
      if (!exclude.contains(v) && dist(v).isFinite && dist(v) > bestDist) {
        bestDist = dist(v)
        best = v
      }
      v += 1
    }
    if (best < 0) None else Some(best)
  }

  private def distancesFrom(g: DirectedGraph, source: Int): Array[Double] = {
    val paths = new DijkstrasAlgorithm(g).findShortestPaths(source)
    Array.tabulate(size)(v => paths.distToVertex(v))
  }
}
