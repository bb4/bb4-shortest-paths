package com.barrybecker4.graph.directed

import scala.collection.mutable

/** 
 * Keeps track of node neighbors. There can be more than one edge from a to b.
 */
class DirectedNeighborMap {

  private val map = mutable.Map.empty[Int, Set[DirectedEdge]]

  def apply(v: Int): Set[DirectedEdge] = map.getOrElse(v, Set.empty)

  def addNeighbor(v1: Int, edge: DirectedEdge): Unit = {
    map(v1) = map.getOrElse(v1, Set.empty) + edge
  }
}
