package com.lynbrookrobotics.potassium.config

import java.io.{File, FileOutputStream, PrintWriter}

class TwoWayFile(file: File) extends TwoWaySignal[String] {
  override def value: String = {
    scala.io.Source.fromFile(file).getLines.mkString("\n")
  }

  override def reversePropagate(newValue: String): Unit = {
    val writer = new PrintWriter(new FileOutputStream(file))
    writer.print(newValue)
    writer.close()
  }
}
