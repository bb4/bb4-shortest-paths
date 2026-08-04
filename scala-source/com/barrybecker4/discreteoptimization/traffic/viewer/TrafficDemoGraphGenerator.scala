package com.barrybecker4.discreteoptimization.traffic.viewer

import TrafficGraphGenerator.loadStyleSheet
import org.graphstream.graph.Edge
import org.graphstream.graph.implementations.MultiGraph

import scala.io.Source
import scala.util.Using
import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficGraphUtil.addEdgeLengths


object TrafficGraphGenerator {
  private val STYLE_SHEET_PATH = "scala-source/com/barrybecker4/discreteoptimization/traffic/viewer/adapter/traffic.css"

  private def loadStyleSheet(): String = {
    Using(Source.fromFile(STYLE_SHEET_PATH)) { source => source.mkString }
      .getOrElse(throw new RuntimeException(s"Failed to read the style sheet from $STYLE_SHEET_PATH"))
  }
}

class TrafficGraphGenerator {

  def generateGraph(): MultiGraph = {
    val graph = new MultiGraph("TestSprites")
    graph.setAttribute("ui.default.title", "Test Many Sprites")
    graph.setAttribute("ui.antialias")
    populateGraph(graph)
    graph.setAttribute("ui.quality")
    graph.setAttribute("ui.stylesheet", loadStyleSheet())
    graph
  }

  private def populateGraph(graph: MultiGraph): Unit = {
    graph.addNode("A")
    graph.addNode("B")
    graph.addNode("C")
    graph.addEdge("AB", "A", "B", true)
    graph.addEdge("BA", "B", "A", true)
    graph.addEdge("BC", "B", "C", true)
    graph.addEdge("CB", "C", "B", true)
    graph.addEdge("CA", "C", "A", true)
    graph.addEdge("AC", "A", "C", true)
    // Replacing node D with the modelling of an intersection
    graph.addNode("D1")
    graph.addNode("D2")
    graph.addNode("D3")
    graph.addNode("D4")
    graph.addNode("D5")
    graph.addNode("D6")
    graph.getNode("A").setAttribute("xyz", -1500d, -1100d, 0.0)
    graph.getNode("B").setAttribute("xyz", 1500d, -1100d, 0.0)
    graph.getNode("C").setAttribute("xyz", 100d, 1500d, 0.0)
    graph.getNode("D1").setAttribute("xyz", -100d, 300d, 0.0)
    graph.getNode("D2").setAttribute("xyz", 100d, 300d, 0.0)
    graph.getNode("D3").setAttribute("xyz", 250d, -100d, 0.0)
    graph.getNode("D4").setAttribute("xyz", 200d, -300d, 0.0)
    graph.getNode("D5").setAttribute("xyz", -200d, -300d, 0.0)
    graph.getNode("D6").setAttribute("xyz", -250d, -100d, 0.0)
    setEdgePoint(graph.addEdge("CD1", "C", "D1", true), -100.0, 800.0)
    graph.addEdge("D2C", "D2", "C", true)
    setEdgePoint(graph.addEdge("BD3", "B", "D3", true), 1000.0d, -600.0d)
    setEdgePoint(graph.addEdge("D4B", "D4", "B", true), 800.0d, -700.0d)
    setEdgePoint(graph.addEdge("AD5", "A", "D5", true), -800.0d, -600.0d)
    setEdgePoint(graph.addEdge("D6A", "D6", "A", true), -800.0d, -300.0d)
    setEdgePoint(graph.addEdge("D1D6", "D1", "D6", true), -100.0d, 100.0d)
    setEdgePoint(graph.addEdge("D1D4", "D1", "D4", true), 0.0d, -0.0d)
    setEdgePoint(graph.addEdge("D5D2", "D5", "D2", true), 0.0d, 0.0d)
    setEdgePoint(graph.addEdge("D3D2", "D3", "D2", true), 100.0d, 100.0d)
    setEdgePoint(graph.addEdge("D3D6", "D3", "D6", true), 0.0d, -0.0d)
    setEdgePoint(graph.addEdge("D5D4", "D5", "D4", true), 0.0d, -240.0d)
    addEdgeLengths(graph)
    // showNodeLabels(graph)
  }

  // The control point for the edge
  private def setEdgePoint(edge: Edge, x: Double, y: Double): Unit = {
    val src = edge.getSourceNode.getAttribute("xyz", classOf[Array[AnyRef]])
    val dst = edge.getTargetNode.getAttribute("xyz", classOf[Array[AnyRef]])
    edge.setAttribute("ui.points", src(0), src(1), 0.0, x, y, 0, x, y, 0, dst(0), dst(1), 0)
  }

}
