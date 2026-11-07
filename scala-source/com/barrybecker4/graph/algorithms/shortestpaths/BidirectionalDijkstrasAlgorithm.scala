package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Point-to-point Dijkstra that grows a forward search from the source and a
 * reverse search from the destination, stopping when the sum of live queue keys
 * meets or exceeds the best meeting path cost μ.
 *
 * Requires non-negative edge weights. A negative weight makes the result undefined;
 * this algorithm does not detect negative cycles. Use [[BellmanFordAlgorithm]] when
 * a weight may be negative.
 */
class BidirectionalDijkstrasAlgorithm(graph: DirectedGraph) {

  def getShortestPath(
      source: Int,
      destination: Int,
      observer: SearchObserver = SearchObserver.None
  ): Option[Path] = {
    if (source == destination) return Some(Path(0.0, List(source)))

    val size = adjacencySize(graph)
    val gForward = Array.fill(size)(Double.PositiveInfinity)
    val gBackward = Array.fill(size)(Double.PositiveInfinity)
    val parentForward = Array.fill(size)(-1)
    val parentBackward = Array.fill(size)(-1)
    val settledForward = mutable.Set.empty[Int]
    val settledBackward = mutable.Set.empty[Int]

    gForward(source) = 0.0
    gBackward(destination) = 0.0

    val forward = DijkstraHeap()
    val backward = DijkstraHeap()

    def stillBestForward(vertex: Int, queued: Double): Boolean =
      !settledForward.contains(vertex) &&
        DijkstraHeap.sameDistance(queued, gForward(vertex))

    def stillBestBackward(vertex: Int, queued: Double): Boolean =
      !settledBackward.contains(vertex) &&
        DijkstraHeap.sameDistance(queued, gBackward(vertex))

    forward.enqueue(source, 0.0)
    backward.enqueue(destination, 0.0)
    observer.frontier(source, 0.0, SearchDirection.Forward)
    observer.frontier(destination, 0.0, SearchDirection.Backward)

    var mu = Double.PositiveInfinity
    var meet = -1

    def considerMeet(vertex: Int): Unit = {
      val total = gForward(vertex) + gBackward(vertex)
      if (total < mu) {
        mu = total
        meet = vertex
      }
    }

    def expandForward(): Unit =
      forward.extractMin(stillBestForward).foreach { vertex =>
        observer.extracted(vertex, SearchDirection.Forward)
        observer.settled(vertex, SearchDirection.Forward)
        settledForward += vertex
        if (gBackward(vertex).isFinite) considerMeet(vertex)
        for (edge <- graph.outgoingNeighborsOf(vertex) if !settledForward.contains(edge.destination)) {
          val next = edge.destination
          val candidate = gForward(vertex) + edge.weight
          if (candidate < gForward(next)) {
            gForward(next) = candidate
            parentForward(next) = vertex
            forward.enqueue(next, candidate)
            observer.frontier(next, candidate, SearchDirection.Forward)
            if (gBackward(next).isFinite) considerMeet(next)
          }
        }
      }

    def expandBackward(): Unit =
      backward.extractMin(stillBestBackward).foreach { vertex =>
        observer.extracted(vertex, SearchDirection.Backward)
        observer.settled(vertex, SearchDirection.Backward)
        settledBackward += vertex
        if (gForward(vertex).isFinite) considerMeet(vertex)
        for (edge <- graph.incomingNeighborsOf(vertex) if !settledBackward.contains(edge.source)) {
          val next = edge.source
          val candidate = gBackward(vertex) + edge.weight
          if (candidate < gBackward(next)) {
            gBackward(next) = candidate
            parentBackward(next) = vertex
            backward.enqueue(next, candidate)
            observer.frontier(next, candidate, SearchDirection.Backward)
            if (gForward(next).isFinite) considerMeet(next)
          }
        }
      }

    @tailrec
    def search(): Unit = {
      val fKey = forward.minKey(stillBestForward)
      val bKey = backward.minKey(stillBestBackward)
      (fKey, bKey) match {
        case (None, _) | (_, None) => ()
        case (Some(fk), Some(bk)) if fk + bk >= mu => ()
        case (Some(fk), Some(bk)) =>
          if (fk <= bk) expandForward() else expandBackward()
          search()
      }
    }

    search()
    if (meet < 0 || mu.isInfinite) None
    else Some(Path(mu, reconstruct(parentForward, parentBackward, source, destination, meet)))
  }

  private def adjacencySize(graph: DirectedGraph): Int = {
    var maxId = graph.numVertices - 1
    for (edge <- graph.edges) {
      if (edge.source > maxId) maxId = edge.source
      if (edge.destination > maxId) maxId = edge.destination
    }
    if (maxId < 0) 0 else maxId + 1
  }

  private def reconstruct(
      parentForward: Array[Int],
      parentBackward: Array[Int],
      source: Int,
      destination: Int,
      meet: Int
  ): List[Int] = {
    @tailrec
    def forwardPath(v: Int, acc: List[Int]): List[Int] =
      if (v == source) source :: acc
      else if (parentForward(v) < 0)
        throw new IllegalStateException(s"Missing forward parent for $v")
      else forwardPath(parentForward(v), v :: acc)

    @tailrec
    def backwardTail(v: Int, acc: List[Int]): List[Int] =
      if (v == destination) acc
      else if (parentBackward(v) < 0)
        throw new IllegalStateException(s"Missing backward parent for $v")
      else {
        val next = parentBackward(v)
        backwardTail(next, acc :+ next)
      }

    val toMeet = forwardPath(meet, Nil)
    if (meet == destination) toMeet
    else toMeet ++ backwardTail(meet, Nil)
  }
}
