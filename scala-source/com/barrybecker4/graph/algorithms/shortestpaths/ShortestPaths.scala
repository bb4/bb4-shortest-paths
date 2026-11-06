package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedEdge

import scala.annotation.tailrec


/**
 * @param numNodes the number of nodes in the graph.
 * @param source the node that we will traverse from.
 */
case class ShortestPaths(numNodes: Int, source: Int) {

  private val edgeTo = Array.fill[Option[DirectedEdge]](numNodes)(None)
  private val distTo = Array.fill(numNodes)(Double.PositiveInfinity)
  distTo(source) = 0.0
  
  def pathNodesToVertex(vertex: Int): List[Int] = {
    val path = findPathToVertex(vertex)
    if (vertex == source) List(source)
    else if (path.isEmpty) List()
    else {
      val start: Int = path.head.source
      val nodes = path.map(_.destination)
      start +: nodes
    }
  }
  
  def previousNode(node: Int): Option[Int] = 
    if (edgeTo(node).isDefined) Some(edgeTo(node).get.source) else None

  def pathToVertex(vertex: Int): Path =
    Path(distToVertex(vertex), pathNodesToVertex(vertex))
  
  def isBetterEdge(edge: DirectedEdge): Boolean = 
    distTo(edge.source) + edge.weight < distTo(edge.destination)
  
  def useEdge(edge: DirectedEdge): Unit = {
    distTo(edge.destination) = distTo(edge.source) + edge.weight
    edgeTo(edge.destination) = Some(edge)
  }

  def distToVertex(v: Int): Double = distTo(v)

  /** @return sequence of edges which form the path from source vertex to vertex
   */
  private def findPathToVertex(vertex: Int): List[DirectedEdge] = {

    @tailrec
    def go(list: List[DirectedEdge], v: Int): List[DirectedEdge] =
      edgeTo(v) match {
        case Some(e) => go(e +: list, e.source)
        case None => list
      }

    if (hasPath(vertex)) go(List(), vertex) else List()
  }

  private def hasPath(v: Int): Boolean = edgeTo(v).isDefined
}