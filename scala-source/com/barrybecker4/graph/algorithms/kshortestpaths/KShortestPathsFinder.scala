package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.graph.Path


/** Finds the k shortest simple paths between two vertices in a directed graph. */
trait KShortestPathsFinder {

  /** @return up to k shortest paths from source to destination */
  def findKShortestPaths(source: Int, destination: Int, k: Int): Seq[Path]

}
