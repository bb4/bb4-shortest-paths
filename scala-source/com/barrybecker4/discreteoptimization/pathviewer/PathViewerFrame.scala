package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.graph.directed.DirectedGraphParser
import com.barrybecker4.graph.visualization.{GraphStreamAdapter, GraphViewerFrame}
import com.barrybecker4.discreteoptimization.kshortestpaths.KShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.discreteoptimization.pathviewer.PathViewerFrame.{K_SHORTEST_PATHS_PREFIX, SHORTEST_PATHS_PREFIX}
import com.barrybecker4.discreteoptimization.pathviewer.render.{AnimatedShortestPathRenderer, KShortestPathRenderer, PathColors, PathRenderer, PlaybackControl, ShortestPathRenderer}
import com.barrybecker4.discreteoptimization.shortestpaths.ShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.solver.{DijkstrasPathSolver, ModifiedDijkstrasPathSolver}
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.DirectedGraph
import org.graphstream.graph.Graph
import org.graphstream.graph.implementations.MultiGraph

import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.{CancellationException, ExecutionException}
import javax.swing.*


/** Draws the shortest paths and allows interacting with them.
 */
object PathViewerFrame {
  private val SHORTEST_PATHS_PREFIX = "scala-test/com/barrybecker4/discreteoptimization/shortestpaths/solver/data/"
  private val K_SHORTEST_PATHS_PREFIX = "scala-test/com/barrybecker4/discreteoptimization/kshortestpaths/solver/data/"
  private val PARSER: DirectedGraphParser = DirectedGraphParser()
}

class PathViewerFrame extends GraphViewerFrame() {

  private var activeRenderer: Option[PathRenderer] = None
  private var loaded: Option[DirectedGraph] = None
  private val solveBar = new SolveBar(solve, _ => ())
  @volatile private var lastSummary = "Open a graph, then Solve"
  getContentPane.add(solveBar, BorderLayout.NORTH)
  solveBar.onShowOther(() => toggleShown())

  private case class Prepared(
      digraph: DirectedGraph,
      request: SolveRequest,
      result: SolveResult,
      other: Option[(SolveRequest, SolveResult)],
      summary: String
  )

  private var shown: Option[Prepared] = None
  private var showingSecond = false

  private[pathviewer] def loadedGraph: Option[DirectedGraph] = loaded

  private[pathviewer] def currentGraphIs(digraph: DirectedGraph): Boolean = loaded.contains(digraph)

  override protected def setGraph(graph: Graph, title: String): Unit = {
    stopActiveRenderer()
    solveBar.setLegend(Seq.empty)
    super.setGraph(graph, title)
  }

  override def dispose(): Unit = {
    stopActiveRenderer()
    super.dispose()
  }

  private def stopActiveRenderer(): Unit = {
    activeRenderer.foreach(_.stop())
    activeRenderer = None
  }

  override protected def createMenu(): Unit = {
    val myMenuBar: JMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openGraphItem = createOpenItemOption()
    val openShortestPathsItem = createOpenShortestPathsItemOption()
    val openKShortestPathsItem = createOpenKShortestPathsItemOption()
    fileMenu.add(openGraphItem)
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

      val solution = ShortestPathsTstUtil.getSolution(selectedFile.getName)
      val graph = loadTheGraph(selectedFile, sourceVertex(solution))

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

      val solution = KShortestPathsTstUtil.getSolution(selectedFile.getName)
      val graph = loadTheGraph(selectedFile, sourceVertex(solution))

      // then load the shortest paths
      showKShortestPaths(solution, graph)
    }
  }

  private def loadTheGraph(file: File, sourceVertex: Int): MultiGraph = {
    val graphName = getGraphName(file.getName)
    val digraph = loadGraphFromName(graphName)
    loaded = Some(digraph)
    clearShown()
    solveBar.setCompareEnabled(true)
    solveBar.setVertexRange(LiveSolver.maxVertexId(digraph))
    solveBar.setSource(sourceVertex)
    solveBar.setAnimateDefault(digraph.edges.size <= GraphStreamAdapter.LARGE_GRAPH_THRESH)
    val graph = GraphStreamAdapter(digraph).createGraph(sourceVertex)
    setGraph(graph, file.getName)
    graph
  }

  override protected def presentDigraph(
    digraph: DirectedGraph,
    title: String,
    sourceVertex: Int = 0
  ): Unit = {
    loaded = Some(digraph)
    clearShown()
    solveBar.setCompareEnabled(true)
    solveBar.setVertexRange(LiveSolver.maxVertexId(digraph))
    solveBar.setSource(sourceVertex)
    solveBar.setAnimateDefault(digraph.edges.size <= GraphStreamAdapter.LARGE_GRAPH_THRESH)
    super.presentDigraph(digraph, title, sourceVertex)
  }

  private[pathviewer] def solve(request: SolveRequest): Unit = {
    val digraph = loaded match {
      case None =>
        setSummary("Open a graph first")
        return
      case Some(graph) => graph
    }
    LiveSolver.validate(digraph, request) match {
      case Left(message) =>
        setSummary(message)
      case Right(valid) =>
        solveBar.setBusy(true)
        val second = secondRequest(valid) // read Swing state on the EDT, before the worker starts
        val worker = new SwingWorker[Prepared, java.lang.Void] {
          override def doInBackground(): Prepared = prepare(digraph, valid, second)
          override def done(): Unit = {
            val discardMsg = "Solve discarded because the graph changed"
            try {
              val prepared = get()
              if (!currentGraphIs(digraph)) setSummary(discardMsg)
              else publishPrepared(prepared)
            } catch {
              case _: CancellationException => ()
              case _: InterruptedException =>
                Thread.currentThread().interrupt()
              case ex: ExecutionException =>
                setSummary(ex.getCause.toString)
              case ex: Exception =>
                setSummary(ex.toString)
            } finally {
              solveBar.setBusy(false)
            }
          }
        }
        worker.execute()
    }
  }

  private[pathviewer] def solveNow(request: SolveRequest): Unit = {
    val digraph = loaded.getOrElse(throw new IllegalStateException("Open a graph first"))
    val valid = LiveSolver.validate(digraph, request)
      .fold(message => throw new IllegalArgumentException(message), identity)
    publishPrepared(prepare(digraph, valid, secondRequest(valid)))
    waitForSourceInViewer(valid.source)
  }

  private def secondRequest(first: SolveRequest): Option[SolveRequest] =
    solveBar.compareAlgorithm().map(kind => first.copy(algorithm = kind))

  /** Safe off the EDT: touches no Swing state. `second` is read from the Solve bar on the EDT. */
  private def prepare(digraph: DirectedGraph, valid: SolveRequest, second: Option[SolveRequest]): Prepared =
    second match {
      case Some(other) if other.algorithm != valid.algorithm =>
        LiveSolver.validate(digraph, other) match {
          case Left(message) =>
            Prepared(digraph, valid, LiveSolver.solve(digraph, valid), None, s"Compare skipped: $message")
          case Right(validOther) =>
            val compared = LiveSolver.compare(digraph, valid, validOther)
            val agreement = if (compared.sameTargetCost) "same target cost" else "target costs differ"
            Prepared(
              digraph,
              valid,
              compared.first,
              Some((validOther, compared.second)),
              s"${valid.algorithm} vs ${validOther.algorithm}: $agreement"
            )
        }
      case _ =>
        Prepared(digraph, valid, LiveSolver.solve(digraph, valid), None, "")
    }

  private def publishPrepared(prepared: Prepared): Unit = {
    showingSecond = false
    shown = prepared.other.map(_ => prepared)
    solveBar.setShowOtherEnabled(prepared.other.isDefined)
    showSolved(prepared.digraph, prepared.request, prepared.result)
    if (prepared.summary.nonEmpty) setSummary(prepared.summary)
  }

  private def clearShown(): Unit = {
    shown = None
    showingSecond = false
    solveBar.setShowOtherEnabled(false)
  }

  private[pathviewer] def toggleShown(): Unit =
    shown.foreach { prepared =>
      if (!currentGraphIs(prepared.digraph)) clearShown()
      else {
        val (request, result) = prepared.other match {
          case Some((otherRequest, otherResult)) if !showingSecond => (otherRequest, otherResult)
          case _ => (prepared.request, prepared.result)
        }
        showingSecond = prepared.other.isDefined && !showingSecond
        showSolved(prepared.digraph, request, result)
      }
    }

  private[pathviewer] def solveBarForTest: SolveBar = solveBar

  private def waitForSourceInViewer(source: Int): Unit = {
    val deadline = System.nanoTime() + 3_000_000_000L
    var ready = false
    while (!ready && System.nanoTime() < deadline) {
      val node = viewer.getGraphicGraph.getNode(source.toString)
      ready = node != null && node.getAttribute("ui.class", classOf[String]) == "source"
      if (!ready) Thread.sleep(1)
    }
  }

  private def showSolved(digraph: DirectedGraph, request: SolveRequest, result: SolveResult): Unit = {
    val stream = GraphStreamAdapter(digraph).createGraph(request.source)
    setGraph(stream, titleFor(request))
    result match {
      case SolveResult.ToAll(solution, trace) =>
        showShortestPaths(solution, stream, trace)
        setSummary(dijkstraStatus(request, solution))
      case SolveResult.OnePath(path, trace) =>
        val solution = ShortestPathsSolution(path.weight, List(path))
        showShortestPaths(solution, stream, trace)
        setSummary(onePathStatus(request, path))
      case SolveResult.KPaths(solution) =>
        showKShortestPaths(solution, stream)
        setSummary(f"Yen k=${solution.k} total ${solution.totalCost}%.4g")
    }
  }

  private def showShortestPaths(
      solution: ShortestPathsSolution,
      graph: MultiGraph,
      trace: com.barrybecker4.graph.algorithms.shortestpaths.SearchTrace
  ): Unit = {
    if (solveBar.animateSelected && trace.events.nonEmpty) {
      solveBar.setLegend(Seq.empty)
      val control = new PlaybackControl
      solveBar.bindPlayback(control)
      activate(new AnimatedShortestPathRenderer(graph, solution, viewer, trace, control))
    } else showShortestPaths(solution, graph)
  }

  private def titleFor(request: SolveRequest): String =
    s"${request.algorithm} ${request.source} → ${request.target}"

  private def dijkstraStatus(request: SolveRequest, solution: ShortestPathsSolution): String = {
    val toTarget = solution.paths.find(path => path.nodes.nonEmpty && path.lastNode == request.target)
    val targetText = toTarget match {
      case Some(path) if !path.weight.isPosInfinity => f"path to ${request.target}: ${path.weight}%.4g"
      case _ => s"no path to ${request.target}"
    }
    s"Dijkstra $targetText"
  }

  private def onePathStatus(request: SolveRequest, path: Path): String =
    if (path.nodes.isEmpty || path.weight.isPosInfinity)
      s"Modified: no path ${request.source} → ${request.target}"
    else
      f"Modified ${request.source} → ${request.target}: ${path.weight}%.4g ${path.nodes.mkString(" ")}"

  private def sourceVertex(solution: ShortestPathsSolution): Int =
    solution.paths.zipWithIndex.collectFirst {
      case (path, vertex) if path.weight == 0.0 && path.nodes == List(vertex) => vertex
    }.getOrElse(0)

  private def sourceVertex(solution: KShortestPathsSolution): Int =
    solution.shortestPaths.view.flatMap(_.nodes.headOption).headOption.getOrElse(0)

  protected def showShortestPaths(solution: ShortestPathsSolution, graph: MultiGraph): Unit = {
    solveBar.setLegend(Seq.empty)
    activate(ShortestPathRenderer(graph, solution, viewer))
  }

  protected def showKShortestPaths(solution: KShortestPathsSolution, graph: MultiGraph): Unit = {
    solveBar.setLegend(solution.shortestPaths.indices.map(index => (index + 1, PathColors.getColor(index))))
    activate(KShortestPathRenderer(graph, solution, viewer))
  }

  private[pathviewer] def setAnimate(on: Boolean): Unit = solveBar.setAnimateDefault(on)

  private def activate(renderer: PathRenderer): Unit = {
    renderer.statusListener = text =>
      SwingUtilities.invokeLater(() => solveBar.setStatus(if (text.isEmpty) lastSummary else text))
    activeRenderer = Some(renderer)
    renderer.render()
  }

  private def setSummary(text: String): Unit = {
    lastSummary = text
    solveBar.setStatus(text)
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
