package com.barrybecker4.graph.algorithms.shortestpaths


/**
 * find the shortest path in a weighted directed graph using Dijkstra's algorithm
 */
trait ShortestPathsFinder {

  /**
   * @return return shortest paths to all other nodes rom the specified source
   */
  def findShortestPaths(source: Int): ShortestPaths
}
