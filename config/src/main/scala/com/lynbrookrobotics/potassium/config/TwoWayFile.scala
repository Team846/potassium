package com.lynbrookrobotics.potassium.config

import java.io.{File, PrintWriter}
import java.nio.file.{Files, StandardCopyOption}

class TwoWayFile(file: File) extends TwoWaySignal[String] {
  private val thread = new Thread(() => {
    try {
      while (!Thread.interrupted()) {
        value = scala.io.Source.fromFile(file).getLines.mkString("\n")
        Thread.sleep(1000)
      }
    } catch {
      case _: InterruptedException =>
    }
  })

  thread.start()

  value = scala.io.Source.fromFile(file).getLines.mkString("\n")

  override def handlePropogate(newValue: String): Unit = {
    val tempFile = new File(file.getPath + "-new")
    if(!tempFile.exists()){
      tempFile.createNewFile()
    }

    val writer = new PrintWriter(tempFile)
    writer.print(newValue)
    writer.close()

    Files.move(tempFile.toPath, file.toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  def close(): Unit = {
    thread.interrupt()
  }
}
