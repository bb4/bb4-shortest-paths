package com.barrybecker4.discreteoptimization.traffic.graph

import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source

class TrafficGraphParserSuite extends AnyFunSuite {

  private val parser = TrafficGraphParser()

  test("parse minimal valid traffic graph") {
    val text =
      """2 1 0
        |0 0 DUMB_TRAFFIC_SIGNAL 0 10
        |100 0 DUMB_TRAFFIC_SIGNAL 180 10
        |0 0 1 0""".stripMargin
    val tg = parser.parse(Source.fromString(text), "test")
    assert(tg.numIntersections == 2)
    assert(tg.streets.size == 1)
    assert(tg.neighborsOf(0).map(_.id).headOption.contains(1))
  }

  test("reject duplicate street on same intersection port") {
    val text =
      """2 2 0
        |0 0 DUMB_TRAFFIC_SIGNAL 0 10
        |100 0 DUMB_TRAFFIC_SIGNAL 180 10
        |0 0 1 0
        |0 0 1 1""".stripMargin
    intercept[IllegalStateException] {
      parser.parse(Source.fromString(text), "test")
    }
  }
}
