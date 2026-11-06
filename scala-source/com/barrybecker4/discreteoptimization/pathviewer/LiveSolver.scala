package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.solver.{DijkstrasPathSolver, ModifiedDijkstrasPathSolver}
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.{SearchObserver, SearchTrace}
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
        val observer = SearchObserver.recording()
        val solution = new DijkstrasPathSolver().findPaths(graph, request.source, observer)
        SolveResult.ToAll(solution, observer.trace)
      case AlgorithmKind.ModifiedDijkstra =>
        val observer = SearchObserver.recording()
        val path = new ModifiedDijkstrasPathSolver()
          .findPath(graph, request.source, request.target, observer)
          .getOrElse(Path.EMPTY_PATH)
        SolveResult.OnePath(path, observer.trace)
      case AlgorithmKind.Yen =>
        val solution = new YensKPathsSolver(request.k)
          .findPaths(graph, request.source, request.target, request.k)
        SolveResult.KPaths(solution)
    }
}
