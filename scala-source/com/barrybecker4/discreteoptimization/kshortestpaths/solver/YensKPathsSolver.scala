package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import com.barrybecker4.graph.algorithms.kshortestpaths.YensAlgorithm
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution


object YensKPathsSolver {
  val BASE_NAME = "yens_kpaths"
}
/**
 */
class YensKPathsSolver(k: Int = 3) extends KShortestPathsSolver {

  /**
   * Find k shortest paths from source
   */
  def findPaths(graph: DirectedGraph, source: Int, destination: Int, k: Int): KShortestPathsSolution = {

    val paths: Seq[Path] = YensAlgorithm(graph).findKShortestPaths(source, destination, k)

    val totalCost = paths.map(path => path.weight).sum
    val solution = KShortestPathsSolution(totalCost, destination, k, paths)

    solution
  }

}
