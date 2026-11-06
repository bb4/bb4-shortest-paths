package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import org.scalatest.funsuite.AnyFunSuite

class KShortestPathRendererSuite extends AnyFunSuite {

  test("selects non-contiguous path indices instead of a contiguous slice") {
    val paths = Seq(
      Path(1, List(0, 1)),
      Path(2, List(0, 2)),
      Path(3, List(0, 3)),
      Path(4, List(0, 4)),
      Path(5, List(0, 5)),
      Path(6, List(0, 6))
    )
    val selected = KShortestPathRenderer.pathsAt(paths, Seq(0, 2, 5))
    assert(selected.map(_._2) == Seq(0, 2, 5))
    assert(selected.map(_._1) == Seq(paths.head, paths(2), paths(5)))
  }
}
