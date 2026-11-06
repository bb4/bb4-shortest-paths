package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

class PathViewerSolveSuite extends AnyFunSuite {

  System.setProperty("org.graphstream.ui", "swing")

  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("solve marks the chosen source and starts one path-renderer pump") {
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      assert(frame.graphLoaded.contains(triangle))

      frame.run(SolveRequest(AlgorithmKind.Dijkstra, 2, 0, 3))

      val graph = frame.viewer.getGraphicGraph
      assert(graph.getNode("2").getAttribute("ui.class", classOf[String]) == "source")
      assert(graph.getNode("0").getAttribute("ui.class") == null)
      val pumps = waitForPumps()
      assert(pumps.size == 1)
    } finally {
      frame.dispose()
    }
  }

  /** Subclass so the test can call protected `presentDigraph`. */
  private class Harness extends PathViewerFrame {
    def load(digraph: DirectedGraph): Unit = presentDigraph(digraph, "triangle", 0)
    def run(request: SolveRequest): Unit = solveNow(request)
    def graphLoaded: Option[DirectedGraph] = loadedGraph
  }

  private def waitForPumps(): Set[Thread] = {
    val deadline = System.nanoTime() + 3_000_000_000L
    var found = Set.empty[Thread]
    while (found.isEmpty && System.nanoTime() < deadline) {
      found = Thread.getAllStackTraces.keySet.asScala.filter { thread =>
        thread.getName == "path-renderer-pump" && thread.isAlive
      }.toSet
      if (found.isEmpty) Thread.sleep(20)
    }
    found
  }
}
