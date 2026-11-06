package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.GraphViewer
import com.barrybecker4.graph.visualization.render.UiClass
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.pathviewer.render.PathRenderer.ANIMATION_DELAY
import com.barrybecker4.discreteoptimization.pathviewer.render.PathColors.*
import com.barrybecker4.graph.visualization.render.UiClass.*
import org.graphstream.graph.implementations.MultiGraph

import java.awt.Color


object KShortestPathRenderer {

  /** Paths at `indices`, which need not be contiguous. */
  def pathsAt(paths: Seq[Path], indices: Seq[Int]): Seq[(Path, Int)] =
    indices.map(index => (paths(index), index))
}

case class KShortestPathRenderer(graph: MultiGraph, solution: KShortestPathsSolution, viewer: GraphViewer)
  extends PathRenderer(graph, viewer) {

  override protected def initialAnimation(): Unit = {
    var ct = 0
    for (path <- solution.shortestPaths) {
      colorPath(path, VISITED, ANIMATION_DELAY, Some(getColor(ct)))
      colorPath(path, VISITED, ANIMATION_DELAY, None)
      ct += 1
    }
  }

  override def colorPaths(nodeIdx: Int, uiClass: UiClass): Unit = {
    val pathIndices = getPathIndices(nodeIdx)
    colorPaths(pathIndices, uiClass)
  }

  override def colorPaths(nodeIdx1: Int, nodeIdx2: Int, uiClass: UiClass): Unit = {
    val pathIndices = getPathIndices(nodeIdx1, nodeIdx2)
    colorPaths(pathIndices, uiClass)
  }
  
  private def colorPaths(pathIndices: Seq[Int], uiClass: UiClass): Unit = {
    for ((path, index) <- KShortestPathRenderer.pathsAt(solution.shortestPaths, pathIndices)) {
      if (uiClass == PLAIN || uiClass == LARGE) colorPath(path, uiClass, 0)
      else colorPath(path, uiClass, 0, Some(getColor(index)))
    }
  }

  def colorPath(path: Path, uiClass: UiClass, animationDelay: Int = ANIMATION_DELAY, color: Option[Color] = None): Unit =
    walkPath(path, animationDelay) { (node, _, isLast) =>
      val name = if (isLast && uiClass.isHighlight) "last" else uiClass.name
      setNodeClass(node, name)
    } { edge =>
      edge.setAttribute("ui.class", uiClass.name)
      if (color.isDefined) {
        val c = colorToCss(color.get)
        edge.setAttribute("ui.style", s"fill-color: $c; size: 3;")
      } else {
        edge.setAttribute("ui.style", "size: 2;")
      }
    }

  // Get all the paths that pass through nodeIdx
  private def getPathIndices(nodeIdx: Int): Seq[Int] =
    solution.shortestPaths.zipWithIndex.filter((path, _) => path.containsNode(nodeIdx)).map(_._2)


  private def getPathIndices(nodeIdx1: Int, nodeIdx2: Int): Seq[Int] =
    solution.shortestPaths.zipWithIndex.filter { (path, _) =>
      val nodes = path.nodes
      val containsBoth = path.containsNode(nodeIdx1) && path.containsNode(nodeIdx2)
      containsBoth && (Math.abs(nodes.indexOf(nodeIdx2) - nodes.indexOf(nodeIdx1)) == 1)
    }.map(_._2)

}
