package com.barrybecker4.discreteoptimization.benchmark

/** Path-cost comparison that treats positive infinities as equal and ignores tiny float noise. */
object PathCosts {

  def sameCost(left: Double, right: Double): Boolean =
    if (left.isInfinite || right.isInfinite) left.isPosInfinity && right.isPosInfinity
    else (left - right).abs <= 1e-6

  def sameCosts(left: Seq[Double], right: Seq[Double]): Boolean =
    left.length == right.length && left.zip(right).forall { case (a, b) => sameCost(a, b) }
}
