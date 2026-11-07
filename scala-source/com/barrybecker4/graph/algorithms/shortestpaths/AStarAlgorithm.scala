package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph

/** Point-to-point A* using a scaled Euclidean potential from node locations. */
class AStarAlgorithm(graph: DirectedGraph) {

  def getShortestPath(
      source: Int,
      destination: Int,
      observer: SearchObserver = SearchObserver.None
  ): Option[Path] = {
    require(AStarAlgorithm.available(graph), "A* needs node locations")
    GoalDirectedSearch.search(
      graph,
      source,
      destination,
      Potential.scaledEuclidean(graph, destination),
      observer
    )
  }
}

object AStarAlgorithm {
  def available(graph: DirectedGraph): Boolean = graph.locations.isDefined
}
