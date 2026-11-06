package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph

import scala.annotation.tailrec

/**
 * Unidirectional s–t search with priority key `g(v) + potential(v)`.
 * With a zero potential this matches point-to-point Dijkstra.
 */
object GoalDirectedSearch {

  def search(
      graph: DirectedGraph,
      source: Int,
      destination: Int,
      potential: Int => Double,
      observer: SearchObserver = SearchObserver.None
  ): Option[Path] = {
    if (source == destination) return Some(Path(0.0, List(source)))

    val size = adjacencySize(graph)
    val distTo = Array.fill(size)(Double.PositiveInfinity)
    val parent = Array.fill(size)(-1)
    distTo(source) = 0.0

    val queue = DijkstraHeap()
    def key(vertex: Int): Double = distTo(vertex) + potential(vertex)

    def stillBest(vertex: Int, distQueued: Double): Boolean =
      DijkstraHeap.sameDistance(distQueued, key(vertex))

    queue.enqueue(source, key(source))
    observer.frontier(source, 0.0)

    @tailrec
    def scan(): Option[Path] =
      queue.extractMin(stillBest) match {
        case None => None
        case Some(vertex) =>
          observer.extracted(vertex)
          observer.settled(vertex)
          if (vertex == destination)
            Some(Path(distTo(destination), reconstruct(parent, source, destination)))
          else {
            for (edge <- graph.outgoingNeighborsOf(vertex)) {
              val next = edge.destination
              val candidate = distTo(vertex) + edge.weight
              if (candidate < distTo(next)) {
                distTo(next) = candidate
                parent(next) = vertex
                queue.enqueue(next, key(next))
                observer.frontier(next, candidate)
              }
            }
            scan()
          }
      }

    scan()
  }

  private def adjacencySize(graph: DirectedGraph): Int = {
    var maxId = graph.numVertices - 1
    for (edge <- graph.edges) {
      if (edge.source > maxId) maxId = edge.source
      if (edge.destination > maxId) maxId = edge.destination
    }
    if (maxId < 0) 0 else maxId + 1
  }

  private def reconstruct(parent: Array[Int], source: Int, destination: Int): List[Int] = {
    @tailrec
    def go(v: Int, acc: List[Int]): List[Int] =
      if (v == source) source :: acc
      else if (parent(v) < 0) throw new IllegalStateException(s"Could not find path to $destination")
      else go(parent(v), v :: acc)

    go(destination, Nil)
  }
}
