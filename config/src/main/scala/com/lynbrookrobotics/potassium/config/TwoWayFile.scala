package com.lynbrookrobotics.potassium.config

import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.{FileSystem, FileSystems}

class TwoWayFile(file: File) extends TwoWaySignal[String] {
  new Thread(new Runnable {
    def run(): Unit = {
      while (!Thread.interrupted()) {
        value = scala.io.Source.fromFile(file).getLines.mkString("\n")
        Thread.sleep(1000)
      }
    }
  }).start()

  value = scala.io.Source.fromFile(file).getLines.mkString("\n")

  override def handlePropogate(newValue: String): Unit = {
    val writer = new PrintWriter(new FileOutputStream(file))
    writer.print(newValue)
    writer.close()
  }
}
