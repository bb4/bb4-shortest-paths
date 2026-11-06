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
