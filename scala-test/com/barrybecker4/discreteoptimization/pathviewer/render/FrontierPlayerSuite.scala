package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.algorithms.shortestpaths.SearchDirection
import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.*
import org.graphstream.graph.implementations.MultiGraph
import org.scalatest.funsuite.AnyFunSuite

class FrontierPlayerSuite extends AnyFunSuite {

  test("frontier, current, and settled replace each other and keep source") {
    val graph = new MultiGraph("t")
    val source = graph.addNode("0")
    source.setAttribute("ui.class", "source")
    graph.addNode("1")
    val player = new FrontierPlayer(graph)

    player.apply(Frontier(0, 0.0))
    assert(source.getAttribute("ui.class", classOf[String]) == "frontier, source")

    player.apply(Extracted(0))
    assert(source.getAttribute("ui.class", classOf[String]) == "current, source")

    player.apply(Settled(0))
    assert(source.getAttribute("ui.class", classOf[String]) == "visited, source")

    player.apply(Frontier(1, 1.0))
    player.apply(Extracted(1))
    assert(graph.getNode("1").getAttribute("ui.class", classOf[String]) == "current")
    assert(source.getAttribute("ui.class", classOf[String]) == "visited, source")
  }

  test("backward settled uses visitedBack while forward settled uses visited") {
    val graph = new MultiGraph("t")
    graph.addNode("0")
    graph.addNode("1")
    val player = new FrontierPlayer(graph)

    player.apply(Settled(0))
    assert(graph.getNode("0").getAttribute("ui.class", classOf[String]) == "visited")

    player.apply(Settled(1, SearchDirection.Backward))
    assert(graph.getNode("1").getAttribute("ui.class", classOf[String]) == "visitedBack")
  }
}
