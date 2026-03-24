package com.barrybecker4.graph

import java.io.File
import scala.io.Source
import scala.util.Using

trait Parser[M] {

  def parse(fileName: String): M =
    parse(Source.fromFile(fileName), fileName)

  def parse(file: File, problemName: String): M =
    parse(Source.fromFile(file), problemName)

  def parse(source: Source, name: String): M =
    Using.resource(source)(s => parse(s.getLines().toIndexedSeq, name))

  protected def parse(lines: IndexedSeq[String], problemName: String): M
}
