package com.barrybecker4.discreteoptimization.traffic.viewer.adapter

import com.barrybecker4.discreteoptimization.traffic.graph.TrafficGraph
import org.graphstream.graph.implementations.MultiGraph
import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficGraphUtil.{addEdgeLengths, showNodeLabels}
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.TrafficStreamAdapter.SHOW_LABELS

import scala.io.Source
import scala.util.Using


object TrafficStreamAdapter {
  val LARGE_GRAPH_THRESH = 60
  val COMPUTE_CURVES = false
  val SHOW_LABELS = false
  private val STYLE_SHEET_PATH =
    "scala-source/com/barrybecker4/discreteoptimization/traffic/viewer/adapter/traffic.css"

  private def loadStyleSheet(): String = {
    Using(Source.fromFile(STYLE_SHEET_PATH)) { source => source.mkString }
      .getOrElse(throw new RuntimeException(s"Failed to read the style sheet from $STYLE_SHEET_PATH"))
  }
}

/** Creates a stream graph from TrafficGraph
 */
case class TrafficStreamAdapter(trafficGraph: TrafficGraph) {

  var intersectionSubGraphs: IndexedSeq[IntersectionSubGraph] = IndexedSeq.empty

  def createGraph(): MultiGraph = {
    val graph = new MultiGraph("Some traffic graph")

    intersectionSubGraphs = addIntersectionsToGraph(graph)
    addStreetsToGraph(graph)
    addEdgeLengths(graph)
    if (SHOW_LABELS)
      showNodeLabels(graph)

    graph.setAttribute("ui.stylesheet", TrafficStreamAdapter.loadStyleSheet())
    graph.setAttribute("ui.antialias", true)
    graph
  }

  private def addIntersectionsToGraph(graph: MultiGraph): IndexedSeq[IntersectionSubGraph] = {
    for {
      intersectionId <- 0 until trafficGraph.numIntersections
      intersection = trafficGraph.getIntersection(intersectionId)
    } yield IntersectionSubGraph(intersection, graph)
  }

  private def addStreetsToGraph(graph: MultiGraph): Unit = {
    for (street <- trafficGraph.streets) {
      StreetSubGraph(
        street,
        intersectionSubGraphs(street.intersectionIdx1),
        intersectionSubGraphs(street.intersectionIdx2),
        graph
      )
    }
  }
}
