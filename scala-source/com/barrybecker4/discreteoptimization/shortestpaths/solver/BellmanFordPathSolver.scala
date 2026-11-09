package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.algorithms.shortestpaths.{
  BellmanFordAlgorithm,
  BellmanFordResult,
  NegativeCycle
}
import com.barrybecker4.graph.directed.DirectedGraph

object BellmanFordPathSolver {
  val BASE_NAME = "bellman_ford"
}

/**
 * Single-source shortest paths via Bellman–Ford.
 * Allows negative edge weights and reports reachable negative cycles.
 */
class BellmanFordPathSolver extends ShortestPathsSolver {

  /**
   * Paths from `sourceVertex` to every vertex.
   * Throws [[java.lang.IllegalStateException]] if a negative cycle is reachable from the source.
   */
  override def findPaths(graph: DirectedGraph, sourceVertex: Int): ShortestPathsSolution = {
    findPathsOrCycle(graph, sourceVertex) match {
      case Right(solution) => solution
      case Left(cycle) =>
        throw new IllegalStateException(
          s"Negative cycle reachable from $sourceVertex: vertices=${cycle.vertices.mkString(" ")} weight=${cycle.weight}"
        )
    }
  }

  /**
   * Paths from `source`, or the negative cycle reachable from it.
   */
  def findPathsOrCycle(
      graph: DirectedGraph,
      source: Int
  ): Either[NegativeCycle, ShortestPathsSolution] =
    new BellmanFordAlgorithm(graph).find(source) match {
      case BellmanFordResult.Paths(paths) =>
        val pathList = Range(0, graph.numVertices).map(paths.pathToVertex).toList
        Right(ShortestPathsSolution(pathList.map(_.weight).sum, pathList))
      case BellmanFordResult.Cycle(cycle) => Left(cycle)
    }
}
