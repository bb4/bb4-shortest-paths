package com.barrybecker4.graph.algorithms.shortestpaths

import org.scalatest.funsuite.AnyFunSuite

class DijkstraHeapSuite extends AnyFunSuite {

  private def stillBest(best: Map[Int, Double])(vertex: Int, queued: Double): Boolean =
    DijkstraHeap.sameDistance(queued, best.getOrElse(vertex, Double.PositiveInfinity))

  test("extractMin returns the closest vertex and skips a stale longer entry") {
    val heap = DijkstraHeap()
    heap.enqueue(1, 5.0)
    heap.enqueue(2, 1.0)
    heap.enqueue(1, 3.0)

    val best = Map(1 -> 3.0, 2 -> 1.0)
    assert(heap.extractMin(stillBest(best)).contains(2))
    assert(heap.extractMin(stillBest(best)).contains(1))
    assert(heap.extractMin(stillBest(best)).isEmpty)
  }

  test("clear drops queued vertices") {
    val heap = DijkstraHeap()
    heap.enqueue(0, 0.0)
    heap.clear()
    assert(heap.extractMin((_, _) => true).isEmpty)
  }

  test("minKey skips a stale longer entry and leaves the live minimum for extractMin") {
    val heap = DijkstraHeap()
    heap.enqueue(0, 5.0)
    heap.enqueue(0, 1.0)
    val stillBest = (vertex: Int, queued: Double) => vertex == 0 && DijkstraHeap.sameDistance(queued, 1.0)
    assert(heap.minKey(stillBest).contains(1.0))
    assert(heap.extractMin(stillBest).contains(0))
  }
}
