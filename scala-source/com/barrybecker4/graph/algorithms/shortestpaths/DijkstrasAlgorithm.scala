package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.directed.DirectedGraph

import scala.collection.mutable


/**
 * Find the shortest path in a weighted directed graph using Dijkstra's algorithm.
 * Complexity should be O (V + E log V) because we are using a min-priority queue.
 *
 * Scala's `mutable.PriorityQueue` is a max-heap; we negate distances so the largest stored
 * entry corresponds to the smallest actual distance. Stale queue entries are skipped
 * by comparing the queued distance to the current best (lazy deletion).
 */
class DijkstrasAlgorithm(graph: DirectedGraph) extends ShortestPathsFinder {

  private val size: Int = graph.numVertices

  /**
   * find a shortest path from source vertex to all other vertices in the graph
   * @return shortest paths to all other nodes
   */
  override def findShortestPaths(source: Int): ShortestPaths = {
    if (source >= size)
      throw new IllegalArgumentException(s"Source vertex must be < $size")
    else {
      val shortestPaths = ShortestPaths(size, source)

      // Max-heap on negated distance => extract-min by distance
      given Ordering[(Int, Double)] = Ordering.by((x: (Int, Double)) => -x._2)
      val queue = mutable.PriorityQueue.empty[(Int, Double)]
      queue.enqueue((source, 0.0))

      while (queue.nonEmpty) {
        val (vertex, distQueued) = queue.dequeue()
        val best = shortestPaths.distToVertex(vertex)
        if (distQueued == best || (distQueued - best).abs < 1e-12) {
          for (edge <- graph.outgoingNeighborsOf(vertex) if shortestPaths.isBetterEdge(edge)) {
            shortestPaths.useEdge(edge)
            val dest = edge.destination
            val d = shortestPaths.distToVertex(dest)
            queue.enqueue((dest, d))
          }
        }
      }

      shortestPaths
    }
  }
}
