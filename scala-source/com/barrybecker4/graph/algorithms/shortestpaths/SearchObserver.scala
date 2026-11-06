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
