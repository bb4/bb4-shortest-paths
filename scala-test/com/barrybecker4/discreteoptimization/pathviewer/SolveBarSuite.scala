package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*
import org.scalatest.funsuite.AnyFunSuite

class SolveBarSuite extends AnyFunSuite {

  System.setProperty("org.graphstream.ui", "swing")

  test("selected request, k spec, algorithm selection, and compare controls") {
    val bar = new SolveBar(_ => (), _ => ())

    assert(bar.selectedRequest() == SolveRequest(Dijkstra, 0, 0, 3))
    assert(bar.kSpec == (3, 1, 10, false))

    bar.selectAlgorithm(Yen)
    assert(bar.kSpec._4)

    bar.selectAlgorithm(Dijkstra)
    assert(!bar.kSpec._4)

    assert(bar.compareAlgorithm().isEmpty)
    assert(!bar.compareBoxEnabled)
  }
}
