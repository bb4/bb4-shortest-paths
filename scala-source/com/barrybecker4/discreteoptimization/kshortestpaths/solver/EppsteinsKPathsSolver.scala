package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import com.barrybecker4.graph.algorithms.kshortestpaths.EppsteinsAlgorithm
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution


object EppsteinsKPathsSolver {
  val BASE_NAME = "eppsteins_kpaths"
}

/** The k shortest s–t walks (vertices may repeat). */
class EppsteinsKPathsSolver(k: Int = 3) extends KShortestPathsSolver {

  def findPaths(graph: DirectedGraph, source: Int, destination: Int, k: Int): KShortestPathsSolution = {

    val paths: Seq[Path] = EppsteinsAlgorithm(graph).findKShortestPaths(source, destination, k)

    val totalCost = paths.map(path => path.weight).sum
    val solution = KShortestPathsSolution(totalCost, destination, k, paths)

    solution
  }

}
