package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.graph.directed.DirectedGraphParser
import com.barrybecker4.graph.visualization.{GraphStreamAdapter, GraphViewerFrame}
import com.barrybecker4.discreteoptimization.kshortestpaths.KShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.discreteoptimization.pathviewer.PathViewerFrame.{K_SHORTEST_PATHS_PREFIX, SHORTEST_PATHS_PREFIX}
import com.barrybecker4.discreteoptimization.pathviewer.render.ShortestPathRenderer
import com.barrybecker4.discreteoptimization.pathviewer.render.KShortestPathRenderer
import com.barrybecker4.discreteoptimization.shortestpaths.ShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.solver.{DijkstrasPathSolver, ModifiedDijkstrasPathSolver}
import org.graphstream.graph.implementations.MultiGraph

import java.io.File
import javax.swing.*


/** Draws the shortest paths and allows interacting with them.
 */
object PathViewerFrame {
  private val SHORTEST_PATHS_PREFIX = "scala-test/com/barrybecker4/discreteoptimization/shortestpaths/solver/data/"
  private val K_SHORTEST_PATHS_PREFIX = "scala-test/com/barrybecker4/discreteoptimization/kshortestpaths/solver/data/"
  private val PARSER: DirectedGraphParser = DirectedGraphParser()
}

class PathViewerFrame extends GraphViewerFrame() {

  override protected def createMenu(): Unit = {
    val myMenuBar: JMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openShortestPathsItem = createOpenShortestPathsItemOption()
    val openKShortestPathsItem = createOpenKShortestPathsItemOption()
    fileMenu.add(openShortestPathsItem)
    fileMenu.add(openKShortestPathsItem)
    myMenuBar.add(fileMenu)
    setJMenuBar(myMenuBar)
  }
  private def createOpenShortestPathsItemOption(): JMenuItem = {
    val openItem = new JMenuItem("Open Shortest Paths")
    openItem.addActionListener(_ => loadShortestPaths())
    openItem
  }

  private def createOpenKShortestPathsItemOption(): JMenuItem = {
    val openItem = new JMenuItem("Open K Shortest Paths")
    openItem.addActionListener(_ => loadKShortestPaths())
    openItem
  }

  private def loadShortestPaths(): Unit = {
    val fileChooser = new JFileChooser()
    fileChooser.setCurrentDirectory(new File(SHORTEST_PATHS_PREFIX))

    val returnValue = fileChooser.showOpenDialog(PathViewerFrame.this)
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      val selectedFile = fileChooser.getSelectedFile
      println("selected file is " + selectedFile.getName)

      val graph = loadTheGraph(selectedFile)
      val solution = ShortestPathsTstUtil.getSolution(selectedFile.getName)

      // then load the shortest paths
      showShortestPaths(solution, graph)
    }
  }

  private def loadKShortestPaths(): Unit = {
    val fileChooser = new JFileChooser()
    fileChooser.setCurrentDirectory(new File(K_SHORTEST_PATHS_PREFIX))

    val returnValue = fileChooser.showOpenDialog(PathViewerFrame.this)
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      val selectedFile = fileChooser.getSelectedFile
      println("selected file is " + selectedFile.getName)

      val graph = loadTheGraph(selectedFile)
      val solution = KShortestPathsTstUtil.getSolution(selectedFile.getName)

      // then load the shortest paths
      showKShortestPaths(solution, graph)
    }
  }

  private def loadTheGraph(file: File): MultiGraph = {
    val graphName = getGraphName(file.getName)
    val digraph = loadGraphFromName(graphName)
    val graph = GraphStreamAdapter(digraph).createGraph()
    setGraph(graph, file.getName)
    graph
  }

  private def showShortestPaths(solution: ShortestPathsSolution, graph: MultiGraph): Unit = {

    ShortestPathRenderer(graph, solution, viewer).render()
  }

  private def showKShortestPaths(solution: KShortestPathsSolution, graph: MultiGraph): Unit = {

    KShortestPathRenderer(graph, solution, viewer).render()
  }

  private def getGraphName(fileName: String): String = {
    val start = getStartIndex(fileName)
    fileName.substring(0, start)
  }

  private def getStartIndex(fileName: String): Int = {
    val suffixes = Seq(
      s"_${ModifiedDijkstrasPathSolver.BASE_NAME}_solution",
      s"_${DijkstrasPathSolver.BASE_NAME}_solution",
      s"_${YensKPathsSolver.BASE_NAME}_solution"
    )
    suffixes.map(fileName.indexOf).find(_ >= 0)
      .getOrElse(throw new IllegalArgumentException("Invalid fileName: " + fileName))
  }
}
