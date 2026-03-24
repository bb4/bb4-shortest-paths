package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.PriorityQueue


/**
 * Find the shortest path in a weighted directed graph using a version of
 * Dijkstra's algorithm that has been modified to support an efficient implementation
 * of the top-k shortest paths algorithm.
 * It allows finding a shortest path from a source to a single destination instead of all destinations at once.
 * Implementation based on https://github.com/yan-qi/k-shortest-paths-scala-version
 *
 * See https://github.com/yan-qi/k-shortest-paths-scala-version
 * From https://www.adrian.idv.hk/2011-05-18-y71-shortestpath/
 * Complexity should be O (V + E log V)  because we are using a minPriority queue.
 *
 * A[1] := Shortest path from S to T
 * for k := 2 to K do {
 *   for i := 1 to length(A[1])-1 do {
 *       nodeA = A[k-1].node(i)
 *       for j := 1 to k-2 {
 *           nodeB = A[j].node(i)
 *           if (nodeA == nodeB)
 *               distance(nodeA, nodeB) = infinity
 *       }
 *       S[i] = The shortest path from nodeA to T according to the current distance values
 *       R[i] = The path in A[k-1] from S to nodeA
 *       B[i] = R[i] + S[i]
 *   }
 *   A[k] = min length paths amongst all B[i]
 *   restore distance(nodeA, nodeB) to original value if modified
 * }
 */
class ModifiedDijkstrasAlgorithm(graph: DirectedGraph) {

  private var determinedVertexSet: Set[Int] = Set()
  private val distanceTo: mutable.Map[Int, Double] = mutable.Map()
  private val vertexCandidateQueue: mutable.PriorityQueue[Int] =
    mutable.PriorityQueue.empty(using Ordering.by(distanceTo).reverse)
  private var predecessorMap: Map[Int, Int] = Map()

  def clear(): Unit = {
    determinedVertexSet = Set()
    vertexCandidateQueue.clear()
    distanceTo.clear()
    predecessorMap = Map()
  }

  def getShortestPath(source: Int, destination: Int): Option[Path] = {

    val isOppositeDirection = false
    determineShortestPaths(source, destination, isOppositeDirection)

    if (distanceTo.contains(destination))
      Some(Path(distanceTo(destination), getPath(source, destination)))
    else None
  }
  
  def setStartVertexDistance(vertex: Int, distance: Double): Unit =
    distanceTo.put(vertex, distance)
    
  def getStartVertexDistance(vertex: Int): Double = 
    distanceTo.getOrElse(vertex, Double.MaxValue)
    
  def setPredecessor(vertex: Int, predecessor: Int): Unit = 
    predecessorMap += vertex -> predecessor

  private def getPath(source: Int, destination: Int): List[Int] = {
    if (source == destination)
      List[Int](source)
    else {
      predecessorMap.get(destination) match {
        case Some(previous) =>
          val path = getPath(source, previous)
          path :+ destination
        case None => throw new IllegalStateException("Could not find " + destination + " in " + predecessorMap.keys)
      }
    }
  }

  @tailrec
  private def getReversePath(sink: Int, list: List[Int]): List[Int] = {
    predecessorMap.get(sink) match {
      case Some(pre) => getReversePath(pre, list :+ sink)
      case None => list :+ sink
    }
  }

  @tailrec
  private def updateVertex (end: Int, isOpposite: Boolean): Unit = {
    if (vertexCandidateQueue.nonEmpty) {
      val node: Int = vertexCandidateQueue.dequeue()

      if (node != end) {
        determinedVertexSet += node
        val neighborSet = if (isOpposite) graph.incomingNeighborsOf(node) else graph.outgoingNeighborsOf(node)

        def isDetermined(e: DirectedEdge): Boolean =
          determinedVertexSet.contains(if (isOpposite) e.source else e.destination)

        neighborSet.filterNot(isDetermined).foreach(nextEdge => {
          val next = if (isOpposite) nextEdge.source else nextEdge.destination
          val edgeWeight = nextEdge.weight
          val curDistance = distanceTo.getOrElse(node, Double.MaxValue - edgeWeight)
          val distance = curDistance + edgeWeight
          if (!distanceTo.contains(next) || distance < distanceTo(next)) {
            distanceTo.put(next, distance)
            predecessorMap += next -> node
            vertexCandidateQueue += next
          }
        })
        updateVertex(end, isOpposite)
      }
    }
  }

  private def determineShortestPaths(source: Int, destination: Int, isOpposite: Boolean): Unit = {
    clear()
    val end = if (isOpposite) source else destination
    val start = if (isOpposite) destination else source
    distanceTo.put(start, 0d)
    vertexCandidateQueue += start
    updateVertex(end, isOpposite)
  }

  /**
   * Construct a flower rooted at "root" with
   * the shortest paths from the other vertices.
   * @param root the node as the root
   */
  def findShortestPathFlowerRootAt(root: Int): Unit =
    determineShortestPaths(-1, root, isOpposite = true)

  /**
   * Correct costs of successors of the input vertex using backward star form.
   * (FLOWER)
   * @param node the input node to start with
   */
  def correctCostBackward(node: Int): Unit = {
    graph.incomingNeighborsOf(node).foreach(preEdge => {
      val previous = preEdge.source
      val newWeight = preEdge.weight + distanceTo(node)
      val oldWeight = distanceTo.getOrElse(previous, Double.MaxValue)
      if (newWeight < oldWeight) {
        distanceTo.put(previous, newWeight)
        predecessorMap += previous -> node
        correctCostBackward(previous)
      }
    })
  }

  private def correctCostForward(node: Int): Double = {
    var cost = Double.MaxValue
    val outgoingEdges = graph.outgoingNeighborsOf(node)
    outgoingEdges
      .filter(e => distanceTo.contains(e.destination))
      .foreach(nextEdge => {
        val next = nextEdge.destination
        val newWeight = nextEdge.weight + distanceTo(next)
        if (newWeight < distanceTo.getOrElse(node, Double.MaxValue)) {
          distanceTo.put(node, newWeight)
          predecessorMap += node -> next
          cost = newWeight
        }
      })
    cost
  }

  def getSubShortestPath(source: Int): Option[Path] = {
    correctCostForward(source) match {
      case Double.MaxValue => None
      case cost => Some(Path(cost, getReversePath(source, List())))
    }
  }
}
