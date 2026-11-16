package com.barrybecker4.discreteoptimization.traffic.demo

import org.graphstream.graph.Graph
import org.graphstream.ui.view.{Viewer, ViewerPipe}
import com.barrybecker4.graph.visualization.render.GraphViewerPipe
import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSpriteGenerator
import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficGraphUtil.sleep
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.IntersectionSubGraph
import com.barrybecker4.discreteoptimization.traffic.demo.TrafficOrchestrator.DELTA_TIME_SECS


object TrafficOrchestrator {
  private val DELTA_TIME_SECS = 0.02
}

class TrafficOrchestrator(graph: Graph, numSprites: Int, initialSpeed: Double,
                          intersectionSubGraphs: IndexedSeq[IntersectionSubGraph], viewerPipe: ViewerPipe) {
  final private val viewerListener = new ViewerAdapter
  final private val spriteGenerator: VehicleSpriteGenerator = new VehicleSpriteGenerator(numSprites, initialSpeed)

  def run(): Unit = {
    val pipeIn = new GraphViewerPipe("my pipe", viewerPipe)
    pipeIn.addViewerListener(viewerListener)

    try {
      spriteGenerator.addSprites(graph)
      simulateTrafficFlow(pipeIn)
    }
    catch
      case e: Exception => throw new IllegalStateException(e)
  }

  private def simulateTrafficFlow(pipeIn: ViewerPipe): Unit = {
    while (viewerListener.isLooping) {
      //pipeIn.pump()
      intersectionSubGraphs.foreach(intersectionSubGraph => intersectionSubGraph.update(DELTA_TIME_SECS, spriteGenerator.getSpriteManager))
      spriteGenerator.moveSprites(DELTA_TIME_SECS)
      sleep(2)
    }
  }
}
