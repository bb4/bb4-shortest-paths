package com.barrybecker4.discreteoptimization.benchmark

import com.barrybecker4.discreteoptimization.kshortestpaths.KShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.discreteoptimization.shortestpaths.ShortestPathsTstUtil
import com.barrybecker4.discreteoptimization.shortestpaths.solver.{DijkstrasPathSolver, ModifiedDijkstrasPathSolver}
import com.barrybecker4.graph.GraphTstUtil

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

/**
 * Accuracy and timing baseline. Not part of `./gradlew test`.
 * Timing numbers are recorded and are not a pass/fail gate.
 * Cost agreement and golden files are.
 *
 * Run: ./gradlew benchmark
 */
object BaselineBenchmark {

  final case class BenchCase(graph: String, role: String, yenDestination: Int, yenK: Int)

  /** Same destination and k as the Yen golden files. */
  val cases: Seq[BenchCase] = Seq(
    BenchCase("tiny_graph_01", "smoke", 5, 2),
    BenchCase("tiny_graph_02", "smoke", 8, 2),
    BenchCase("sp_4_1", "smoke", 2, 2),
    BenchCase("sp_4_2", "smoke", 2, 2),
    BenchCase("sp_10_1", "smoke", 5, 3),
    BenchCase("sp_400", "medium", 100, 40),
    BenchCase("sp_500", "medium", 200, 50),
    BenchCase("sp_700", "medium", 40, 10),
    BenchCase("road_network_01", "large", 11000, 3)
  )

  val OutputPath = "benchmarks/baseline/baseline.csv"

  private val CsvHeader =
    "graph,role,solver,source,destination,k,warmup_runs,wall_time_ms,total_cost,num_paths"

  def main(args: Array[String]): Unit = {
    val rows = scala.collection.mutable.ListBuffer.empty[String]
    val failures = scala.collection.mutable.ListBuffer.empty[String]

    println("solver timings are informational; cost and golden mismatches fail the run")
    println(f"${"graph"}%-18s ${"role"}%-8s ${"solver"}%-20s ${"warmup"}%6s ${"ms"}%8s ${"total_cost"}%16s ${"paths"}%6s")

    for (bench <- cases) {
      val graph = GraphTstUtil.getGraph(bench.graph)
      val warmup = if (bench.role == "large") 0 else 1

      val (dijkstra, dijkstraMs) = measure(warmup)(DijkstrasPathSolver().findPaths(graph))
      emit(rows, bench, "dijkstra", None, None, warmup, dijkstraMs, dijkstra.totalCost, dijkstra.paths.size)
      checkGolden(
        failures,
        s"${bench.graph} dijkstra",
        dijkstra.toString + "\n",
        ShortestPathsTstUtil.getSerializedSolution(s"${bench.graph}_dijkstra_solution.txt")
      )

      val (modified, modifiedMs) = measure(warmup)(ModifiedDijkstrasPathSolver().findPaths(graph))
      emit(rows, bench, "modified_dijkstra", None, None, warmup, modifiedMs, modified.totalCost, modified.paths.size)
      checkGolden(
        failures,
        s"${bench.graph} modified_dijkstra",
        modified.toString + "\n",
        ShortestPathsTstUtil.getSerializedSolution(s"${bench.graph}_modified_dijkstra_solution.txt")
      )
      if (!PathCosts.sameCosts(dijkstra.paths.map(_.weight), modified.paths.map(_.weight)))
        failures += s"${bench.graph}: Dijkstra and Modified Dijkstra path costs differ for source 0"

      val (yen, yenMs) = measure(warmup) {
        YensKPathsSolver().findPaths(graph, 0, bench.yenDestination, bench.yenK)
      }
      emit(
        rows, bench, "yens_kpaths", Some(bench.yenDestination), Some(bench.yenK),
        warmup, yenMs, yen.totalCost, yen.shortestPaths.size
      )
      checkGolden(
        failures,
        s"${bench.graph} yens_kpaths",
        yen.toString + "\n",
        KShortestPathsTstUtil.getSerializedSolution(s"${bench.graph}_yens_kpaths_solution.txt")
      )
      val dijkstraCost = dijkstra.paths(bench.yenDestination).weight
      yen.shortestPaths.headOption match {
        case Some(first) if !PathCosts.sameCost(first.weight, dijkstraCost) =>
          failures += s"${bench.graph}: Yen first-path cost ${first.weight} != Dijkstra cost $dijkstraCost to ${bench.yenDestination}"
        case None if !dijkstraCost.isPosInfinity =>
          failures += s"${bench.graph}: Yen found no path to ${bench.yenDestination} but Dijkstra cost is $dijkstraCost"
        case _ => ()
      }
    }

    val path = Paths.get(OutputPath)
    Files.createDirectories(path.getParent)
    Files.writeString(path, (CsvHeader +: rows.toSeq).mkString("\n") + "\n", StandardCharsets.UTF_8)
    println(s"wrote $OutputPath")

    if (failures.nonEmpty) {
      failures.foreach(message => System.err.println(message))
      sys.exit(1)
    }
  }

  private def emit(
      rows: scala.collection.mutable.ListBuffer[String],
      bench: BenchCase,
      solver: String,
      destination: Option[Int],
      k: Option[Int],
      warmup: Int,
      millis: Long,
      totalCost: Double,
      numPaths: Int
  ): Unit = {
    val line = Seq(
      bench.graph,
      bench.role,
      solver,
      "0",
      destination.fold("")(_.toString),
      k.fold("")(_.toString),
      warmup.toString,
      millis.toString,
      totalCost.toString,
      numPaths.toString
    ).mkString(",")
    rows += line
    println(f"${bench.graph}%-18s ${bench.role}%-8s ${solver}%-20s ${warmup}%6d ${millis}%8d ${totalCost}%16s ${numPaths}%6d")
  }

  private def checkGolden(
      failures: scala.collection.mutable.ListBuffer[String],
      label: String,
      actual: String,
      expected: String
  ): Unit =
    if (actual != expected)
      failures += s"$label golden mismatch (${describeMismatch(actual, expected)})"

  private def describeMismatch(actual: String, expected: String): String = {
    val a = actual.split("\n", -1)
    val e = expected.split("\n", -1)
    val index = (0 until math.max(a.length, e.length)).find(i => a.lift(i) != e.lift(i)).getOrElse(0)
    val exp = e.lift(index).getOrElse("<eof>")
    val act = a.lift(index).getOrElse("<eof>")
    val clip = (s: String) => if (s.length > 80) s.take(80) + "..." else s
    s"line ${index + 1}: expected '${clip(exp)}' actual '${clip(act)}'"
  }

  private def measure[A](warmupRuns: Int)(block: => A): (A, Long) = {
    var i = 0
    while (i < warmupRuns) {
      block
      i += 1
    }
    val start = System.nanoTime()
    val result = block
    val millis = (System.nanoTime() - start) / 1000000L
    (result, millis)
  }
}
