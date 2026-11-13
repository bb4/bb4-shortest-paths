package com.barrybecker4.graph.directed

import com.barrybecker4.graph.geometry.FloatLocation
import scala.collection.mutable


/**
 * A version of a directed graph that allows you to remove nodes and edges from the original graph.
 * You can also recover nodes and edges after they have been removed.
 * 
 * This implementation maintains mutable adjacency lists that are updated in-place during
 * remove/recover operations, avoiding the overhead of filtering the supergraph neighbors
 * on every query (which was a bottleneck in Yen's algorithm with frequent residual-graph searches).
 */
class ChangeableDirectedGraph(numVertices: Int, edges: IndexedSeq[DirectedEdge], locations: Option[Array[FloatLocation]] = None)
  extends DirectedGraph(numVertices, edges, locations) {

  def this(graph: DirectedGraph) = {
    this(graph.numVertices, graph.edges, graph.locations)
  }
  
  private var removedNodeSet = collection.immutable.Set[Int]()
  private var removedEdgeSet = collection.immutable.Set[DirectedEdge]()
  
  // Compute adjacency size directly (same logic as parent's private adjacencySize)
  private val adjSize: Int = {
    var maxId = numVertices - 1
    for (edge <- edges) {
      if (edge.source > maxId) maxId = edge.source
      if (edge.destination > maxId) maxId = edge.destination
    }
    if (maxId < 0) 0 else maxId + 1
  }
  
  // Mutable adjacency lists, initialized from the supergraph and updated in-place
  private val mutableOutgoing: Array[mutable.ArrayBuffer[DirectedEdge]] = 
    Array.tabulate(adjSize)(i => mutable.ArrayBuffer.from(super.outgoingNeighborsOf(i)))
  private val mutableIncoming: Array[mutable.ArrayBuffer[DirectedEdge]] = 
    Array.tabulate(adjSize)(i => mutable.ArrayBuffer.from(super.incomingNeighborsOf(i)))

  def remove(node: Int): Unit = {
    if (!removedNodeSet.contains(node)) {
      removedNodeSet += node
      
      // Remove all outgoing edges from this node
      mutableOutgoing(node).clear()
      
      // Remove all incoming edges to this node
      mutableIncoming(node).clear()
      
      // Remove edges pointing to this node from other nodes' outgoing lists
      for (i <- mutableOutgoing.indices if i != node) {
        mutableOutgoing(i).filterInPlace(_.destination != node)
      }
      
      // Remove edges coming from this node from other nodes' incoming lists
      for (i <- mutableIncoming.indices if i != node) {
        mutableIncoming(i).filterInPlace(_.source != node)
      }
    }
  }

  def remove(source: Int, dest: Int): Unit = {
    val edge = findOrigEdge(source, dest)
    if (!removedEdgeSet.contains(edge)) {
      removedEdgeSet += edge
      // Remove all edges with the same source, dest, and weight (handles equal parallel edges)
      mutableOutgoing(source).filterInPlace(e => !(e.source == edge.source && e.destination == edge.destination && e.weight == edge.weight))
      mutableIncoming(dest).filterInPlace(e => !(e.source == edge.source && e.destination == edge.destination && e.weight == edge.weight))
    }
  }
  
  def recover(node: Int): Unit = {
    if (removedNodeSet.contains(node)) {
      removedNodeSet -= node
      
      // Restore outgoing edges from this node (unless the edge or destination node is removed)
      mutableOutgoing(node).clear()
      mutableOutgoing(node) ++= super.outgoingNeighborsOf(node).filter(edge => 
        !removedEdgeSet.contains(edge) && !removedNodeSet.contains(edge.destination))
      
      // Restore incoming edges to this node (unless the edge or source node is removed)
      mutableIncoming(node).clear()
      mutableIncoming(node) ++= super.incomingNeighborsOf(node).filter(edge => 
        !removedEdgeSet.contains(edge) && !removedNodeSet.contains(edge.source))
      
      // Restore edges pointing to this node in other nodes' outgoing lists
      for (i <- mutableOutgoing.indices if i != node && !removedNodeSet.contains(i)) {
        val restoredEdges = super.outgoingNeighborsOf(i).filter(edge =>
          edge.destination == node && !removedEdgeSet.contains(edge) && !mutableOutgoing(i).contains(edge))
        mutableOutgoing(i) ++= restoredEdges
      }
      
      // Restore edges coming from this node in other nodes' incoming lists
      for (i <- mutableIncoming.indices if i != node && !removedNodeSet.contains(i)) {
        val restoredEdges = super.incomingNeighborsOf(i).filter(edge =>
          edge.source == node && !removedEdgeSet.contains(edge) && !mutableIncoming(i).contains(edge))
        mutableIncoming(i) ++= restoredEdges
      }
    }
  }

  def recover(source: Int, dest: Int): Unit = {
    val edge = findOrigEdge(source, dest)
    if (removedEdgeSet.contains(edge)) {
      removedEdgeSet -= edge
      
      // Only restore if both endpoints are not removed
      if (!removedNodeSet.contains(source) && !removedNodeSet.contains(dest)) {
        // Restore all original edges with matching source, dest, and weight
        // Note: parallel edges with equal weight are all represented by one entry in removedEdgeSet,
        // so we must restore all of them from the supergraph
        val edgesToRestore = super.outgoingNeighborsOf(source).filter(e => 
          e.source == edge.source && e.destination == edge.destination && e.weight == edge.weight)
        
        mutableOutgoing(source) ++= edgesToRestore
        mutableIncoming(dest) ++= edgesToRestore
      }
    }
  }
  
  /**
   * Lightest original edge from `source` to `dest`.
   * Parallel edges are distinguished by weight; equal weights are interchangeable.
   */
  def findOrigEdge(source: Int, dest: Int): DirectedEdge = {
    val matches = super.outgoingNeighborsOf(source).filter(_.destination == dest)
    if (matches.isEmpty) throw new IllegalStateException("No edge found from " + source + " to " + dest)
    else matches.minBy(_.weight)
  }
 
  def recover(): Unit = {
    removedEdgeSet = Set()
    removedNodeSet = Set()
    
    // Restore all adjacency lists from the supergraph
    for (i <- mutableOutgoing.indices) {
      mutableOutgoing(i).clear()
      mutableOutgoing(i) ++= super.outgoingNeighborsOf(i)
    }
    for (i <- mutableIncoming.indices) {
      mutableIncoming(i).clear()
      mutableIncoming(i) ++= super.incomingNeighborsOf(i)
    }
  }

  override def incomingNeighborsOf(node: Int): IndexedSeq[DirectedEdge] = 
    mutableIncoming(node).toIndexedSeq

  override def outgoingNeighborsOf(node: Int): IndexedSeq[DirectedEdge] = 
    mutableOutgoing(node).toIndexedSeq

  def edgeWeight(edge: DirectedEdge): Double =
    if (removedEdgeSet.contains(edge)) Double.MaxValue else edge.weight

  def edge: Seq[DirectedEdge] = edges.filter(edge => !removedEdgeSet.contains(edge))

}
