package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.{ModifiedDijkstrasAlgorithm, SearchObserver}
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution

import scala.compiletime.uninitialized


object ModifiedDijkstrasPathSolver {
  val BASE_NAME = "modified_dijkstra"
}


class ModifiedDijkstrasPathSolver extends ShortestPathsSolver {

  var graph: DirectedGraph = uninitialized

  /** Single-source shortest paths from `sourceVertex` to every vertex. */
  def findPaths(graph: DirectedGraph, sourceVertex: Int): ShortestPathsSolution = {

    val alg = new ModifiedDijkstrasAlgorithm(graph)
    val pathCalc = alg.findShortestPaths(sourceVertex)

    val paths = Range(0, graph.numVertices).map(pathCalc.pathToVertex).toList
    val totalCost = paths.map(_.weight).sum
    val solution = ShortestPathsSolution(totalCost, paths)

    solution
  }

  def findPath(
      graph: DirectedGraph,
      source: Int,
      destination: Int,
      observer: SearchObserver = SearchObserver.None
  ): Option[Path] =
    new ModifiedDijkstrasAlgorithm(graph).getShortestPath(source, destination, observer)

}
