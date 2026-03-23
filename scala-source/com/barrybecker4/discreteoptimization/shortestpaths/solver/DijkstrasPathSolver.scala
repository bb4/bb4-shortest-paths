package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.graph.algorithms.shortestpaths.DijkstrasAlgorithm
import com.barrybecker4.graph.{Graph, Path}
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution

import scala.compiletime.uninitialized
import scala.util.Random

object DijkstrasPathSolver {
  val BASE_NAME = "dijkstra"
}

/**
 */
class DijkstrasPathSolver extends ShortestPathsSolver {

  var graph: DirectedGraph = uninitialized

  /**
   * Find k shortest paths from source
   * For now, just print the shortest path k times
   */
  def findPaths(graph: DirectedGraph, sourceVertex: Int): ShortestPathsSolution = {

    val pathCalc = new DijkstrasAlgorithm(graph).findShortestPaths(0)

    val paths = Range(0, graph.numVertices).map(pathCalc.pathToVertex).toList
    val totalCost = paths.map(_.weight).sum
    val solution = ShortestPathsSolution(totalCost, paths)

    solution
  }

}
