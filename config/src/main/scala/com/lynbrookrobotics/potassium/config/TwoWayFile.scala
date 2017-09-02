package com.lynbrookrobotics.potassium.config

import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.{FileSystem, FileSystems, Files}

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
    val tempFile = new File(file.getPath + "-new")
    if(!tempFile.exists()){
      tempFile.createNewFile()
    }
    val fileOutputMain = new FileOutputStream(file)
    val writer = new PrintWriter(new FileOutputStream(tempFile))
    writer.print(newValue)
    writer.close()
    Files.copy(tempFile.toPath, fileOutputMain)
  }
}
