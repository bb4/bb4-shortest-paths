package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.render.UiClass
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.pathviewer.render.PathRenderer.ANIMATION_DELAY
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.view.Viewer


object ShortestPathRenderer {
  def distanceText(solution: ShortestPathsSolution, nodeIdx: Int): Option[String] = {
    val ending = solution.paths.filter(path => path.nodes.nonEmpty && path.lastNode == nodeIdx)
    if (ending.nonEmpty) {
      val finite = ending.filterNot(_.weight.isPosInfinity)
      if (finite.isEmpty) Some("unreachable")
      else {
        val formatted = f"${finite.minBy(_.weight).weight}%.4g"
        Some(formatted.replaceFirst("""(\.\d*?[1-9])0+(e.*)?$""", "$1$2")
          .replaceFirst("""\.0+(e.*)?$""", "$1"))
      }
    } else {
      solution.paths.lift(nodeIdx)
        .filter(path => path.nodes.isEmpty || path.weight.isPosInfinity)
        .map(_ => "unreachable")
    }
  }
}

case class ShortestPathRenderer(graph: MultiGraph, solution: ShortestPathsSolution, viewer: Viewer)
  extends PathRenderer(graph, viewer) {

  override def distanceText(nodeIdx: Int): Option[String] =
    ShortestPathRenderer.distanceText(solution, nodeIdx)

  override def colorPaths(nodeIdx: Int, uiClass: UiClass): Unit = {
    val path = getPath(nodeIdx)
    colorPath(path, uiClass, 0)
  }

  override def colorPaths(nodeIdx1: Int, nodeIdx2: Int, uiClass: UiClass): Unit = {
    val path = getPath(nodeIdx1, nodeIdx2)
    colorPath(path, uiClass, 0)
  }

  def colorPath(path: Path, uiClass: UiClass, animationDelay: Int = ANIMATION_DELAY): Unit =
    walkPath(path, animationDelay) { (node, _, isLast) =>
      val name = if (isLast && uiClass.isHighlight) "last" else uiClass.name
      setNodeClass(node, name)
    } { edge =>
      edge.setAttribute("ui.class", uiClass.name)
    }

  private def getPath(nodeIdx: Int): Path = {
    val optionalPath = solution.paths.find(path => path.nodes.nonEmpty && path.lastNode == nodeIdx)
    if (optionalPath.isEmpty) {
      println("There is no path to node " + nodeIdx)
      Path.EMPTY_PATH
    }
    else optionalPath.get
  }

  private def getPath(nodeIdx1: Int, nodeIdx2: Int): Path = {
    val paths = solution.paths.filter(path => path.nodes.nonEmpty && (path.lastNode == nodeIdx1 || path.lastNode == nodeIdx2))
    if (paths.isEmpty) {
      println("There is no path to node " + nodeIdx1 + " or " + nodeIdx2)
      return Path.EMPTY_PATH
    }
    else if (paths.size < 2) {
      println("There is no shortest path that includes both " + nodeIdx1 + " and " + nodeIdx2)
      return Path.EMPTY_PATH
    }

    val shorterPath = paths.minBy(p => p.nodes.size)
    val longerPath = paths.maxBy(p => p.nodes.size)
    if (longerPath.nodes(longerPath.nodes.size - 2) != shorterPath.nodes.last) {
      println("Node " + nodeIdx1 + " and " + nodeIdx2 + " no not connect via a shortest edge")
      Path.EMPTY_PATH
    }
    else longerPath
  }
}
