package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.ModifiedDijkstrasAlgorithm
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution

import scala.compiletime.uninitialized


object ModifiedDijkstrasPathSolver {
  val BASE_NAME = "modified_dijkstra"
}


class ModifiedDijkstrasPathSolver extends ShortestPathsSolver {

  var graph: DirectedGraph = uninitialized

  /**
   * Find k shortest paths from source
   * For now, just print the shortest path k times
   */
  def findPaths(graph: DirectedGraph, sourceVertex: Int): ShortestPathsSolution = {

    val alg = new ModifiedDijkstrasAlgorithm(graph)

    val paths = Range(0, graph.numVertices)
      .map(v => alg.getShortestPath(0, v).getOrElse(Path.EMPTY_PATH)).toList
    val totalCost = paths.map(_.weight).sum
    val solution = ShortestPathsSolution(totalCost, paths)

    solution
  }

}
