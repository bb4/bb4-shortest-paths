package com.barrybecker4.graph.algorithms.shortestpaths

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Shared extract-min core for Dijkstra searches.
 *
 * Entries are `(vertex, distance)` snapshotted at enqueue time. Scala's priority
 * queue is a max-heap, so distances are negated. A vertex may be queued more than
 * once; [[extractMin]] drops entries that `stillBest` rejects.
 */
class DijkstraHeap {

  private val queue =
    mutable.PriorityQueue.empty[(Int, Double)](using Ordering.by[(Int, Double), Double](entry => -entry._2))

  def enqueue(vertex: Int, distance: Double): Unit =
    queue.enqueue((vertex, distance))

  def clear(): Unit =
    queue.clear()

  @tailrec
  final def extractMin(stillBest: (Int, Double) => Boolean): Option[Int] =
    if (queue.isEmpty) None
    else {
      val (vertex, distQueued) = queue.dequeue()
      if (stillBest(vertex, distQueued)) Some(vertex)
      else extractMin(stillBest)
    }
}

object DijkstraHeap {

  private val DistanceEpsilon = 1e-12

  def sameDistance(queued: Double, best: Double): Boolean =
    queued == best || (queued - best).abs < DistanceEpsilon
}
