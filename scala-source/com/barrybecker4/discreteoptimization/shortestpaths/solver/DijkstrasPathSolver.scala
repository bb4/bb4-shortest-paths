package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.graph.algorithms.shortestpaths.{DijkstrasAlgorithm, SearchObserver}
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution

import scala.compiletime.uninitialized

object DijkstrasPathSolver {
  val BASE_NAME = "dijkstra"
}

/**
 */
class DijkstrasPathSolver extends ShortestPathsSolver {

  var graph: DirectedGraph = uninitialized

  /** Single-source shortest paths from `sourceVertex` to every vertex. */
  override def findPaths(graph: DirectedGraph, sourceVertex: Int): ShortestPathsSolution =
    findPaths(graph, sourceVertex, SearchObserver.None)

  def findPaths(
      graph: DirectedGraph,
      sourceVertex: Int,
      observer: SearchObserver
  ): ShortestPathsSolution = {
    val pathCalc = new DijkstrasAlgorithm(graph).findShortestPaths(sourceVertex, observer)
    val paths = Range(0, graph.numVertices).map(pathCalc.pathToVertex).toList
    ShortestPathsSolution(paths.map(_.weight).sum, paths)
  }

}
