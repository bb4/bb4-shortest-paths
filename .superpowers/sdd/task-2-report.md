# Task 2 Report: Solve bar and frame wiring

## What I implemented

- Added `SolveBar` with algorithm, source, target, k, Solve, disabled Compare controls, status text, and the required public interface.
- Added `GraphViewerFrame.presentDigraph` and routed File → Open Graph through it.
- Wired `PathViewerFrame` to remember the loaded digraph, configure the Solve bar, validate requests, solve in a `SwingWorker`, and render Dijkstra, Modified Dijkstra, or Yen results.
- Added the synchronous `solveNow` test seam.
- Preserved and exposed all three File menu actions: Open Graph, Open Shortest Paths, and Open K Shortest Paths.
- Kept GraphStream viewer replacement/disposal synchronized so concurrently executed Swing viewer tests do not race during renderer shutdown.
- Updated the README usage paragraph.

## What I tested

- Focused test:
  - `./gradlew cleanTest test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerReloadSuite`
  - Result: `BUILD SUCCESSFUL`; 2 tests passed.
- Full suite:
  - `./gradlew test`
  - Result: `BUILD SUCCESSFUL`.

## TDD Evidence

### RED

Command:

`./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite`

Relevant failing output:

```text
PathViewerSolveSuite.scala:42:46: Not found: presentDigraph
PathViewerSolveSuite.scala:43:44: Not found: solveNow
PathViewerSolveSuite.scala:44:46: Not found: loadedGraph
Execution failed for task ':compileTestScala'.
BUILD FAILED
```

This was expected because the test exercised the new graph-presentation, synchronous solve, and loaded-graph interfaces before they existed.

### GREEN

Command:

`./gradlew cleanTest test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerReloadSuite`

Relevant passing output:

```text
> Task :test
BUILD SUCCESSFUL
```

The new solve behavior passed, and the reload regression test confirmed that replacing a graph still stops the previous renderer pump.

## Files changed

- `README.md`
- `scala-source/com/barrybecker4/discreteoptimization/pathviewer/SolveBar.scala`
- `scala-source/com/barrybecker4/discreteoptimization/pathviewer/PathViewerFrame.scala`
- `scala-source/com/barrybecker4/graph/visualization/GraphViewerFrame.scala`
- `scala-test/com/barrybecker4/discreteoptimization/pathviewer/PathViewerSolveSuite.scala`

## Self-review findings

- Completeness: all interfaces and exact UI/status behavior from the brief are implemented.
- Quality: solving remains off the Swing event thread; graph replacement continues to stop active renderers.
- YAGNI: no comparison behavior was added; its controls remain disabled for Task 6.
- Tests exercise real GraphStream rendering and pump lifecycle behavior.
- The brief used `compareAlgorithm` for both a private field and a required method, which does not compile in Scala. The private widget was named `compareAlgorithmBox` while preserving the required method.

## Issues or concerns

None.

## Fix: stale solve result on graph change (review)

### What changed

- `showSolved` now takes the `DirectedGraph` used for that solve (from `solveNow` or captured in the worker) instead of re-reading `loaded`, so solutions are not rendered on a graph loaded after the worker started.
- The `SwingWorker.done()` path compares `loaded` to that digraph: if the user loaded another graph, status is set to `Solve discarded because the graph changed` and `showSolved` is skipped.
- `done()` wraps `get()` and `showSolved` in a broader catch: `CancellationException`, `InterruptedException` (interrupt flag restored), `ExecutionException` (status from cause), and any other `Exception` from `showSolved` (status from `toString`). `setBusy(false)` runs in `finally` so the busy state always clears.

### Tests

Command:

`./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerReloadSuite`

Relevant output:

```text
> Task :test
BUILD SUCCESSFUL in 4s
```

### Commit

- `59bfe21` — #18 Tie async solve UI to the graph that was solved

## Fix: Task 2 review — discard check and Solve bar contract tests

### What changed

- Added `PathViewerFrame.currentGraphIs(digraph)` (`loaded.contains`) and used it in the async solve worker discard path instead of an inline `loaded.contains` / redundant local.
- Added `SolveBar` package-private `selectAlgorithm`, `kSpec` (spinner value, min, max, k enabled), and `compareBoxEnabled` for tests.
- Added `SolveBarSuite` for the default selection, k spec, Yen/Dijkstra k enablement, and compare controls.
- Extended `PathViewerSolveSuite` with Modified Dijkstra source styling, Yen source assertion (composite `ui.class`), and `currentGraphIs` after presenting a second graph (teardown sleeps briefly before `dispose()` so GraphStream can finish after the second `presentDigraph`; no change to `VIEWER_LOCK` or `waitForSourceInViewer`).

### Tests

Command:

`./gradlew test --tests com.barrybecker4.discreteoptimization.pathviewer.SolveBarSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerSolveSuite --tests com.barrybecker4.discreteoptimization.pathviewer.PathViewerReloadSuite`

Relevant output:

```text
> Task :test
BUILD SUCCESSFUL in 6s
```

### Commit

- `#18 Test SolveBar contract and pathviewer graph identity` — full SHA: same revision as this file (see `git log -1 --format=%H` on branch `issue-18-pathviewer-live-solve`)
