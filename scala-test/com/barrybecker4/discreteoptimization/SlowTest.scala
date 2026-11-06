package com.barrybecker4.discreteoptimization

import org.scalatest.Tag

/**
 * Large fixtures such as `road_network_01`.
 * Everyday `./gradlew test` skips these. Opt in with `-Dshortestpaths.includeSlow=true`.
 */
object SlowTest extends Tag("com.barrybecker4.discreteoptimization.SlowTest") {

  def include: Boolean =
    sys.props.get("shortestpaths.includeSlow").exists(_.equalsIgnoreCase("true"))
}
