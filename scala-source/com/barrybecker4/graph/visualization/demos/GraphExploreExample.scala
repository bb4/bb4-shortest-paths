package com.barrybecker4.graph.visualization.demos

import com.barrybecker4.graph.visualization.demos.GraphExploreExample.STYLE_SHEET
import org.graphstream.graph.*
import org.graphstream.graph.implementations.*

import scala.jdk.CollectionConverters.*


object GraphExploreExample {

  private val STYLE_SHEET: String = "node {" + "	fill-color: black;" + "}" + "node.marked {" + "	fill-color: red;" + "}"
  def main(args: Array[String]): Unit = {
    new GraphExploreExample
  }
}

class GraphExploreExample {
  System.setProperty("org.graphstream.ui", "swing")
  val graph = new SingleGraph("tutorial 1")
  graph.setAttribute("ui.stylesheet", STYLE_SHEET)
  graph.setAutoCreate(true)
  graph.setStrict(false)
  graph.display
  graph.addEdge("AB", "A", "B")
  graph.addEdge("BC", "B", "C")
  graph.addEdge("CA", "C", "A")
  graph.addEdge("AD", "A", "D")
  graph.addEdge("DE", "D", "E")
  graph.addEdge("DF", "D", "F")
  graph.addEdge("EF", "E", "F")

  for (node <- graph.iterator().asScala) {
    node.setAttribute("ui.label", node.getId)
  }
  explore(graph.getNode("A"))

  def explore(source: Node): Unit = {
    val k = source.getBreadthFirstIterator
    while (k.hasNext) {
      val next = k.next
      next.setAttribute("ui.class", "marked")
      sleep()
    }
  }

  protected def sleep(): Unit = {
    try Thread.sleep(1000)
    catch {
      case e: Exception =>
    }
  }
}