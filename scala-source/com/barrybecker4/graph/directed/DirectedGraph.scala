package com.barrybecker4.graph.directed

import com.barrybecker4.graph.geometry.FloatLocation


/** 
 * An immutable directed graph with optional node locations
 */
case class DirectedGraph(numVertices: Int, edges: IndexedSeq[DirectedEdge], locations: Option[Array[FloatLocation]] = None) {

  // Adjacency indexed by vertex id. Ids may exceed numVertices - 1.
  private val outgoingNeighborMap = DirectedNeighborMap(adjacencySize)
  private val incomingNeighborMap = DirectedNeighborMap(adjacencySize)
  computeNeighborsMap()

  def outgoingNeighborsOf(v: Int): IndexedSeq[DirectedEdge] = outgoingNeighborMap(v)
  def incomingNeighborsOf(v: Int): IndexedSeq[DirectedEdge] = incomingNeighborMap(v)

  def findMinWeightEdge(source: Int, dest: Int): DirectedEdge = {
    val edges = outgoingNeighborsOf(source).filter(_.destination == dest)
    if (edges.isEmpty)
      throw new IllegalStateException("No edge found from " + source + " to " + dest) 
    else edges.minBy(_.weight)
  }
  
  /** Reverse all the edges in a given directed graph */
  def transpose: DirectedGraph = 
    DirectedGraph(numVertices, edges.map(e => DirectedEdge(e.destination, e.source, e.weight)), locations)
  
  
  /** Vertex ids are not always a dense 0 .. numVertices - 1 range. */
  private def adjacencySize: Int = {
    var maxId = numVertices - 1
    for (edge <- edges) {
      if (edge.source > maxId) maxId = edge.source
      if (edge.destination > maxId) maxId = edge.destination
    }
    if (maxId < 0) 0 else maxId + 1
  }

  private def computeNeighborsMap(): Unit = {
    for (edge <- edges) {
      outgoingNeighborMap.addNeighbor(edge.source, edge)
      incomingNeighborMap.addNeighbor(edge.destination, edge)
    }
    outgoingNeighborMap.freeze()
    incomingNeighborMap.freeze()
  }

}
