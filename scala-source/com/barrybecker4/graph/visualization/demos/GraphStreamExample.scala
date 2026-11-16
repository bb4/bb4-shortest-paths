package com.barrybecker4.graph.visualization.demos

import org.graphstream.graph.implementations.*


object GraphStreamExample {
  def main(args: Array[String]): Unit = {
    System.setProperty("org.graphstream.ui", "swing")
    val graph = new SingleGraph("Tutorial 1")
    graph.addNode("A")
    graph.addNode("B")
    graph.addNode("C")
    graph.addEdge("AB", "A", "B")
    graph.addEdge("BC", "B", "C")
    graph.addEdge("CA", "C", "A")
    graph.display
  }
}