package com.barrybecker4.graph.algorithms.shortestpaths

enum SearchDirection {
  case Forward, Backward
}

enum SearchEvent {
  case Frontier(vertex: Int, distance: Double, direction: SearchDirection = SearchDirection.Forward)
  case Extracted(vertex: Int, direction: SearchDirection = SearchDirection.Forward)
  case Settled(vertex: Int, direction: SearchDirection = SearchDirection.Forward)
}

case class SearchTrace(events: Vector[SearchEvent])

object SearchTrace {
  val empty: SearchTrace = SearchTrace(Vector.empty)
}

trait SearchObserver {
  def frontier(vertex: Int, distance: Double, direction: SearchDirection = SearchDirection.Forward): Unit
  def extracted(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit
  def settled(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit
}

object SearchObserver {
  val None: SearchObserver = new SearchObserver {
    def frontier(vertex: Int, distance: Double, direction: SearchDirection = SearchDirection.Forward): Unit = ()
    def extracted(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit = ()
    def settled(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit = ()
  }

  def recording(): RecordingSearchObserver = new RecordingSearchObserver
}

final class RecordingSearchObserver extends SearchObserver {
  private val buffer = Vector.newBuilder[SearchEvent]
  def frontier(vertex: Int, distance: Double, direction: SearchDirection = SearchDirection.Forward): Unit =
    buffer += SearchEvent.Frontier(vertex, distance, direction)
  def extracted(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit =
    buffer += SearchEvent.Extracted(vertex, direction)
  def settled(vertex: Int, direction: SearchDirection = SearchDirection.Forward): Unit =
    buffer += SearchEvent.Settled(vertex, direction)
  def trace: SearchTrace = SearchTrace(buffer.result())
}
