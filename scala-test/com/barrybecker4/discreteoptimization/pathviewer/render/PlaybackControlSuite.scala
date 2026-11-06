package com.barrybecker4.discreteoptimization.pathviewer.render

import org.scalatest.funsuite.AnyFunSuite

class PlaybackControlSuite extends AnyFunSuite {

  test("step releases a paused waiter once") {
    val control = new PlaybackControl
    control.pause()
    val waiter = new Thread(() => {
      assert(control.awaitAdvance(2_000))
      assert(!control.awaitAdvance(50))
      ()
    })
    waiter.start()
    control.step()
    waiter.join(3_000)
    assert(!waiter.isAlive)
  }

  test("skip reports shouldStop and releases the waiter") {
    val control = new PlaybackControl
    control.pause()
    val waiter = new Thread(() => {
      assert(control.awaitAdvance(2_000))
      ()
    })
    waiter.start()
    control.skip()
    waiter.join(3_000)
    assert(control.shouldStop)
    assert(!waiter.isAlive)
  }
}
