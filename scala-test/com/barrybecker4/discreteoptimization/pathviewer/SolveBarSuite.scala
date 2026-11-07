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

    bar.selectAlgorithm(Eppstein)
    assert(bar.kSpec._4)

    bar.selectAlgorithm(Dijkstra)
    assert(!bar.kSpec._4)

    assert(bar.compareAlgorithm().isEmpty)
    assert(!bar.compareBoxEnabled)
  }

  test("Compare checkbox enables second-algorithm combo; compareAlgorithm follows combo") {
    val bar = new SolveBar(_ => (), _ => ())
    bar.setCompareEnabled(true)
    assert(bar.compareBoxEnabled)
    assert(!bar.compareAlgorithmBoxEnabled)

    bar.clickCompareBox()
    assert(bar.compareAlgorithmBoxEnabled)
    assert(bar.compareAlgorithm().contains(Dijkstra))

    bar.selectCompareAlgorithm(ModifiedDijkstra)
    assert(bar.compareAlgorithm().contains(ModifiedDijkstra))

    bar.clickCompareBox()
    assert(!bar.compareAlgorithmBoxEnabled)
    assert(bar.compareAlgorithm().isEmpty)
  }

  test("setCompareEnabled(false) disables Compare and the second-algorithm combo") {
    val bar = new SolveBar(_ => (), _ => ())
    bar.setCompareEnabled(true)
    bar.clickCompareBox()
    assert(bar.compareAlgorithmBoxEnabled)

    bar.setCompareEnabled(false)
    assert(!bar.compareBoxEnabled)
    assert(!bar.compareAlgorithmBoxEnabled)
  }
}
