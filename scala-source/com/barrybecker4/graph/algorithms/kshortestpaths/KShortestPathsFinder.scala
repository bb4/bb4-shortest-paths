package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.graph.Path


/** Finds up to k walks between two vertices in a directed graph, ordered by weight. */
trait KShortestPathsFinder {

  /** @return up to k shortest walks from source to destination, cheapest first */
  def findKShortestPaths(source: Int, destination: Int, k: Int): Seq[Path]

}
