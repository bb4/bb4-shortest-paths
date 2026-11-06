package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.Path
import org.graphstream.graph.implementations.MultiGraph
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

/** File → Open must stop the previous mouse-event pump before the next solution is shown. */
class PathViewerReloadSuite extends AnyFunSuite {

  test("replacing the graph stops the previous path renderer pump") {
    class Harness extends PathViewerFrame {
      def showSolution(graph: MultiGraph, solution: ShortestPathsSolution): Unit = {
        setGraph(graph, graph.getId)
        showShortestPaths(solution, graph)
      }
    }
    val frame = new Harness
    frame.setVisible(false)

    try {
      frame.showSolution(lineGraph("first"), solution)
      val firstPump = waitForPump()
      assert(firstPump.isAlive)

      frame.showSolution(lineGraph("second"), solution)

      val stopped = waitUntil(!firstPump.isAlive)
      assert(stopped, s"previous pump ${firstPump.getName} still alive after opening another graph:\n${dump(firstPump)}")
      val pumps = livePumps
      assert(pumps.size == 1, s"expected one pump after reload, found ${pumps.map(_.getName)}")
    } finally {
      frame.dispose()
    }
  }

  private val solution = ShortestPathsSolution(0.0, List(Path(0.0, List(0))))

  private def lineGraph(name: String): MultiGraph = {
    val graph = new MultiGraph(name)
    graph.addNode("0")
    graph.addNode("1")
    graph.addEdge("0-1", "0", "1", true)
    graph
  }

  private def livePumps: Set[Thread] =
    Thread.getAllStackTraces.keySet.asScala.filter { thread =>
      thread.getName == "path-renderer-pump" && thread.isAlive
    }.toSet

  private def waitForPump(): Thread = {
    val deadline = System.nanoTime() + 3_000_000_000L
    var found: Set[Thread] = Set.empty
    while (found.isEmpty && System.nanoTime() < deadline) {
      found = livePumps
      if (found.isEmpty) Thread.sleep(20)
    }
    assert(found.size == 1, s"expected one pump, found ${found.map(_.getName)}")
    found.head
  }

  private def waitUntil(predicate: => Boolean): Boolean = {
    val deadline = System.nanoTime() + 3_000_000_000L
    while (!predicate && System.nanoTime() < deadline) Thread.sleep(20)
    predicate
  }

  private def dump(thread: Thread): String =
    thread.getStackTrace.mkString("\n")
}
