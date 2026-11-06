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
