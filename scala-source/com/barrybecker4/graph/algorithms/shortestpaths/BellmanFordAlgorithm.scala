package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.directed.DirectedGraph

import scala.collection.mutable.ListBuffer

/**
 * Single-source shortest paths that allow negative edge weights.
 * Reports a reachable negative cycle instead of returning undefined distances.
 * Complexity is O(V E).
 */
case class NegativeCycle(vertices: List[Int], weight: Double)

enum BellmanFordResult {
  case Paths(paths: ShortestPaths)
  /** Reachable negative cycle; payload is [[NegativeCycle]]. */
  case Cycle(cycle: NegativeCycle)
}

class BellmanFordAlgorithm(graph: DirectedGraph) extends ShortestPathsFinder {

  private val size: Int = graph.numVertices

  /**
   * Shortest paths from `source`, or throws if a negative cycle is reachable.
   */
  override def findShortestPaths(source: Int): ShortestPaths =
    find(source) match {
      case BellmanFordResult.Paths(paths) => paths
      case BellmanFordResult.Cycle(cycle) =>
        throw new IllegalStateException(
          s"Negative cycle reachable from $source: vertices=${cycle.vertices.mkString(" ")} weight=${cycle.weight}"
        )
    }

  /**
   * Shortest paths from `source`, or an explicit negative cycle reachable from it.
   */
  def find(source: Int): BellmanFordResult = {
    if (source < 0 || source >= size)
      throw new IllegalArgumentException(s"Source vertex must be in 0 until $size")

    val shortestPaths = ShortestPaths(size, source)
    val edges = graph.edges

    var pass = 0
    while (pass < size - 1) {
      for (edge <- edges if shortestPaths.isBetterEdge(edge)) {
        shortestPaths.useEdge(edge)
      }
      pass += 1
    }

    edges.find(shortestPaths.isBetterEdge) match {
      case Some(edge) =>
        shortestPaths.useEdge(edge)
        BellmanFordResult.Cycle(extractCycle(shortestPaths, edge.destination))
      case None =>
        BellmanFordResult.Paths(shortestPaths)
    }
  }

  /**
   * Walk far enough along predecessors to land on the cycle, then collect it
   * in directed-edge order with the smallest vertex id first.
   */
  private def extractCycle(paths: ShortestPaths, from: Int): NegativeCycle = {
    var v = from
    var i = 0
    while (i < paths.numNodes) {
      v = paths.previousNode(v).get
      i += 1
    }

    val cycleStart = v
    val reverseVerts = ListBuffer[Int](cycleStart)
    var weight = 0.0
    var current = cycleStart
    var closed = false
    while (!closed) {
      val edge = paths.edgeToVertex(current).get
      weight += edge.weight
      current = edge.source
      reverseVerts += current
      closed = current == cycleStart
    }

    canonicalize(reverseVerts.toList.reverse, weight)
  }

  /** Closed directed cycle rotated so the smallest vertex id is first. */
  private def canonicalize(vertices: List[Int], weight: Double): NegativeCycle = {
    val open = vertices.init
    val minIdx = open.zipWithIndex.minBy(_._1)._2
    val rotated = open.drop(minIdx) ++ open.take(minIdx)
    NegativeCycle(rotated :+ rotated.head, weight)
  }
}
