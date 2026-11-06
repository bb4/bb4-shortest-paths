package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.graph.Path
import org.scalatest.funsuite.AnyFunSuite

class HoverTextSuite extends AnyFunSuite {

  test("distance text uses the finite path that ends at the node") {
    val solution = ShortestPathsSolution(4.0, List(
      Path(0.0, List(0)),
      Path(1.0, List(0, 1)),
      Path(Double.PositiveInfinity, List())
    ))
    assert(ShortestPathRenderer.distanceText(solution, 1).contains("1"))
    assert(ShortestPathRenderer.distanceText(solution, 2).contains("unreachable"))
    assert(ShortestPathRenderer.distanceText(solution, 9).isEmpty)
  }

  test("one-path solution does not use its path as a vertex-indexed fallback") {
    val solution = ShortestPathsSolution(
      Double.PositiveInfinity,
      List(Path(Double.PositiveInfinity, List()))
    )
    assert(ShortestPathRenderer.distanceText(solution, 0).isEmpty)
  }

  test("rank text is 1-based and lists every path through the node") {
    val solution = KShortestPathsSolution(7.0, 2, 2, Seq(
      Path(3.0, List(0, 1, 2)),
      Path(4.0, List(0, 2))
    ))
    assert(KShortestPathRenderer.ranksText(solution, 1).contains("1"))
    assert(KShortestPathRenderer.ranksText(solution, 2).contains("1, 2"))
    assert(KShortestPathRenderer.ranksText(solution, 4).isEmpty)
  }
}
