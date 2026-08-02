package com.barrybecker4.graph.visualization.demos

import com.barrybecker4.graph.visualization.demos.LeHavreMap
import com.barrybecker4.graph.visualization.demos.LeHavreMap.{PATH_PREFIX, STYLE_SHEET}
import org.graphstream.graph.*
import org.graphstream.graph.implementations.*
import org.graphstream.ui.swing_viewer.ViewPanel
import org.graphstream.ui.view.Viewer

object LeHavreMap {

  private val STYLE_SHEET: String = """
      |node {
      |	size: 1px;
      |	fill-color: #777;
      |	text-mode: hidden;
      |	z-index: 0;
      |}
      |
      |edge {
      |	shape: line;
      |	fill-mode: dyn-plain;
      |	fill-color: #222, #555, green, yellow;
      |	arrow-size: 3px, 2px;
      |}
      |edge.tollway { size: 2px; stroke-color: red; stroke-width: 1px; stroke-mode: plain; }
      |edge.tunnel { stroke-color: blue; stroke-width: 1px; stroke-mode: plain; }
      |edge.bridge { stroke-color: yellow; stroke-width: 1px; stroke-mode: plain; }
      |""".stripMargin
  private val PATH_PREFIX = "scala-source/com/barrybecker4/graph/visualization/demos/"

  def main(args: Array[String]): Unit = {
    System.setProperty("org.graphstream.ui", "swing")
    new LeHavreMap
  }
}

class LeHavreMap {
  val graph = new MultiGraph("Le Havre")
  try graph.read(PATH_PREFIX + "LeHavre.dgs")
  catch {
    case e: Exception =>
      e.printStackTrace()
      System.exit(1)
  }
  graph.edges.forEach(edge => {
    if (edge.hasAttribute("isTollway")) edge.setAttribute("ui.class", "tollway")
    else if (edge.hasAttribute("isTunnel")) edge.setAttribute("ui.class", "tunnel")
    else if (edge.hasAttribute("isBridge")) edge.setAttribute("ui.class", "bridge")

    val speedMax: Float = edge.getNumber("speedMax").toFloat / 130.0f
    edge.setAttribute("ui.color", speedMax)
  })
  graph.setAttribute("ui.stylesheet", STYLE_SHEET)

  val viewer: Viewer = graph.display(false)

  val view: ViewPanel = viewer.getDefaultView.asInstanceOf[ViewPanel] // ViewPanel is the view for gs-ui-swing
  view.resizeFrame(1100, 900)

  graph.setAttribute("ui.screenshot", PATH_PREFIX + "lehavre-screenshot.png")
}