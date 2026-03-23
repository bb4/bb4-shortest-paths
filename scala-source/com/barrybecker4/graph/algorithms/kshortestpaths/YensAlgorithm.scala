package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.common.util.BoundedPriorityQueue
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.ModifiedDijkstrasAlgorithm
import com.barrybecker4.graph.directed.{ChangeableDirectedGraph, DirectedGraph}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.compiletime.uninitialized


/**
 * https://en.wikipedia.org/wiki/Yen%27s_algorithm#:~:text=In%20graph%20theory%2C%20Yen's%20algorithm,deviations%20of%20the%20best%20path.
 */
class YensAlgorithm(graph: DirectedGraph) extends KShortestPathsFinder {

  private var pathCandidates: BoundedPriorityQueue[Path] = uninitialized
  private val pathDerivationNodeIndex = mutable.Map[Path, Int]()
  private val paths = mutable.ListBuffer[Path]()
  private val changeableGraph = ChangeableDirectedGraph(graph)

  def findKShortestPaths(start: Int, end: Int, k: Int): Seq[Path] = {

    require(k > 0, "The number of paths should be positive")
    require(start != end, "the path is loopless, so the ends of paths should be different")

    // all results would be put in a priority queue with fixed length
    val sortByWeight: Ordering[Path] = (a, b) => a.weight.compareTo(b.weight)
    pathCandidates = new BoundedPriorityQueue[Path](k)(using sortByWeight)

    pathDerivationNodeIndex.clear()
    searchAll(start, end, k)
  }

  @tailrec
  private def searchAll(start: Int, end: Int, k: Int): List[Path] = {
    if (paths.isEmpty && pathCandidates.isEmpty) {
      new ModifiedDijkstrasAlgorithm(graph).getShortestPath(start, end) match {
        case None => return List.empty
        case Some(path) =>
          pathCandidates.addOne(path)
          pathDerivationNodeIndex.put(path, start)
      }

      searchAll(start, end, k) // recurse
    } else if (paths.size == k || pathCandidates.isEmpty) {
      paths.toList
    } else {
      val nextPath = pathCandidates.dequeue()

      val nextDerivedNodeIdx = removeEdgesAndNodes(nextPath) // 1. remove the edges and nodes from the graph

      // Why is it a million times faster if we skip this step????????????????????????
      identifyNewCandidateResults(nextPath, nextDerivedNodeIdx, end) // 2. recover from the ending, and calculate a few more candidates

      paths += nextPath // 3. recover all and update the result list
      changeableGraph.recover()

      searchAll(start, end, k) // 4. try it again
    }
  }

  private def removeEdgesAndNodes(nextPath: Path): Int = {
    val nextDerivedNode = pathDerivationNodeIndex(nextPath)
    val nextDerivedNodeIdx = nextPath.nodes.indexOf(nextDerivedNode)
    val nextSubPathNodeList: List[Int] = nextPath.nodes.dropRight(nextPath.nodes.size - nextDerivedNodeIdx - 1)

    def keep(path: Path): Boolean = {
      path.nodes.contains(nextDerivedNode) &&
        path.nodes.dropRight(path.nodes.size - path.nodes.indexOf(nextDerivedNode) - 1) == nextSubPathNodeList
    }

    paths.filter(keep).foreach(path =>
      changeableGraph.remove(nextDerivedNode, path.nodes(path.nodes.indexOf(nextDerivedNode) + 1))
    )

    for (nodeSeq <- 0 until nextPath.nodes.length - 1) {
      changeableGraph.remove(nextPath.nodes(nodeSeq))
      changeableGraph.remove(nextPath.nodes(nodeSeq), nextPath.nodes(nodeSeq + 1))
    }
    nextDerivedNodeIdx
  }

  private def identifyNewCandidateResults(nextPath: Path, nextDerivedNodeIdx: Int, end: Int): Unit = {
    // 2.1 calculate the shortest tree rooted at target vertex in the graph
    val findShortestPath = new ModifiedDijkstrasAlgorithm(changeableGraph)
    findShortestPath.findShortestPathFlowerRootAt(end)

    // 2.2 recover the deleted vertices and update the cost and identify the new candidate results
    for (nodeSeq <- nextPath.nodes.size - 2 to nextDerivedNodeIdx by -1 if nodeSeq >= 0) {
      val recoveredNode = nextPath.nodes(nodeSeq)
      changeableGraph.recover(recoveredNode)

      findShortestPath.getSubShortestPath(recoveredNode) match {
        case None =>
        case Some(subPath) =>
          findShortestPath.correctCostBackward(recoveredNode)
          val prefix = nextPath.nodes.dropRight(nextPath.nodes.size - nodeSeq - 1)
          var cost = 0.0
          for (i <- 0 until prefix.size - 1) {
            cost += changeableGraph.findOrigEdge(prefix(i), prefix(1 + i)).weight
          }
          val newPath: Path = Path(cost + subPath.weight, prefix.dropRight(1) ::: subPath.nodes)

          if (!pathDerivationNodeIndex.contains(newPath)) {
            pathDerivationNodeIndex.put(newPath, recoveredNode)
            pathCandidates += newPath
          }
      }

      // 2.3 recover edges
      val nextNode = nextPath.nodes(nodeSeq + 1)
      changeableGraph.recover(recoveredNode, nextNode)

      // 2.4 update cost if necessary
      val newCost = changeableGraph.findOrigEdge(recoveredNode, nextNode).weight +
        findShortestPath.getStartVertexDistance(nextNode)
      if (findShortestPath.getStartVertexDistance(recoveredNode) > newCost && newCost < Double.MaxValue) {
        findShortestPath.setStartVertexDistance(recoveredNode, newCost)
        findShortestPath.setPredecessor(recoveredNode, nextNode)
        findShortestPath.correctCostBackward(recoveredNode)
      }
    }
  }
}

