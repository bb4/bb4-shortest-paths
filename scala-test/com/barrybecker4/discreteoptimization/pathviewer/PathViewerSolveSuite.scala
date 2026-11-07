package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.Outcome
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

class PathViewerSolveSuite extends AnyFunSuite {

  System.setProperty("org.graphstream.ui", "swing")

  /** PathViewerFrame is a JFrame; skip when CI runs with -Djava.awt.headless=true. */
  override def withFixture(test: NoArgTest): Outcome = {
    assume(
      !java.awt.GraphicsEnvironment.isHeadless,
      "GUI test skipped in headless environment"
    )
    super.withFixture(test)
  }

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

      frame.useAnimation(false)
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

  test("solveNow ModifiedDijkstra with source 2 marks node 2 as source") {
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      frame.useAnimation(false)
      frame.run(SolveRequest(AlgorithmKind.ModifiedDijkstra, 2, 0, 3))
      val graph = frame.viewer.getGraphicGraph
      assert(graph.getNode("2").getAttribute("ui.class", classOf[String]) == "source")
    } finally {
      frame.dispose()
    }
  }

  test("solveNow Yen source 0 target 2 k 2 leaves node 0 as source") {
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      frame.run(SolveRequest(AlgorithmKind.Yen, 0, 2, 2))
      val graph = frame.viewer.getGraphicGraph
      val cls = graph.getNode("0").getAttribute("ui.class", classOf[String])
      assert(cls != null && cls.contains("source"))
    } finally {
      frame.dispose()
    }
  }

  test("presenting a second graph updates currentGraphIs") {
    val second = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 1.0))
    )
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      assert(frame.currentGraphIs(triangle))
      frame.presentGraph(second, "second", 0)
      assert(!frame.currentGraphIs(triangle))
      assert(frame.currentGraphIs(second))
    } finally {
      Thread.sleep(300)
      frame.dispose()
    }
  }

  test("Compare enables Show other, reports agreement, and toggling redraws the other result") {
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      frame.useAnimation(false)
      val bar = frame.solveBarForTest
      assert(!bar.showOtherEnabled)

      frame.run(SolveRequest(AlgorithmKind.Dijkstra, 0, 2, 3))
      assert(!bar.showOtherEnabled)

      bar.selectCompare(Some(AlgorithmKind.ModifiedDijkstra))
      frame.run(SolveRequest(AlgorithmKind.Dijkstra, 0, 2, 3))
      assert(bar.showOtherEnabled)
      assert(bar.statusText == "Dijkstra vs ModifiedDijkstra: same target cost")

      bar.clickShowOther()
      assert(bar.statusText.startsWith("ModifiedDijkstra 0 → 2"))
      bar.clickShowOther()
      assert(bar.statusText.startsWith("Dijkstra path to 2"))

      frame.presentGraph(triangle, "again", 0)
      assert(!bar.showOtherEnabled)
    } finally {
      Thread.sleep(300)
      frame.dispose()
    }
  }

  /** Subclass so the test can call protected `presentDigraph`. */
  private class Harness extends PathViewerFrame {
    def load(digraph: DirectedGraph): Unit = presentDigraph(digraph, "triangle")
    def presentGraph(digraph: DirectedGraph, title: String, source: Int): Unit =
      presentDigraph(digraph, title, source)
    def useAnimation(on: Boolean): Unit = setAnimate(on)
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
