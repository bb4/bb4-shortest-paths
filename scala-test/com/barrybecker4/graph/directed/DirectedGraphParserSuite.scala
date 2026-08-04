package com.barrybecker4.graph.directed

import com.barrybecker4.common.geometry.FloatLocation
import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

class DirectedGraphParserSuite extends AnyFunSuite {

  private val parser = DirectedGraphParser()

  test("parses vertices, edges, and optional locations") {
    val input =
      """3 2 true
        |1.0 2.0
        |3.0 4.0
        |5.0 6.0
        |0 1 1.5
        |1 2
        |""".stripMargin
    val graph = parser.parse(Source.fromString(input), "tiny")
    assert(graph.numVertices == 3)
    assert(graph.edges == IndexedSeq(
      DirectedEdge(0, 1, 1.5),
      DirectedEdge(1, 2, 1.0)
    ))
    assert(graph.locations.isDefined)
    assert(graph.locations.get.toSeq == Seq(
      FloatLocation(1.0f, 2.0f),
      FloatLocation(3.0f, 4.0f),
      FloatLocation(5.0f, 6.0f)
    ))
  }

  test("parses graph without locations") {
    val input =
      """2 1 false
        |0 1 2.0
        |""".stripMargin
    val graph = parser.parse(Source.fromString(input), "noloc")
    assert(graph.numVertices == 2)
    assert(graph.locations.isEmpty)
    assert(graph.edges == IndexedSeq(DirectedEdge(0, 1, 2.0)))
  }
}
