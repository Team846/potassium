package com.lynbrookrobotics.potassium.config

import java.io.{File, FileOutputStream, PrintWriter}

class TwoWayFile(file: File) extends TwoWaySignal[String] {
  private var isWriting = false
  private val thread = new Thread(() => {
    try {
      while (!Thread.interrupted()) {
        if (!isWriting) {
          value = scala.io.Source.fromFile(file).getLines.mkString("\n")
        }

        Thread.sleep(1000)
      }
    } catch {
      case _: InterruptedException =>
    }
  })

  thread.start()

  value = scala.io.Source.fromFile(file).getLines.mkString("\n")

  override def handlePropogate(newValue: String): Unit = {
    isWriting = true
    println("writing new value")

    val writer = new PrintWriter(new FileOutputStream(file))
    writer.print(newValue)
    writer.close()

    isWriting = false
  }

  def close(): Unit = {
    thread.interrupt()
  }
}
