# PathViewer Live Solve Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let PathViewer run Dijkstra, Modified Dijkstra, or Yen on the loaded graph, with a chosen source, target, and `k`, then animate Dijkstra’s frontier and compare two algorithms on that same graph.

**Architecture:** A pure `LiveSolver` turns a `DirectedGraph` plus a `SolveRequest` into a `SolveResult`. Dijkstra and the single s–t Modified Dijkstra search record a `SearchTrace` through a no-op-by-default `SearchObserver`. `PathViewerFrame` keeps the loaded digraph, collects the request from a north control bar, and renders with the existing `ShortestPathRenderer` / `KShortestPathRenderer`. Frontier playback runs on the existing renderer pump thread before hover listening. Compare stores two results and swaps which one is shown.

**Tech Stack:** Scala 3, ScalaTest `AnyFunSuite`, Swing, GraphStream 2.0, Gradle (`./gradlew test --tests <suite>`).

## Global Constraints

- Issue 18: menu or sidebar to select algorithm, source, target (for s–t / k-paths), and `k`; run the solver on the in-memory `DirectedGraph`; still allow opening golden files; optional stepped animation of settled vs frontier vs current extract-min; distance labels on hover; k-path legend mapping color → rank; side-by-side or toggle compare of two algorithms.
- Compare is a toggle on the one existing GraphStream view. Do not add a second viewer window.
- `k` in the UI is an integer from 1 through 10 inclusive. Default is 3.
- The Animate checkbox starts checked only when `digraph.edges.size <= GraphStreamAdapter.LARGE_GRAPH_THRESH` (300).
- Yen does not record a frontier trace. Its opening animation stays the existing per-path walk in `KShortestPathRenderer.initialAnimation`.
- Hover distance is a status label. Do not replace a node’s `ui.label` (that label is the vertex id).
- `graph.css` must not hardcode `node#0`. The source vertex is the `source` UI class applied by `GraphStreamAdapter.createGraph(sourceVertex)`. That behavior is already covered by `GraphStreamAdapterSuite`.
- `SearchObserver` defaults to a no-op. Existing solver call sites, golden files, and `BaselineBenchmark` pass/fail rules stay as they are. Do not add Eppstein.
- The viewer must call Modified Dijkstra once, from source to target. Do not call `ModifiedDijkstrasPathSolver.findPaths` from the viewer: that method runs one search per vertex.
- Commit messages start with `#18`.

---

## File structure

- Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala` — request, result, validation, and the three solver calls.
- Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala` — Swing controls and status / legend.
- Create `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala` — observer, events, trace, recording observer.
- Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/FrontierPlayer.scala` — apply one trace event to node UI classes.
- Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PlaybackControl.scala` — play / pause / step / skip for the pump thread.
- Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/AnimatedShortestPathRenderer.scala` — `ShortestPathRenderer` that plays a trace before hover.
- Modify `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/DijkstrasAlgorithm.scala` — record events without changing distances.
- Modify `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/ModifiedDijkstrasAlgorithm.scala` — same, for one s–t search.
- Modify `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/ShortestPathsFinder.scala` — no signature change. The extra observer method is an overload on `DijkstrasAlgorithm` only.
- Modify `scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/DijkstrasPathSolver.scala` — pass an observer through `findPaths`.
- Modify `scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/ModifiedDijkstrasPathSolver.scala` — add `findPath` for one s–t search. Leave `findPaths` unchanged.
- Modify `scala-source/com/barrybecker4/graph/visualization/GraphViewerFrame.scala` — `presentDigraph` so a subclass can remember the digraph.
- Modify `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala` — bar, solve worker, compare toggle.
- Modify `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PathRenderer.scala` — status callback and `distanceOf`.
- Modify `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/ShortestPathRenderer.scala` — distance lookup.
- Modify `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/KShortestPathRenderer.scala` — ranks through a node, for the status line.
- Modify `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/GraphViewerListener.scala` — publish hover distance / ranks.
- Modify `scala-source/com/barrybecker4/graph/visualization/graph.css` — `node.frontier` and `node.current`.
- Modify `README.md` — say the viewer can run a solver, not only open fixtures.
- Tests live beside the code under `scala-test/`, named below in each task.

---

### Task 1: LiveSolver

**Files:**
- Create: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala`
- Test: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala`

**Interfaces:**
- Consumes: `DijkstrasPathSolver.findPaths(graph, source)` and `YensKPathsSolver.findPaths(graph, source, destination, k)` as they exist today. `ModifiedDijkstrasAlgorithm.getShortestPath(source, destination)` as it exists today. Task 3 will add an observer parameter with a default, so this call shape still compiles.
- Produces:
  - `enum AlgorithmKind { case Dijkstra, ModifiedDijkstra, Yen }`
  - `case class SolveRequest(algorithm: AlgorithmKind, source: Int, target: Int, k: Int)`
  - `enum SolveResult { case ToAll(solution: ShortestPathsSolution, trace: SearchTrace); case OnePath(path: Path, trace: SearchTrace); case KPaths(solution: KShortestPathsSolution) }`
  - `object LiveSolver` with `def maxVertexId(graph: DirectedGraph): Int`, `def validate(graph: DirectedGraph, request: SolveRequest): Either[String, SolveRequest]`, `def solve(graph: DirectedGraph, request: SolveRequest): SolveResult`
  - `SearchTrace` is created empty in this task. Task 3 fills it. Define the empty type here so `SolveResult` does not change later:

```scala
package com.barrybecker4.graph.algorithms.shortestpaths

enum SearchEvent {
  case Frontier(vertex: Int, distance: Double)
  case Extracted(vertex: Int)
  case Settled(vertex: Int)
}

case class SearchTrace(events: Vector[SearchEvent])

object SearchTrace {
  val empty: SearchTrace = SearchTrace(Vector.empty)
}
```

Put that empty definition in `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala` during this task (observer methods arrive in Task 3). `LiveSolver` imports `SearchTrace`.

- [ ] **Step 1: Write the failing test**

Create `scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class LiveSolverSuite extends AnyFunSuite {

  /** 0→1 (1), 1→2 (2), 0→2 (4). Shortest 0→2 is 3 via 1. */
  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("maxVertexId is the larger of numVertices-1 and any edge endpoint") {
    assert(LiveSolver.maxVertexId(triangle) == 2)
    val sparse = DirectedGraph(1, IndexedSeq(DirectedEdge(0, 4, 1.0)))
    assert(LiveSolver.maxVertexId(sparse) == 4)
  }

  test("Dijkstra from 0 returns a path of weight 3 to vertex 2") {
    val result = LiveSolver.solve(triangle, SolveRequest(Dijkstra, 0, 2, 3))
    result match {
      case SolveResult.ToAll(solution, _) =>
        val to2 = solution.paths.find(_.lastNode == 2).get
        assert(to2 == Path(3.0, List(0, 1, 2)))
      case other => fail(s"expected ToAll, got $other")
    }
  }

  test("Modified Dijkstra returns only the s-t path") {
    val result = LiveSolver.solve(triangle, SolveRequest(ModifiedDijkstra, 0, 2, 3))
    assert(result == SolveResult.OnePath(Path(3.0, List(0, 1, 2)), com.barrybecker4.graph.algorithms.shortestpaths.SearchTrace.empty))
  }

  test("Yen returns two loopless 0-to-2 paths, cheapest first") {
    val result = LiveSolver.solve(triangle, SolveRequest(Yen, 0, 2, 2))
    result match {
      case SolveResult.KPaths(solution) =>
        assert(solution.shortestPaths.map(_.weight) == Seq(3.0, 4.0))
        assert(solution.shortestPaths.head.nodes == List(0, 1, 2))
      case other => fail(s"expected KPaths, got $other")
    }
  }

  test("rejects a target outside the graph, Yen with source == target, and k outside 1..10") {
    assert(LiveSolver.validate(triangle, SolveRequest(Dijkstra, 0, 9, 3)) ==
      Left("Target must be from 0 to 2"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 1, 1, 3)) ==
      Left("Yen needs different source and target"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 0, 2, 0)) ==
      Left("k must be from 1 to 10"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 0, 2, 11)) ==
      Left("k must be from 1 to 10"))
  }

  test("Modified Dijkstra with no path returns the empty path") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val result = LiveSolver.solve(graph, SolveRequest(ModifiedDijkstra, 1, 0, 3))
    assert(result == SolveResult.OnePath(Path.EMPTY_PATH, com.barrybecker4.graph.algorithms.shortestpaths.SearchTrace.empty))
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite`

Expected: FAIL. `LiveSolver` is not defined.

- [ ] **Step 3: Write the minimal implementation**

Create `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala` with only the trace types for now:

```scala
package com.barrybecker4.graph.algorithms.shortestpaths

enum SearchEvent {
  case Frontier(vertex: Int, distance: Double)
  case Extracted(vertex: Int)
  case Settled(vertex: Int)
}

case class SearchTrace(events: Vector[SearchEvent])

object SearchTrace {
  val empty: SearchTrace = SearchTrace(Vector.empty)
}
```

Create `scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.solver.DijkstrasPathSolver
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.{ModifiedDijkstrasAlgorithm, SearchTrace}
import com.barrybecker4.graph.directed.DirectedGraph

enum AlgorithmKind {
  case Dijkstra, ModifiedDijkstra, Yen
}

case class SolveRequest(algorithm: AlgorithmKind, source: Int, target: Int, k: Int)

enum SolveResult {
  case ToAll(solution: ShortestPathsSolution, trace: SearchTrace)
  case OnePath(path: Path, trace: SearchTrace)
  case KPaths(solution: KShortestPathsSolution)
}

object LiveSolver {

  def maxVertexId(graph: DirectedGraph): Int = {
    val edgeMax = graph.edges.foldLeft(-1) { (maxId, edge) =>
      maxId.max(edge.source).max(edge.destination)
    }
    edgeMax.max(graph.numVertices - 1)
  }

  def validate(graph: DirectedGraph, request: SolveRequest): Either[String, SolveRequest] = {
    val maxId = maxVertexId(graph)
    if (request.source < 0 || request.source > maxId)
      Left(s"Source must be from 0 to $maxId")
    else if (request.target < 0 || request.target > maxId)
      Left(s"Target must be from 0 to $maxId")
    else if (request.algorithm == AlgorithmKind.Yen && request.source == request.target)
      Left("Yen needs different source and target")
    else if (request.algorithm == AlgorithmKind.Yen && (request.k < 1 || request.k > 10))
      Left("k must be from 1 to 10")
    else Right(request)
  }

  /** Caller must pass a request that `validate` accepts. */
  def solve(graph: DirectedGraph, request: SolveRequest): SolveResult =
    request.algorithm match {
      case AlgorithmKind.Dijkstra =>
        val solution = new DijkstrasPathSolver().findPaths(graph, request.source)
        SolveResult.ToAll(solution, SearchTrace.empty)
      case AlgorithmKind.ModifiedDijkstra =>
        val path = new ModifiedDijkstrasAlgorithm(graph)
          .getShortestPath(request.source, request.target)
          .getOrElse(Path.EMPTY_PATH)
        SolveResult.OnePath(path, SearchTrace.empty)
      case AlgorithmKind.Yen =>
        val solution = new YensKPathsSolver(request.k)
          .findPaths(graph, request.source, request.target, request.k)
        SolveResult.KPaths(solution)
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala
git commit -m "$(cat <<'EOF'
#18 Add a pure solver entry for the path viewer

EOF
)"
```

---

### Task 2: Solve bar and frame wiring

**Files:**
- Create: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala`
- Modify: `scala-source/com/barrybecker4/graph/visualization/GraphViewerFrame.scala` (`loadGraph`, add `presentDigraph`)
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala`
- Modify: `README.md` (the “How to use it” paragraph that describes GraphViewerApp)
- Test: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/PathViewerSolveSuite.scala`

**Interfaces:**
- Consumes: `LiveSolver.validate`, `LiveSolver.solve`, `LiveSolver.maxVertexId`, `SolveRequest`, `SolveResult`, `AlgorithmKind` from Task 1. `ShortestPathRenderer(graph, solution, viewer)` and `KShortestPathRenderer(graph, solution, viewer)`. `GraphStreamAdapter(digraph).createGraph(sourceVertex)`.
- Produces:
  - `class SolveBar(onSolve: SolveRequest => Unit, onCompare: Option[AlgorithmKind] => Unit)` with `setVertexRange(maxId: Int): Unit`, `setStatus(text: String): Unit`, `setBusy(busy: Boolean): Unit`, `selectedRequest(): SolveRequest`, `compareAlgorithm(): Option[AlgorithmKind]`.
  - `GraphViewerFrame.presentDigraph(digraph: DirectedGraph, title: String, sourceVertex: Int = 0): Unit`
  - `PathViewerFrame` method `private[pathviewer] def solve(request: SolveRequest): Unit` and `private[pathviewer] def loadedGraph: Option[DirectedGraph]`

`onCompare` is stored and unused until Task 6. Pass `(_ => ())` from the frame in this task. The bar still shows the Compare checkbox disabled until Task 6 enables it. Build the checkbox now so Task 6 does not recreate the panel: `compareBox.setEnabled(false)`.

- [ ] **Step 1: Write the failing test**

Create `scala-test/com/barrybecker4/discreteoptimization/pathviewer/PathViewerSolveSuite.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

import scala.jdk.CollectionConverters.*

class PathViewerSolveSuite extends AnyFunSuite {

  System.setProperty("org.graphstream.ui", "swing")

  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("solve marks the chosen source and starts one path-renderer pump") {
    val frame = new Harness
    frame.setVisible(false)
    try {
      frame.load(triangle)
      assert(frame.graphLoaded.contains(triangle))

      frame.run(SolveRequest(AlgorithmKind.Dijkstra, 2, 0, 3))

      val graph = frame.viewer.getGraphicGraph
      assert(graph.getNode("2").getAttribute("ui.class", classOf[String]) == "source")
      assert(graph.getNode("0").getAttribute("ui.class") == null)
      val pumps = waitForPumps()
      assert(pumps.size == 1)
    } finally {
      frame.dispose()
    }
  }

  /** Subclass so the test can call protected `presentDigraph`. */
  private class Harness extends PathViewerFrame {
    def load(digraph: DirectedGraph): Unit = presentDigraph(digraph, "triangle", 0)
    def run(request: SolveRequest): Unit = solveNow(request)
    def graphLoaded: Option[DirectedGraph] = loadedGraph
  }

  private def waitForPumps(): Set[Thread] = {
    val deadline = System.nanoTime() + 3_000_000_000L
    var found = Set.empty[Thread]
    while (found.isEmpty && System.nanoTime() < deadline) {
      found = Thread.getAllStackTraces.keySet.asScala.filter { thread =>
        thread.getName == "path-renderer-pump" && thread.isAlive
      }.toSet
      if (found.isEmpty) Thread.sleep(20)
    }
    found
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite`

Expected: FAIL to compile. `solveNow` is not defined on `PathViewerFrame`.

- [ ] **Step 3: Write the minimal implementation**

In `GraphViewerFrame`, replace `loadGraph` with:

```scala
protected def loadGraph(): Unit = {
  val fileChooser = new JFileChooser()
  fileChooser.setCurrentDirectory(new File(PREFIX))

  val returnValue = fileChooser.showOpenDialog(GraphViewerFrame.this)
  if (returnValue == JFileChooser.APPROVE_OPTION) {
    val selectedFile = fileChooser.getSelectedFile
    val digraph = loadGraphFromFile(selectedFile)
    presentDigraph(digraph, selectedFile.getName)
  }
}

protected def presentDigraph(digraph: DirectedGraph, title: String, sourceVertex: Int = 0): Unit = {
  val graph = GraphStreamAdapter(digraph).createGraph(sourceVertex)
  setGraph(graph, title)
}
```

Create `SolveBar.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*

import java.awt.FlowLayout
import javax.swing.*

class SolveBar(onSolve: SolveRequest => Unit, onCompare: Option[AlgorithmKind] => Unit) extends JPanel(new FlowLayout(FlowLayout.LEFT)) {

  private val algorithm = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val source = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val target = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1))
  private val k = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1))
  private val solveButton = new JButton("Solve")
  private val compareBox = new JCheckBox("Compare")
  private val compareAlgorithm = new JComboBox[AlgorithmKind](Array(Dijkstra, ModifiedDijkstra, Yen))
  private val status = new JLabel("Open a graph, then Solve")

  compareBox.setEnabled(false)
  compareAlgorithm.setEnabled(false)
  k.setEnabled(false)

  algorithm.addActionListener(_ => k.setEnabled(algorithm.getSelectedItem == Yen))
  solveButton.addActionListener(_ => onSolve(selectedRequest()))
  compareBox.addActionListener(_ => onCompare(this.compareAlgorithm()))

  add(new JLabel("Algorithm"))
  add(algorithm)
  add(new JLabel("Source"))
  add(source)
  add(new JLabel("Target"))
  add(target)
  add(new JLabel("k"))
  add(k)
  add(solveButton)
  add(compareBox)
  add(compareAlgorithm)
  add(status)

  def setVertexRange(maxId: Int): Unit = {
    source.setModel(new SpinnerNumberModel(0, 0, maxId.max(0), 1))
    val initialTarget = if (maxId > 0) maxId else 0
    target.setModel(new SpinnerNumberModel(initialTarget, 0, maxId.max(0), 1))
  }

  def setSource(vertex: Int): Unit = source.setValue(vertex)

  def setStatus(text: String): Unit = status.setText(text)

  def setBusy(busy: Boolean): Unit = solveButton.setEnabled(!busy)

  def selectedRequest(): SolveRequest =
    SolveRequest(
      algorithm.getSelectedItem.asInstanceOf[AlgorithmKind],
      source.getValue.asInstanceOf[Int],
      target.getValue.asInstanceOf[Int],
      k.getValue.asInstanceOf[Int]
    )

  def compareAlgorithm(): Option[AlgorithmKind] =
    if (compareBox.isSelected) Some(compareAlgorithm.getSelectedItem.asInstanceOf[AlgorithmKind])
    else None

  /** Task 6 calls this. Until then the checkbox stays disabled. */
  def setCompareEnabled(enabled: Boolean): Unit = {
    compareBox.setEnabled(enabled)
    compareAlgorithm.setEnabled(enabled && compareBox.isSelected)
  }
}
```

In `PathViewerFrame`, add fields and wire the bar. Keep the File menu items. Replace `loadTheGraph` so the digraph is remembered. Add `solve`.

```scala
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.graph.Path

import java.awt.BorderLayout
import javax.swing.SwingWorker

// inside the class, next to activeRenderer:
private var loaded: Option[DirectedGraph] = None
private val solveBar = new SolveBar(solve, _ => ())
getContentPane.add(solveBar, BorderLayout.NORTH)

private[pathviewer] def loadedGraph: Option[DirectedGraph] = loaded

override protected def presentDigraph(digraph: DirectedGraph, title: String, sourceVertex: Int = 0): Unit = {
  loaded = Some(digraph)
  solveBar.setVertexRange(LiveSolver.maxVertexId(digraph))
  solveBar.setSource(sourceVertex)
  super.presentDigraph(digraph, title, sourceVertex)
}

private[pathviewer] def solve(request: SolveRequest): Unit = {
  val digraph = loaded match {
    case None =>
      solveBar.setStatus("Open a graph first")
      return
    case Some(graph) => graph
  }
  LiveSolver.validate(digraph, request) match {
    case Left(message) =>
      solveBar.setStatus(message)
    case Right(valid) =>
      solveBar.setBusy(true)
      val worker = new SwingWorker[SolveResult, java.lang.Void] {
        override def doInBackground(): SolveResult = LiveSolver.solve(digraph, valid)
        override def done(): Unit = {
          solveBar.setBusy(false)
          try showSolved(valid, get())
          catch {
            case _: java.util.concurrent.CancellationException => ()
            case ex: java.util.concurrent.ExecutionException =>
              solveBar.setStatus(ex.getCause.toString)
          }
        }
      }
      worker.execute()
  }
}

private def showSolved(request: SolveRequest, result: SolveResult): Unit = {
  val digraph = loaded.get
  val stream = GraphStreamAdapter(digraph).createGraph(request.source)
  setGraph(stream, titleFor(request))
  result match {
    case SolveResult.ToAll(solution, _) =>
      showShortestPaths(solution, stream)
      solveBar.setStatus(dijkstraStatus(request, solution))
    case SolveResult.OnePath(path, _) =>
      val solution = ShortestPathsSolution(path.weight, List(path))
      showShortestPaths(solution, stream)
      solveBar.setStatus(onePathStatus(request, path))
    case SolveResult.KPaths(solution) =>
      showKShortestPaths(solution, stream)
      solveBar.setStatus(f"Yen k=${solution.k} total ${solution.totalCost}%.4g")
  }
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
  if (path.nodes.isEmpty || path.weight.isPosInfinity) s"Modified: no path ${request.source} → ${request.target}"
  else f"Modified ${request.source} → ${request.target}: ${path.weight}%.4g ${path.nodes.mkString(" ")}"
```

Change `loadTheGraph` to go through `presentDigraph` is wrong because `showShortestPaths` needs the `MultiGraph` after `setGraph`. Keep building it, but remember the digraph:

```scala
private def loadTheGraph(file: File, sourceVertex: Int): MultiGraph = {
  val graphName = getGraphName(file.getName)
  val digraph = loadGraphFromName(graphName)
  loaded = Some(digraph)
  solveBar.setVertexRange(LiveSolver.maxVertexId(digraph))
  solveBar.setSource(sourceVertex)
  val graph = GraphStreamAdapter(digraph).createGraph(sourceVertex)
  setGraph(graph, file.getName)
  graph
}
```

The Solve button calls `solve`, which uses the worker. The test calls `solveNow`, which applies the same `showSolved` body on the calling thread:

```scala
private[pathviewer] def solveNow(request: SolveRequest): Unit = {
  val digraph = loaded.getOrElse(throw new IllegalStateException("Open a graph first"))
  val valid = LiveSolver.validate(digraph, request).fold(message => throw new IllegalArgumentException(message), identity)
  showSolved(valid, LiveSolver.solve(digraph, valid))
}
```

In `README.md`, replace the sentence “The default application is the GraphViewerApp. It allows you to view the graph tests cases and their results.” with:

```markdown
The default application is PathViewer. File → Open Graph loads a graph, and the Solve bar runs Dijkstra (all targets), Modified Dijkstra (the chosen source and target only), or Yen (k shortest paths, k from 1 to 10). File → Open Shortest Paths and File → Open K Shortest Paths still open the golden solution files.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerReloadSuite`

Expected: PASS. Reload still stops the previous pump because `setGraph` still calls `stopActiveRenderer`.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala \
  scala-source/com/barrybecker4/graph/visualization/GraphViewerFrame.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/PathViewerSolveSuite.scala \
  README.md
git commit -m "$(cat <<'EOF'
#18 Run Dijkstra, Modified Dijkstra, or Yen from PathViewer

EOF
)"
```

---

### Task 3: Record the Dijkstra frontier

**Files:**
- Modify: `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala`
- Modify: `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/DijkstrasAlgorithm.scala`
- Modify: `scala-source/com/barrybecker4/graph/algorithms/shortestpaths/ModifiedDijkstrasAlgorithm.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/DijkstrasPathSolver.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/ModifiedDijkstrasPathSolver.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala`
- Test: `scala-test/com/barrybecker4/graph/algorithms/shortestpaths/SearchTraceSuite.scala`
- Modify: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala` (the Modified assertion that expects `SearchTrace.empty`)

**Interfaces:**
- Consumes: `SearchEvent` and `SearchTrace` from Task 1.
- Produces:
  - `trait SearchObserver { def frontier(vertex: Int, distance: Double): Unit; def extracted(vertex: Int): Unit; def settled(vertex: Int): Unit }`
  - `object SearchObserver { val None: SearchObserver; def recording(): RecordingSearchObserver }`
  - `final class RecordingSearchObserver extends SearchObserver { def trace: SearchTrace }`
  - `DijkstrasAlgorithm.findShortestPaths(source: Int, observer: SearchObserver): ShortestPaths` — the trait method `findShortestPaths(source: Int)` delegates to this with `SearchObserver.None`.
  - `ModifiedDijkstrasAlgorithm.getShortestPath(source: Int, destination: Int, observer: SearchObserver = SearchObserver.None): Option[Path]`
  - `DijkstrasPathSolver.findPaths(graph, sourceVertex, observer: SearchObserver = SearchObserver.None)`
  - `ModifiedDijkstrasPathSolver.findPath(graph, source, destination, observer: SearchObserver = SearchObserver.None): Option[Path]`
  - `LiveSolver.solve` puts the recorded trace on `ToAll` and `OnePath`. `KPaths` still has no trace.

Do not change `YensAlgorithm`. It keeps calling `getShortestPath(start, end)` with the default no-op observer.

- [ ] **Step 1: Write the failing test**

Create `scala-test/com/barrybecker4/graph/algorithms/shortestpaths/SearchTraceSuite.scala`:

```scala
package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.*
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class SearchTraceSuite extends AnyFunSuite {

  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("Dijkstra records frontier, extract-min, and settled vertices without changing the path") {
    val observer = SearchObserver.recording()
    val paths = new DijkstrasAlgorithm(triangle).findShortestPaths(0, observer)
    assert(paths.pathToVertex(2) == Path(3.0, List(0, 1, 2)))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1),
      Frontier(2, 3.0),
      Extracted(2), Settled(2)
    ))
  }

  test("Modified Dijkstra aimed at 2 records the same triangle trace and the same path") {
    val observer = SearchObserver.recording()
    val path = new ModifiedDijkstrasAlgorithm(triangle).getShortestPath(0, 2, observer)
    assert(path.contains(Path(3.0, List(0, 1, 2))))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1),
      Frontier(2, 3.0),
      Extracted(2), Settled(2)
    ))
  }

  test("Modified Dijkstra aimed at 1 stops when 1 is extracted") {
    val observer = SearchObserver.recording()
    val path = new ModifiedDijkstrasAlgorithm(triangle).getShortestPath(0, 1, observer)
    assert(path.contains(Path(1.0, List(0, 1))))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1)
    ))
  }

  test("the default observer leaves findShortestPaths unchanged") {
    val paths = new DijkstrasAlgorithm(triangle).findShortestPaths(0)
    assert(paths.pathToVertex(2) == Path(3.0, List(0, 1, 2)))
  }
}
```

In `LiveSolverSuite`, change the Modified success assertion from `SearchTrace.empty` to a non-empty trace whose settled vertices are `0, 1, 2`:

```scala
test("Modified Dijkstra returns only the s-t path and a non-empty trace") {
  val result = LiveSolver.solve(triangle, SolveRequest(ModifiedDijkstra, 0, 2, 3))
  result match {
    case SolveResult.OnePath(path, trace) =>
      assert(path == Path(3.0, List(0, 1, 2)))
      assert(trace.events.collect { case com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.Settled(v) => v } ==
        Vector(0, 1, 2))
    case other => fail(s"expected OnePath, got $other")
  }
}
```

Change the unreachable Modified assertion so it still expects `Path.EMPTY_PATH` and does not require `SearchTrace.empty`:

```scala
result match {
  case SolveResult.OnePath(path, _) => assert(path == Path.EMPTY_PATH)
  case other => fail(s"expected OnePath, got $other")
}
```

Add one Dijkstra assertion in that suite:

```scala
test("Dijkstra solve keeps a trace whose settled order is 0, 1, 2") {
  val result = LiveSolver.solve(triangle, SolveRequest(Dijkstra, 0, 2, 3))
  result match {
    case SolveResult.ToAll(_, trace) =>
      assert(trace.events.collect { case com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.Settled(v) => v } ==
        Vector(0, 1, 2))
    case other => fail(s"expected ToAll, got $other")
  }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests com.barrybecker4.graph.algorithms.shortestpaths.SearchTraceSuite --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite`

Expected: FAIL to compile. `SearchObserver` and the observer overloads do not exist, and `LiveSolver` still returns `SearchTrace.empty`.

- [ ] **Step 3: Write the minimal implementation**

Replace `SearchObserver.scala` with:

```scala
package com.barrybecker4.graph.algorithms.shortestpaths

enum SearchEvent {
  case Frontier(vertex: Int, distance: Double)
  case Extracted(vertex: Int)
  case Settled(vertex: Int)
}

case class SearchTrace(events: Vector[SearchEvent])

object SearchTrace {
  val empty: SearchTrace = SearchTrace(Vector.empty)
}

trait SearchObserver {
  def frontier(vertex: Int, distance: Double): Unit
  def extracted(vertex: Int): Unit
  def settled(vertex: Int): Unit
}

object SearchObserver {
  val None: SearchObserver = new SearchObserver {
    def frontier(vertex: Int, distance: Double): Unit = ()
    def extracted(vertex: Int): Unit = ()
    def settled(vertex: Int): Unit = ()
  }

  def recording(): RecordingSearchObserver = new RecordingSearchObserver
}

final class RecordingSearchObserver extends SearchObserver {
  private val buffer = Vector.newBuilder[SearchEvent]
  def frontier(vertex: Int, distance: Double): Unit = buffer += SearchEvent.Frontier(vertex, distance)
  def extracted(vertex: Int): Unit = buffer += SearchEvent.Extracted(vertex)
  def settled(vertex: Int): Unit = buffer += SearchEvent.Settled(vertex)
  def trace: SearchTrace = SearchTrace(buffer.result())
}
```

In `DijkstrasAlgorithm`, keep the trait method and add the overload. The search body moves to the overload:

```scala
override def findShortestPaths(source: Int): ShortestPaths =
  findShortestPaths(source, SearchObserver.None)

def findShortestPaths(source: Int, observer: SearchObserver): ShortestPaths = {
  if (source >= size)
    throw new IllegalArgumentException(s"Source vertex must be < $size")
  else {
    val shortestPaths = ShortestPaths(size, source)
    val queue = DijkstraHeap()
    queue.enqueue(source, 0.0)
    observer.frontier(source, 0.0)

    def stillBest(vertex: Int, distQueued: Double): Boolean =
      DijkstraHeap.sameDistance(distQueued, shortestPaths.distToVertex(vertex))

    @tailrec
    def search(): Unit =
      queue.extractMin(stillBest) match {
        case None => ()
        case Some(vertex) =>
          observer.extracted(vertex)
          observer.settled(vertex)
          for (edge <- graph.outgoingNeighborsOf(vertex) if shortestPaths.isBetterEdge(edge)) {
            shortestPaths.useEdge(edge)
            val distance = shortestPaths.distToVertex(edge.destination)
            queue.enqueue(edge.destination, distance)
            observer.frontier(edge.destination, distance)
          }
          search()
      }

    search()
    shortestPaths
  }
}
```

In `ModifiedDijkstrasAlgorithm`, replace `getShortestPath`, `updateVertex`, and `determineShortestPaths` with the versions below. The default observer keeps every existing caller compiling. `findShortestPathFlowerRootAt` calls `determineShortestPaths(-1, root, isOpposite = true, SearchObserver.None)`. Observer calls sit before the recursive `updateVertex` call, so the method stays tail recursive.

```scala
def getShortestPath(source: Int, destination: Int, observer: SearchObserver = SearchObserver.None): Option[Path] = {
  determineShortestPaths(source, destination, isOpposite = false, observer)
  if (distanceTo.contains(destination))
    Some(Path(distanceTo(destination), getPath(source, destination)))
  else None
}

@tailrec
private def updateVertex(end: Int, isOpposite: Boolean, observer: SearchObserver): Unit = {
  vertexCandidateQueue.extractMin { (node, distQueued) =>
    !determinedVertexSet.contains(node) &&
      DijkstraHeap.sameDistance(distQueued, distanceTo.getOrElse(node, Double.MaxValue))
  } match {
    case None => ()
    case Some(node) if node == end =>
      observer.extracted(node)
      observer.settled(node)
    case Some(node) =>
      observer.extracted(node)
      observer.settled(node)
      determinedVertexSet += node
      val neighborSet = if (isOpposite) graph.incomingNeighborsOf(node) else graph.outgoingNeighborsOf(node)

      def isDetermined(e: DirectedEdge): Boolean =
        determinedVertexSet.contains(if (isOpposite) e.source else e.destination)

      neighborSet.filterNot(isDetermined).foreach { nextEdge =>
        val next = if (isOpposite) nextEdge.source else nextEdge.destination
        val edgeWeight = nextEdge.weight
        val curDistance = distanceTo.getOrElse(node, Double.MaxValue - edgeWeight)
        val distance = curDistance + edgeWeight
        if (!distanceTo.contains(next) || distance < distanceTo(next)) {
          distanceTo.put(next, distance)
          predecessorMap += next -> node
          vertexCandidateQueue.enqueue(next, distance)
          observer.frontier(next, distance)
        }
      }
      updateVertex(end, isOpposite, observer)
  }
}

private def determineShortestPaths(
    source: Int,
    destination: Int,
    isOpposite: Boolean,
    observer: SearchObserver
): Unit = {
  clear()
  val end = if (isOpposite) source else destination
  val start = if (isOpposite) destination else source
  distanceTo.put(start, 0d)
  vertexCandidateQueue.enqueue(start, 0d)
  observer.frontier(start, 0d)
  updateVertex(end, isOpposite, observer)
}
```

In `DijkstrasPathSolver`:

```scala
def findPaths(
    graph: DirectedGraph,
    sourceVertex: Int,
    observer: SearchObserver = SearchObserver.None
): ShortestPathsSolution = {
  val pathCalc = new DijkstrasAlgorithm(graph).findShortestPaths(sourceVertex, observer)
  val paths = Range(0, graph.numVertices).map(pathCalc.pathToVertex).toList
  ShortestPathsSolution(paths.map(_.weight).sum, paths)
}
```

Add the import for `SearchObserver`.

In `ModifiedDijkstrasPathSolver`, add (do not change `findPaths`):

```scala
def findPath(
    graph: DirectedGraph,
    source: Int,
    destination: Int,
    observer: SearchObserver = SearchObserver.None
): Option[Path] =
  new ModifiedDijkstrasAlgorithm(graph).getShortestPath(source, destination, observer)
```

In `LiveSolver.solve`, record traces:

```scala
case AlgorithmKind.Dijkstra =>
  val observer = SearchObserver.recording()
  val solution = new DijkstrasPathSolver().findPaths(graph, request.source, observer)
  SolveResult.ToAll(solution, observer.trace)
case AlgorithmKind.ModifiedDijkstra =>
  val observer = SearchObserver.recording()
  val path = new ModifiedDijkstrasPathSolver()
    .findPath(graph, request.source, request.target, observer)
    .getOrElse(Path.EMPTY_PATH)
  SolveResult.OnePath(path, observer.trace)
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests com.barrybecker4.graph.algorithms.shortestpaths.SearchTraceSuite --tests com.barrybecker4.graph.algorithms.shortestpaths.DijkstrasAlgorithmSuite --tests com.barrybecker4.graph.algorithms.shortestpaths.ModifiedDijkstrasAlgorithmSuite --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite --tests com.barrybecker4.graph.algorithms.kshortestpaths.YensAlgorithmSuite`

Expected: PASS. Yen still finds the same paths because its `getShortestPath` calls use the no-op observer.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/graph/algorithms/shortestpaths/SearchObserver.scala \
  scala-source/com/barrybecker4/graph/algorithms/shortestpaths/DijkstrasAlgorithm.scala \
  scala-source/com/barrybecker4/graph/algorithms/shortestpaths/ModifiedDijkstrasAlgorithm.scala \
  scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/DijkstrasPathSolver.scala \
  scala-source/com/barrybecker4/discreteoptimization/shortestpaths/solver/ModifiedDijkstrasPathSolver.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala \
  scala-test/com/barrybecker4/graph/algorithms/shortestpaths/SearchTraceSuite.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala
git commit -m "$(cat <<'EOF'
#18 Record Dijkstra frontier events without changing path costs

EOF
)"
```

---

### Task 4: Play, pause, and step the frontier

**Files:**
- Create: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/FrontierPlayer.scala`
- Create: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PlaybackControl.scala`
- Create: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/AnimatedShortestPathRenderer.scala`
- Modify: `scala-source/com/barrybecker4/graph/visualization/graph.css` (append `node.frontier` and `node.current`)
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala` (Animate checkbox, Play, Pause, Step, Skip)
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala` (`showSolved` uses the animated renderer when Animate is checked and the result has a trace)
- Test: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/FrontierPlayerSuite.scala`
- Test: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/PlaybackControlSuite.scala`

**Interfaces:**
- Consumes: `SearchEvent`, `SearchTrace`, `PathRenderer.mergeUiClass`, `SolveResult.ToAll` and `SolveResult.OnePath`.
- Produces:
  - `class FrontierPlayer(graph: MultiGraph) { def apply(event: SearchEvent): Unit }`
  - `final class PlaybackControl` with `def pause(): Unit`, `def play(): Unit`, `def step(): Unit`, `def skip(): Unit`, `def awaitAdvance(timeoutMs: Long): Boolean` (throws `InterruptedException`), `def shouldStop: Boolean`
  - `SolveBar.animateSelected: Boolean`, `SolveBar.setAnimateDefault(on: Boolean): Unit`, and playback buttons that call a `PlaybackControl` supplied with `def bindPlayback(control: PlaybackControl): Unit`
  - `AnimatedShortestPathRenderer(graph, solution, viewer, trace, control)` extends the coloring behavior of `ShortestPathRenderer` and overrides `initialAnimation`

Hover stays blocked until the trace finishes or Skip is pressed, because `PathRenderer.pumpEvents` runs `initialAnimation` before `listenForMouseEvents`. That is the intended order.

- [ ] **Step 1: Write the failing tests**

Create `FrontierPlayerSuite.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.*
import org.graphstream.graph.implementations.MultiGraph
import org.scalatest.funsuite.AnyFunSuite

class FrontierPlayerSuite extends AnyFunSuite {

  test("frontier, current, and settled replace each other and keep source") {
    val graph = new MultiGraph("t")
    val source = graph.addNode("0")
    source.setAttribute("ui.class", "source")
    graph.addNode("1")
    val player = new FrontierPlayer(graph)

    player.apply(Frontier(0, 0.0))
    assert(source.getAttribute("ui.class", classOf[String]) == "frontier, source")

    player.apply(Extracted(0))
    assert(source.getAttribute("ui.class", classOf[String]) == "current, source")

    player.apply(Settled(0))
    assert(source.getAttribute("ui.class", classOf[String]) == "visited, source")

    player.apply(Frontier(1, 1.0))
    player.apply(Extracted(1))
    assert(graph.getNode("1").getAttribute("ui.class", classOf[String]) == "current")
    assert(source.getAttribute("ui.class", classOf[String]) == "visited, source")
  }
}
```

Create `PlaybackControlSuite.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer.render

import org.scalatest.funsuite.AnyFunSuite

class PlaybackControlSuite extends AnyFunSuite {

  test("step releases a paused waiter once") {
    val control = new PlaybackControl
    control.pause()
    val waiter = new Thread(() => {
      assert(control.awaitAdvance(2_000))
      assert(!control.awaitAdvance(50))
    })
    waiter.start()
    control.step()
    waiter.join(3_000)
    assert(!waiter.isAlive)
  }

  test("skip reports shouldStop and releases the waiter") {
    val control = new PlaybackControl
    control.pause()
    val waiter = new Thread(() => assert(control.awaitAdvance(2_000)))
    waiter.start()
    control.skip()
    waiter.join(3_000)
    assert(control.shouldStop)
    assert(!waiter.isAlive)
  }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.render.FrontierPlayerSuite --tests com.barrybecker4.discreteoptimization.pathviewer.render.PlaybackControlSuite`

Expected: FAIL. The two classes are not defined.

- [ ] **Step 3: Write the minimal implementation**

Append to `graph.css`:

```css
node.frontier {
  fill-color: #ffe08a;
  stroke-color: #c48a00;
  stroke-width: 2px;
}

node.current {
  fill-color: #ffb347;
  stroke-color: #cc5500;
  stroke-width: 3px;
  z-index: 15;
}
```

Create `FrontierPlayer.scala`:

```scala
package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph

class FrontierPlayer(graph: MultiGraph) {
  private var current: Option[Node] = None

  def apply(event: SearchEvent): Unit = event match {
    case SearchEvent.Frontier(vertex, _) =>
      setClass(vertex, "frontier")
    case SearchEvent.Extracted(vertex) =>
      clearCurrent()
      setClass(vertex, "current")
      current = Option(graph.getNode(vertex.toString))
    case SearchEvent.Settled(vertex) =>
      setClass(vertex, "visited")
      if (current.exists(_.getId == vertex.toString)) current = None
  }

  private def clearCurrent(): Unit =
    current.foreach { node =>
      if (node.hasAttribute("ui.class")) {
        val kept = node.getAttribute("ui.class", classOf[String])
          .split(',').iterator.map(_.trim).filter(_ != "current").mkString(", ")
        if (kept.isEmpty) node.removeAttribute("ui.class")
        else node.setAttribute("ui.class", kept)
      }
    }

  private def setClass(vertex: Int, uiClass: String): Unit = {
    val node = graph.getNode(vertex.toString)
    if (node != null) {
      val existing =
        if (node.hasAttribute("ui.class")) Some(node.getAttribute("ui.class", classOf[String]))
        else None
      node.setAttribute("ui.class", PathRenderer.mergeUiClass(existing, uiClass))
    }
  }
}
```

Create `PlaybackControl.scala`. A playing control returns from `awaitAdvance` immediately and does not consume steps. A paused control blocks until `step`, `skip`, or the timeout. One `step` releases one waiter.

```scala
package com.barrybecker4.discreteoptimization.pathviewer.render

final class PlaybackControl {
  private var paused = false
  private var steps = 0
  private var stop = false

  def pause(): Unit = synchronized {
    paused = true
    notifyAll()
  }

  def play(): Unit = synchronized {
    paused = false
    steps = 0
    notifyAll()
  }

  def step(): Unit = synchronized {
    paused = true
    steps += 1
    notifyAll()
  }

  def skip(): Unit = synchronized {
    stop = true
    notifyAll()
  }

  def shouldStop: Boolean = synchronized(stop)

  def isPaused: Boolean = synchronized(paused)

  /** @return false on timeout. True when the caller may show the next event, or when `shouldStop` is set. */
  def awaitAdvance(timeoutMs: Long): Boolean = synchronized {
    if (!paused || stop) return true
    if (steps > 0) {
      steps -= 1
      return true
    }
    val deadline = System.currentTimeMillis() + timeoutMs
    while (paused && steps == 0 && !stop) {
      val remaining = deadline - System.currentTimeMillis()
      if (remaining <= 0) return false
      wait(remaining)
    }
    if (steps > 0) steps -= 1
    true
  }
}
```

Create `AnimatedShortestPathRenderer.scala`. Extend `ShortestPathRenderer` so hover coloring stays. `initialAnimation` sleeps only while playing. While paused it waits for Step or Skip. `viewerPipe` is the protected field on `PathRenderer`.

```scala
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
```

Add to `SolveBar` a checkbox `animate`, buttons Play / Pause / Step / Skip, and:

```scala
private var playback: PlaybackControl = new PlaybackControl

def bindPlayback(control: PlaybackControl): Unit = playback = control
def animateSelected: Boolean = animate.isSelected
def setAnimateDefault(on: Boolean): Unit = animate.setSelected(on)
```

Play calls `playback.play()`, Pause calls `playback.pause()`, Step calls `playback.step()`, Skip calls `playback.skip()`.

In `PathViewerFrame.showSolved`, for `ToAll` and `OnePath`, when `solveBar.animateSelected` and `trace.events.nonEmpty`:

```scala
val control = new PlaybackControl
solveBar.bindPlayback(control)
val wrapped = result match {
  case SolveResult.OnePath(path, _) => ShortestPathsSolution(path.weight, List(path))
  case SolveResult.ToAll(solution, _) => solution
  case SolveResult.KPaths(_) => throw new IllegalStateException("Yen has no frontier trace")
}
activate(new AnimatedShortestPathRenderer(stream, wrapped, viewer, trace, control))
```

When Animate is unchecked, keep today’s `ShortestPathRenderer`. Yen always uses `KShortestPathRenderer`.

When a graph is loaded (`presentDigraph` and `loadTheGraph`), call:

```scala
solveBar.setAnimateDefault(digraph.edges.size <= com.barrybecker4.graph.visualization.GraphStreamAdapter.LARGE_GRAPH_THRESH)
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.render.FrontierPlayerSuite --tests com.barrybecker4.discreteoptimization.pathviewer.render.PlaybackControlSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.render.PathRendererLifecycleSuite`

In `PathViewerSolveSuite.Harness`, add `def useAnimation(on: Boolean): Unit = setAnimate(on)` and call `frame.useAnimation(false)` before `frame.run`. Add `private[pathviewer] def setAnimate(on: Boolean): Unit = solveBar.setAnimateDefault(on)` on `PathViewerFrame`. The suite then still sees `ui.class == "source"` because no frontier event runs. The pump thread is still named `path-renderer-pump`.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/FrontierPlayer.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PlaybackControl.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/AnimatedShortestPathRenderer.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala \
  scala-source/com/barrybecker4/graph/visualization/graph.css \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/FrontierPlayerSuite.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/PlaybackControlSuite.scala
git commit -m "$(cat <<'EOF'
#18 Animate the Dijkstra frontier with play, pause, and step

EOF
)"
```

---

### Task 5: Hover distance and the k-path legend

**Files:**
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PathRenderer.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/ShortestPathRenderer.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/KShortestPathRenderer.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/GraphViewerListener.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala` (`setLegend(entries: Seq[(Int, java.awt.Color)])`, `setStatus`)
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala` (pass the status callback, set the legend for Yen)
- Test: `scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/HoverTextSuite.scala`

**Interfaces:**
- Consumes: `ShortestPathsSolution`, `KShortestPathsSolution`, `PathColors.getColor`.
- Produces:
  - `PathRenderer.distanceText(nodeIdx: Int): Option[String]` default `None`
  - `PathRenderer.ranksText(nodeIdx: Int): Option[String]` default `None`
  - `PathRenderer.statusListener: String => Unit`, default ignores the string. `GraphViewerListener` calls it.
  - `ShortestPathRenderer.distanceText` returns `Some(f"$weight%.4g")` for a finite path ending at `nodeIdx`, `Some("unreachable")` when every matching path is infinite or missing nodes, and `None` when no path ends at that node.
  - `KShortestPathRenderer.ranksText(nodeIdx)` returns `Some("1, 3")` using 1-based ranks of paths that contain the node, or `None` when none do.
  - `SolveBar.setLegend(entries: Seq[(Int, java.awt.Color)])` replaces any previous legend swatches. Rank `1` is the first path.

- [ ] **Step 1: Write the failing test**

Create `HoverTextSuite.scala`. Do not call `render()`; constructing `ShortestPathRenderer` touches `viewer.getDefaultView`. Test the text helpers through a tiny subclass setup that does not need a viewer by making the helpers functions on the companion, which the renderer methods call.

The test targets the companions so it does not open a window:

```scala
package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.Path
import org.scalatest.funsuite.AnyFunSuite

class HoverTextSuite extends AnyFunSuite {

  test("distance text uses the finite path that ends at the node") {
    val solution = ShortestPathsSolution(4.0, List(
      Path(0.0, List(0)),
      Path(1.0, List(0, 1)),
      Path(Double.PositiveInfinity, List())
    ))
    assert(ShortestPathRenderer.distanceText(solution, 1).contains("1"))
    assert(ShortestPathRenderer.distanceText(solution, 2).contains("unreachable"))
    assert(ShortestPathRenderer.distanceText(solution, 9).isEmpty)
  }

  test("rank text is 1-based and lists every path through the node") {
    val solution = KShortestPathsSolution(7.0, 2, 2, Seq(
      Path(3.0, List(0, 1, 2)),
      Path(4.0, List(0, 2))
    ))
    assert(KShortestPathRenderer.ranksText(solution, 1).contains("1"))
    assert(KShortestPathRenderer.ranksText(solution, 2).contains("1, 2"))
    assert(KShortestPathRenderer.ranksText(solution, 4).isEmpty)
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.render.HoverTextSuite`

Expected: FAIL. `distanceText` and `ranksText` are not defined on the companions.

- [ ] **Step 3: Write the minimal implementation**

On `ShortestPathRenderer`’s companion (add `object ShortestPathRenderer`):

```scala
object ShortestPathRenderer {
  def distanceText(solution: ShortestPathsSolution, nodeIdx: Int): Option[String] = {
    val ending = solution.paths.filter(path => path.nodes.nonEmpty && path.lastNode == nodeIdx)
    if (ending.isEmpty) None
    else {
      val finite = ending.filterNot(_.weight.isPosInfinity)
      if (finite.isEmpty) Some("unreachable")
      else Some(f"${finite.minBy(_.weight).weight}%.4g")
    }
  }
}
```

The case class `ShortestPathRenderer` can share the name with the companion. Add the instance method:

```scala
override def distanceText(nodeIdx: Int): Option[String] =
  ShortestPathRenderer.distanceText(solution, nodeIdx)
```

On `KShortestPathRenderer`’s existing companion, add:

```scala
def ranksText(solution: KShortestPathsSolution, nodeIdx: Int): Option[String] = {
  val ranks = solution.shortestPaths.zipWithIndex.collect {
    case (path, index) if path.containsNode(nodeIdx) => (index + 1).toString
  }
  if (ranks.isEmpty) None else Some(ranks.mkString(", "))
}
```

Instance method:

```scala
override def ranksText(nodeIdx: Int): Option[String] =
  KShortestPathRenderer.ranksText(solution, nodeIdx)
```

On the `PathRenderer` trait:

```scala
var statusListener: String => Unit = _ => ()

def distanceText(nodeIdx: Int): Option[String] = None

def ranksText(nodeIdx: Int): Option[String] = None
```

Replace `mouseOver` and `mouseLeft` with:

```scala
override def mouseOver(id: String): Unit = {
  val edge = graph.getEdge(id)
  if (edge != null) {
    pathRenderer.colorPaths(edge.getNode0.getId.toInt, edge.getNode1.getId.toInt, HIGHLIGHTED)
    val left = pathRenderer.ranksText(edge.getNode0.getId.toInt)
    val right = pathRenderer.ranksText(edge.getNode1.getId.toInt)
    val ranks = (left.toList ++ right.toList).distinct.mkString(", ")
    val detail = if (ranks.isEmpty) "" else s"  ranks $ranks"
    pathRenderer.statusListener(s"edge ${edge.getId}$detail")
  } else {
    pathRenderer.colorPaths(id.toInt, HIGHLIGHTED)
    val nodeIdx = id.toInt
    val distance = pathRenderer.distanceText(nodeIdx).map(text => s"dist $text")
    val ranks = pathRenderer.ranksText(nodeIdx).map(text => s"ranks $text")
    val detail = (distance.toList ++ ranks.toList).mkString("  ")
    val message = if (detail.isEmpty) s"node $nodeIdx" else s"node $nodeIdx  $detail"
    pathRenderer.statusListener(message)
  }
}

override def mouseLeft(id: String): Unit = {
  val edge = graph.getEdge(id)
  val uiClass = if (isLarge) LARGE else PLAIN
  if (edge != null) pathRenderer.colorPaths(edge.getNode0.getId.toInt, edge.getNode1.getId.toInt, uiClass)
  else pathRenderer.colorPaths(id.toInt, uiClass)
  pathRenderer.statusListener("")
}
```

Node and edge ids in this viewer are numeric. These `toInt` calls match the listener that already exists.

In `SolveBar.setLegend`:

```scala
private val legend = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0))

def setLegend(entries: Seq[(Int, java.awt.Color)]): Unit = {
  legend.removeAll()
  entries.foreach { (rank, color) =>
    val swatch = new JLabel(rank.toString)
    swatch.setOpaque(true)
    swatch.setBackground(color)
    legend.add(swatch)
  }
  if (legend.getParent == null) add(legend)
  revalidate()
  repaint()
}
```

In `PathViewerFrame.showSolved`, before `activate`:

```scala
renderer.statusListener = solveBar.setStatus
```

`setStatus` is already `String => Unit` if its signature is `def setStatus(text: String): Unit`. Scala eta-expands that as `solveBar.setStatus _` only if needed; writing `solveBar.setStatus` may not eta-expand a method with a side effect name. Assign `text => solveBar.setStatus(text)`.

For `KPaths`, also:

```scala
import com.barrybecker4.discreteoptimization.pathviewer.render.PathColors
solveBar.setLegend(solution.shortestPaths.indices.map(index => (index + 1, PathColors.getColor(index))))
```

For `ToAll` and `OnePath`, call `solveBar.setLegend(Seq.empty)`.

Set the listener inside `activate` so every renderer gets it:

```scala
private def activate(renderer: PathRenderer): Unit = {
  renderer.statusListener = text => solveBar.setStatus(text)
  activeRenderer = Some(renderer)
  renderer.render()
}
```

Do this only when hover fires. `showSolved` sets the summary status before `activate`. The listener must not run until `mouseOver`. `activate` only stores the function. Good.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.render.HoverTextSuite --tests com.barrybecker4.discreteoptimization.pathviewer.render.KShortestPathRendererSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/PathRenderer.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/ShortestPathRenderer.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/KShortestPathRenderer.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/render/GraphViewerListener.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/render/HoverTextSuite.scala
git commit -m "$(cat <<'EOF'
#18 Show hover distance and a k-path color legend

EOF
)"
```

---

### Task 6: Toggle a second algorithm

**Files:**
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala`
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala` (enable Compare; add Show other)
- Modify: `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala`
- Modify: `README.md` (one sentence on Compare)
- Test: extend `scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala`

**Interfaces:**
- Consumes: `LiveSolver.solve`, `SolveResult`, `AlgorithmKind`.
- Produces:
  - `case class ComparedSolutions(first: SolveResult, second: SolveResult, sameTargetCost: Boolean)`
  - `LiveSolver.compare(graph: DirectedGraph, first: SolveRequest, second: SolveRequest): ComparedSolutions`
  - `def targetCost(result: SolveResult, target: Int): Option[Double]` — finite cost of the path to `target`. For `KPaths`, the first path’s weight. For `OnePath`, that path’s weight when it is finite. For `ToAll`, the finite path whose last node is `target`.
  - `SolveBar.setCompareEnabled(enabled: Boolean)` already exists from Task 2. Call it with `true` from `presentDigraph` and `loadTheGraph`.
  - `SolveBar` button `Show other`, disabled until two results exist. `def onShowOther(action: () => Unit)` registers the click.

`sameTargetCost` is true only when both `targetCost` values are defined and equal.

- [ ] **Step 1: Write the failing test**

Append to `LiveSolverSuite`:

```scala
test("compare reports the shared target cost for Dijkstra and Modified Dijkstra") {
  val compared = LiveSolver.compare(
    triangle,
    SolveRequest(Dijkstra, 0, 2, 3),
    SolveRequest(ModifiedDijkstra, 0, 2, 3)
  )
  assert(LiveSolver.targetCost(compared.first, 2).contains(3.0))
  assert(LiveSolver.targetCost(compared.second, 2).contains(3.0))
  assert(compared.sameTargetCost)
}

test("compare reports different target costs when the second request starts elsewhere") {
  val compared = LiveSolver.compare(
    triangle,
    SolveRequest(Dijkstra, 0, 2, 3),
    SolveRequest(Dijkstra, 1, 2, 3)
  )
  assert(LiveSolver.targetCost(compared.second, 2).contains(2.0))
  assert(!compared.sameTargetCost)
}

test("Yen target cost is the cheapest path weight") {
  val result = LiveSolver.solve(triangle, SolveRequest(Yen, 0, 2, 2))
  assert(LiveSolver.targetCost(result, 2).contains(3.0))
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite`

Expected: FAIL. `LiveSolver.compare` and `targetCost` are not defined.

- [ ] **Step 3: Write the minimal implementation**

Add to `LiveSolver.scala`:

```scala
case class ComparedSolutions(first: SolveResult, second: SolveResult, sameTargetCost: Boolean)

def targetCost(result: SolveResult, target: Int): Option[Double] = result match {
  case SolveResult.ToAll(solution, _) =>
    solution.paths.find(path => path.nodes.nonEmpty && path.lastNode == target && !path.weight.isPosInfinity).map(_.weight)
  case SolveResult.OnePath(path, _) =>
    if (path.nodes.nonEmpty && !path.weight.isPosInfinity) Some(path.weight) else None
  case SolveResult.KPaths(solution) =>
    solution.shortestPaths.headOption.filter(path => path.nodes.nonEmpty && !path.weight.isPosInfinity).map(_.weight)
}

def compare(graph: DirectedGraph, first: SolveRequest, second: SolveRequest): ComparedSolutions = {
  val a = solve(graph, first)
  val b = solve(graph, second)
  val same = (targetCost(a, first.target), targetCost(b, second.target)) match {
    case (Some(left), Some(right)) => left == right
    case _ => false
  }
  ComparedSolutions(a, b, same)
}
```

`compare` scores each result at that request’s own target. The Solve bar’s second request copies source, target, and `k`, and changes only the algorithm. `SolveRequest` is a case class, so `copy` exists.

```scala
private def secondRequest(first: SolveRequest): Option[SolveRequest] =
  solveBar.compareAlgorithm().map(kind => first.copy(algorithm = kind))
```

Enable compare when a graph is loaded:

```scala
solveBar.setCompareEnabled(true)
```

in `presentDigraph` and `loadTheGraph`.

Add the Show other button to `SolveBar`, initially disabled:

```scala
private val showOther = new JButton("Show other")
private var showOtherAction: () => Unit = () => ()
showOther.setEnabled(false)
showOther.addActionListener(_ => showOtherAction())
add(showOther)

def setShowOtherEnabled(enabled: Boolean): Unit = showOther.setEnabled(enabled)

def onShowOther(action: () => Unit): Unit = showOtherAction = action
```

Call `solveBar.onShowOther(() => toggleShown())` once, from the `PathViewerFrame` constructor.

Both `solve` and `solveNow` share `prepare`, which is safe to run off the EDT. `solveNow` calls it directly. `solve` runs it inside `doInBackground` and calls `publishPrepared` from `done`.

```scala
private case class Prepared(
    request: SolveRequest,
    result: SolveResult,
    other: Option[(SolveRequest, SolveResult)],
    summary: String
)

private var shown: Option[Prepared] = None
private var showingSecond = false

private def prepare(digraph: DirectedGraph, valid: SolveRequest): Prepared =
  secondRequest(valid) match {
    case Some(other) if other.algorithm != valid.algorithm =>
      val compared = LiveSolver.compare(digraph, valid, other)
      val agreement = if (compared.sameTargetCost) "same target cost" else "target costs differ"
      Prepared(
        valid,
        compared.first,
        Some((other, compared.second)),
        s"${valid.algorithm} vs ${other.algorithm}: $agreement"
      )
    case _ =>
      Prepared(valid, LiveSolver.solve(digraph, valid), None, "")
  }

private def publishPrepared(prepared: Prepared): Unit = {
  showingSecond = false
  shown = prepared.other.map(_ => prepared)
  solveBar.setShowOtherEnabled(prepared.other.isDefined)
  showSolved(prepared.request, prepared.result)
  if (prepared.summary.nonEmpty) solveBar.setStatus(prepared.summary)
}

private def toggleShown(): Unit =
  shown.foreach { prepared =>
    val (request, result) = prepared.other match {
      case Some((otherRequest, otherResult)) if !showingSecond => (otherRequest, otherResult)
      case _ => (prepared.request, prepared.result)
    }
    showingSecond = prepared.other.isDefined && !showingSecond
    showSolved(request, result)
  }
```

`showSolved` already picks `KShortestPathRenderer` for `KPaths` and the frontier renderer only when Animate is checked and the shown result has a non-empty trace. Set the agreement status after `showSolved`, because `showSolved` writes its own status line. `solveNow` becomes `publishPrepared(prepare(digraph, valid))`.

Add this README sentence after the Solve bar sentence:

```markdown
Compare runs a second algorithm on the same source, target, and k, then Show other swaps which result is drawn. The status line says whether the two target costs match.
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.LiveSolverSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.graph.algorithms.shortestpaths.* --tests com.barrybecker4.discreteoptimization.pathviewer.*`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add scala-source/com/barrybecker4/discreteoptimization/pathviewer/LiveSolver.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala \
  scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala \
  scala-test/com/barrybecker4/discreteoptimization/pathviewer/LiveSolverSuite.scala \
  README.md
git commit -m "$(cat <<'EOF'
#18 Compare two algorithms on the same loaded graph

EOF
)"
```

---

## Spec coverage

- Select algorithm, source, target, and `k`: Task 2.
- Run the solver on the in-memory digraph, and still open golden files: Task 2. Golden File menu items stay.
- Stepped frontier animation: Tasks 3 and 4. Yen keeps its path walk (Global Constraints).
- Distance on hover and k-path color legend: Task 5.
- Compare two algorithms: Task 6, as a toggle.
- Source styling is not `node#0`: already true; Task 2 passes the chosen source into `createGraph`, and `PathViewerSolveSuite` checks node 2.

## Out of scope

- Replaying every Yen deviation search.
- A second GraphStream window.
- Eppstein, fixture regeneration, and benchmark thresholds.
