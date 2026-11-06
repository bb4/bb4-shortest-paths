package com.barrybecker4.discreteoptimization.traffic.graph.model

case class Street(intersectionIdx1: Int, 
                  portIdx1: Int, 
                  intersectionIdx2: Int, 
                  portIdx2: Int) {

  override def toString: String =
    s"intersectionIdx1=$intersectionIdx1, portIdx1=$portIdx1; intersectionIdx2=$intersectionIdx2, portIdx2=$portIdx2"
}