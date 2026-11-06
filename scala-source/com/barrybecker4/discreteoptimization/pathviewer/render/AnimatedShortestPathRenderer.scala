package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.discreteoptimization.pathviewer.render.PathRenderer.ANIMATION_DELAY
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.algorithms.shortestpaths.SearchTrace
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.view.Viewer

class AnimatedShortestPathRenderer(
    graph: MultiGraph,
    solution: ShortestPathsSolution,
    viewer: Viewer,
    trace: SearchTrace,
    control: PlaybackControl
) extends ShortestPathRenderer(graph, solution, viewer) {

  override protected def initialAnimation(): Unit = {
    val player = new FrontierPlayer(graph)
    for (event <- trace.events if !control.shouldStop) {
      if (control.isPaused) control.awaitAdvance(60_000)
      if (!control.shouldStop) {
        player.apply(event)
        viewerPipe.pump()
        if (!control.isPaused) Thread.sleep(ANIMATION_DELAY.toLong)
      }
    }
  }
}
