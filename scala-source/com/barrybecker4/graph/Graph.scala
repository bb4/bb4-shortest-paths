package com.barrybecker4.graph

import scala.collection.immutable.{Map, Set}


/** Undirected graph
 */
case class Graph(numVertices: Int, edges: IndexedSeq[Edge])  {

  // Map from vertex to its neighbors
  private val neighborMap: NeighborMap = NeighborMap()
  computeNeighborsMap()

  def neighborsOf(v: Int): Set[Int] = neighborMap(v)
  
  private def computeNeighborsMap(): Unit = {
    for (edge <- edges) {
      neighborMap.addNeighbor(edge.v1, edge.v2)
      neighborMap.addNeighbor(edge.v2, edge.v1)
    }
  }
}

