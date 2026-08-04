package com.barrybecker4.graph

import org.scalatest.funsuite.AnyFunSuite

class PathSuite extends AnyFunSuite {

  test("parses weight and node list from line") {
    val path = new Path("3.5 0 1 2")
    assert(path.weight == 3.5)
    assert(path.nodes == List(0, 1, 2))
    assert(path.lastNode == 2)
    assert(path.containsNode(1))
    assert(!path.containsNode(9))
  }

  test("EMPTY_PATH has infinite weight and no nodes") {
    assert(Path.EMPTY_PATH.weight.isPosInfinity)
    assert(Path.EMPTY_PATH.nodes.isEmpty)
  }

  test("toString round-trips the line format") {
    val path = Path(4.0, List(0, 2, 3))
    assert(path.toString == "4.0 0 2 3")
  }
}
