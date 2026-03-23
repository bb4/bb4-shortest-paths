package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, PriorityQueue}


/**
 * Find the shortest path in a weighted directed graph using Dijkstra's algorithm.
 * Complexity should be O (V + E log V)  because we are using a minPriority queue.
 */
class DijkstrasAlgorithm(graph: DirectedGraph) extends ShortestPathsFinder {

  private val size: Int = graph.numVertices

  /**
   * - add 2 implementations - Yen's and Eppstein's (https://codeforces.com/blog/entry/102085).
   * - add graph path visualization
   *
   * find a shortest path from source vertex to all other vertices in the graph
   * @return return shortest paths to all other nodes
   */
  override def findShortestPaths(source: Int): ShortestPaths = {
    if (source >= size)
      throw new IllegalArgumentException(s"Source vertex must be < $size")
    else {
      val shortestPaths = ShortestPaths(size, source)

      val sourceDist = (source, 0.0)
      val sortByWeight: Ordering[(Int, Double)] = (a, b) => a._2.compareTo(b._2)
      val queue = mutable.PriorityQueue.empty[(Int, Double)](using sortByWeight)
      queue.enqueue(sourceDist)

      while (queue.nonEmpty) {
        val (minDestVertex, _) = queue.dequeue()
        val edges = graph.outgoingNeighborsOf(minDestVertex)

        for (edge <- edges) {
          val destination = edge.destination
          if (shortestPaths.isBetterEdge(edge)) {
            shortestPaths.useEdge(edge)
            if (!queue.exists(_._1 == destination))
              queue.enqueue((destination, shortestPaths.distToVertex(destination)))
          }
        }
      }

      shortestPaths
    }
  }
}
