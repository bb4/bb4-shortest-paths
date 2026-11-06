package com.barrybecker4.graph.directed

import scala.collection.mutable.ArrayBuffer

/**
 * Edges incident to each vertex, stored in an array indexed by vertex id.
 * Parallel edges are retained in insertion order, including equal weights.
 * Call [[freeze]] after the last [[addNeighbor]].
 */
class DirectedNeighborMap(numVertices: Int) {

  private val buffers = Array.fill(numVertices)(ArrayBuffer.empty[DirectedEdge])
  private var neighbors: Array[IndexedSeq[DirectedEdge]] = _

  def apply(v: Int): IndexedSeq[DirectedEdge] = neighbors(v)

  def addNeighbor(v: Int, edge: DirectedEdge): Unit =
    buffers(v) += edge

  def freeze(): Unit =
    neighbors = buffers.map(_.toIndexedSeq)
}
