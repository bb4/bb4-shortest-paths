package com.barrybecker4.discreteoptimization.apps

import com.barrybecker4.graph.algorithms.kshortestpaths.YensAlgorithm
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}

object DemoGraphApp {

  def main(args: Array[String]): Unit = {
    val graph = createGraph()
    val numShortestPaths = 2
    val kshortestPaths = YensAlgorithm(graph).findKShortestPaths(0, 4, numShortestPaths)
    println(s"The $numShortestPaths found were:\n" + kshortestPaths)
  }

  private def createGraph(): DirectedGraph = {
    val edges = IndexedSeq(
      DirectedEdge(0, 2, 2.2),
      DirectedEdge(0, 3, 2.2),
      DirectedEdge(2, 1, 2.2),
      DirectedEdge(3, 1, 2.2),
      DirectedEdge(1, 4, 2.2),
      DirectedEdge(2, 4, 2.2),
      DirectedEdge(2, 4, 20.8),
      DirectedEdge(4, 2, 1.2),
      DirectedEdge(1, 0, 1.1),
    )
    DirectedGraph(5, edges)
  }

}



