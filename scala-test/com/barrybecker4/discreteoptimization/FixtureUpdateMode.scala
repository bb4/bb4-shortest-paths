package com.barrybecker4.discreteoptimization

/** When true, golden solution files are rewritten instead of compared. */
private[discreteoptimization] object FixtureUpdateMode {

  def updateFixtures: Boolean =
    sys.props.get("shortestpaths.updateFixtures").exists(_.equalsIgnoreCase("true")) ||
      sys.env.get("SHORTEST_PATHS_UPDATE_FIXTURES").exists(_.equalsIgnoreCase("true"))
}
