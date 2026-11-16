package com.barrybecker4.discreteoptimization.traffic.viewer

import com.barrybecker4.discreteoptimization.traffic.demo.TrafficOrchestrator


/**
 * This just shows the hardcoded graph from TrafficGraphGenerator.
 * For general traffic map configurations, use TrafficViewerApp.
 */
object TrafficDemoApplication {
  private val SPRITE_COUNT = 100

  def main(args: Array[String]): Unit = {
    System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display")

    val graph = new TrafficGraphGenerator().generateGraph()
    val pipeIn = graph.display(false).newViewerPipe()
    val initialSpeed = 10.0
    new TrafficOrchestrator(graph, SPRITE_COUNT, initialSpeed, IndexedSeq(), pipeIn).run()
  }
}
