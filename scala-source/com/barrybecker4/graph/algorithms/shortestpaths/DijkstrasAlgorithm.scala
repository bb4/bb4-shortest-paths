package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.directed.DirectedGraph

import scala.annotation.tailrec


/**
 * Find the shortest path in a weighted directed graph using Dijkstra's algorithm.
 * Complexity should be O (V + E log V) because we are using a min-priority queue.
 *
 * Requires non-negative edge weights. A negative weight makes the result undefined;
 * this algorithm does not detect negative cycles. Use [[BellmanFordAlgorithm]] when
 * a weight may be negative.
 */
class DijkstrasAlgorithm(graph: DirectedGraph) extends ShortestPathsFinder {

  private val size: Int = graph.numVertices

  /**
   * find a shortest path from source vertex to all other vertices in the graph
   * @return shortest paths to all other nodes
   */
  override def findShortestPaths(source: Int): ShortestPaths =
    findShortestPaths(source, SearchObserver.None)

  def findShortestPaths(source: Int, observer: SearchObserver): ShortestPaths = {
    if (source >= size)
      throw new IllegalArgumentException(s"Source vertex must be < $size")
    else {
      val shortestPaths = ShortestPaths(size, source)
      val queue = DijkstraHeap()
      queue.enqueue(source, 0.0)
      observer.frontier(source, 0.0)

      def stillBest(vertex: Int, distQueued: Double): Boolean =
        DijkstraHeap.sameDistance(distQueued, shortestPaths.distToVertex(vertex))

      @tailrec
      def search(): Unit =
        queue.extractMin(stillBest) match {
          case None => ()
          case Some(vertex) =>
            observer.extracted(vertex)
            observer.settled(vertex)
            for (edge <- graph.outgoingNeighborsOf(vertex) if shortestPaths.isBetterEdge(edge)) {
              shortestPaths.useEdge(edge)
              val distance = shortestPaths.distToVertex(edge.destination)
              queue.enqueue(edge.destination, distance)
              observer.frontier(edge.destination, distance)
            }
            search()
        }

      search()
      shortestPaths
    }
  }
}
