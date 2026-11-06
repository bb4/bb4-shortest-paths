package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.GraphViewer
import org.graphstream.graph.implementations.MultiGraph
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

/**
 * The mouse-event pump must end when the view closes or the renderer is stopped.
 * Otherwise File → Open leaks a thread still blocked on the previous viewer pipe.
 */
class PathRendererLifecycleSuite extends AnyFunSuite {

  System.setProperty("org.graphstream.ui", "swing")

  test("closing the viewer stops the path renderer pump thread") {
    val graph = lineGraph()
    val viewer = new GraphViewer(graph)
    viewer.getViewPanel
    val renderer = ShortestPathRenderer(
      graph,
      ShortestPathsSolution(0.0, List(Path(0.0, List(0)))),
      viewer
    )

    renderer.render()
    val pump = waitForPump()
    assert(pump.isAlive)

    // Same signal DefaultView sends when the view closes. viewer.close() can
    // throw before that signal if the surface was never painted.
    viewer.getGraphicGraph.setAttribute("ui.viewClosed", viewer.getDefaultView.getIdView)

    val stopped = waitUntil(!pump.isAlive)
    assert(stopped, s"pump thread ${pump.getName} still alive after viewClosed:\n${dump(pump)}")
  }

  test("stop unblocks the pump while it is waiting for mouse events") {
    val graph = lineGraph()
    val viewer = new GraphViewer(graph)
    viewer.disableAutoLayout()
    viewer.getViewPanel
    val renderer = ShortestPathRenderer(
      graph,
      ShortestPathsSolution(0.0, List(Path(0.0, List(0)))),
      viewer
    )

    renderer.render()
    val pump = waitForPump()
    assert(pump.isAlive)
    assert(waitUntil(isParked(pump)), s"pump never parked in blockingPump:\n${dump(pump)}")
    Thread.sleep(400)
    assert(isParked(pump), s"pump left blockingPump before stop:\n${dump(pump)}")

    renderer.stop()

    val stopped = waitUntil(!pump.isAlive, 400)
    assert(stopped, s"pump thread ${pump.getName} still blocked after stop:\n${dump(pump)}")
  }

  private def lineGraph(): MultiGraph = {
    val graph = new MultiGraph("lifecycle")
    graph.setAutoCreate(true)
    graph.addNode("0")
    graph.addNode("1")
    graph.addEdge("0-1", "0", "1", true)
    graph
  }

  private def waitForPump(): Thread = {
    val deadline = System.nanoTime() + 3_000_000_000L
    var found: Thread = null
    while (found == null && System.nanoTime() < deadline) {
      found = Thread.getAllStackTraces.keySet.asScala.find(isPump).orNull
      if (found == null) Thread.sleep(20)
    }
    assert(found != null, s"pump thread never started:\n${dumpAll()}")
    found
  }

  private def waitUntil(predicate: => Boolean, timeoutMs: Long = 3000): Boolean = {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000L
    while (!predicate && System.nanoTime() < deadline) Thread.sleep(20)
    predicate
  }

  private def isPump(thread: Thread): Boolean =
    thread.getName == "path-renderer-pump" && thread.isAlive

  private def isParked(thread: Thread): Boolean =
    thread.isAlive && thread.getStackTrace.exists { frame =>
      frame.getClassName.contains("ConditionObject") || frame.getMethodName == "await"
    }

  private def dump(thread: Thread): String =
    thread.getStackTrace.mkString("\n")

  private def dumpAll(): String =
    Thread.getAllStackTraces.asScala.map { case (thread, stack) =>
      s"${thread.getName}\n${stack.mkString("\n")}"
    }.mkString("\n---\n")
}
