package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.render.UiClass
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.pathviewer.render.PathRenderer.{ANIMATION_DELAY, PAUSE}
import com.barrybecker4.graph.visualization.render.UiClass.*
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.{Edge, Node}
import org.graphstream.ui.view.{Viewer, ViewerPipe}

import java.awt.Color


object ShortestPathRenderer {
  private val ANIMATION_DELAY = 20
  private val PAUSE = 100
}

case class ShortestPathRenderer(graph: MultiGraph, solution: ShortestPathsSolution, viewer: Viewer)
  extends PathRenderer(graph, viewer) {

  override def colorPaths(nodeIdx: Int, uiClass: UiClass): Unit = {
    val path = getPath(nodeIdx)
    colorPath(path, uiClass, 0)
  }

  override def colorPaths(nodeIdx1: Int, nodeIdx2: Int, uiClass: UiClass): Unit = {
    val path = getPath(nodeIdx1, nodeIdx2)
    colorPath(path, uiClass, 0)
  }

  def colorPath(path: Path, uiClass: UiClass, animationDelay: Int = ANIMATION_DELAY): Unit = {

    if (path.nodes.size > 1) {
      var prevNode: Node = null
      var nextNode: Node = null
      val lastNodeIdx = path.lastNode

      for (nodeIdx <- path.nodes) {
        val nextNode = graph.getNode(nodeIdx)
        val leavingEdge: Edge =
          if (prevNode != null) prevNode.leavingEdges().filter(e => e.getNode1 == nextNode).findFirst().get()
          else null
        if (nodeIdx == lastNodeIdx && uiClass.isHighlight)
          nextNode.setAttribute("ui.class", "last")
        else nextNode.setAttribute("ui.class", uiClass.name)

        if (leavingEdge != null) {
          leavingEdge.setAttribute("ui.class", uiClass.name)
        }
        prevNode = nextNode
        if (animationDelay > 0) {
          viewerPipe.pump()
          Thread.sleep(animationDelay)
        }
      }
      if (animationDelay == 0) viewerPipe.pump()
    }
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
