package com.barrybecker4.graph.visualization

import com.barrybecker4.graph.GraphTstUtil
import com.barrybecker4.graph.directed.{DirectedGraph, DirectedGraphParser}
import com.barrybecker4.graph.visualization.GraphViewerFrame.{PARSER, PREFIX}
import com.barrybecker4.graph.visualization.{GraphStreamAdapter, GraphViewer}
import org.graphstream.graph.Graph
import org.graphstream.ui.layout.springbox.implementations.{LinLog, SpringBox}
import org.graphstream.ui.swing_viewer.{SwingViewer, ViewPanel}
import org.graphstream.ui.view.{View, Viewer}

import java.io.File
import javax.swing.*
import scala.compiletime.uninitialized
import scala.io.Source


object GraphViewerFrame {
  private val PREFIX = GraphTstUtil.PREFIX
  private val PARSER: DirectedGraphParser = DirectedGraphParser()
}

class GraphViewerFrame extends JFrame("Graph Viewer") {
  System.setProperty("org.graphstream.ui", "swing")

  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  setSize(1000, 900)

  var viewer: GraphViewer = uninitialized
  createMenu()

  setVisible(true)

  protected def createMenu(): Unit = {
    val myMenuBar: JMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openItem = createOpenItemOption()
    fileMenu.add(openItem)
    myMenuBar.add(fileMenu)
    setJMenuBar(myMenuBar)
  }

  protected def createOpenItemOption(): JMenuItem = {
    val openItem = new JMenuItem("Open Graph")
    openItem.addActionListener(_ => loadGraph())
    openItem
  }

  protected def setGraph(graph: Graph, title: String): Unit = {
    if (viewer != null)
      remove(viewer.getViewPanel)
    viewer = new GraphViewer(graph)
    this.setTitle(title)

    // If the graph nodes have location data, don't use an layout
    if (graph.getNode(0).hasAttribute("xy")) viewer.disableAutoLayout()
    else viewer.enableAutoLayout(SpringBox()) //SpringBox()) // LinLog()

    add(viewer.getViewPanel)
    this.repaint()
    setVisible(true)
  }

  protected def loadGraph(): Unit = {
    val fileChooser = new JFileChooser()
    fileChooser.setCurrentDirectory(new File(PREFIX))

    val returnValue = fileChooser.showOpenDialog(GraphViewerFrame.this)
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      val selectedFile = fileChooser.getSelectedFile
      val digraph = loadGraphFromFile(selectedFile)
      val graph = GraphStreamAdapter(digraph).createGraph()
      setGraph(graph, selectedFile.getName)
    }
  }

  protected def loadGraphFromFile(file: File): DirectedGraph = {
    val name = file.getName
    val source: Source = Source.fromFile(file.getAbsolutePath)
    PARSER.parse(source, name)
  }

  protected def loadGraphFromName(name: String): DirectedGraph = {
    loadGraphFromFile(new File(PREFIX + name))
  }
}


