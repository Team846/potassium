package com.lynbrookrobotics.potassium.logging

import java.io.PrintStream

object Logger {
  private val originalOut = Console.out

  def initialize(): Unit = {
    Console.setOut(new PrintStream(originalOut) {
      private def toString(obj: Any) = obj.toString

      override def println(): Unit = {
        debug("")
      }

      override def println(x: Boolean): Unit = {
        debug(toString(x))
      }

      override def println(x: Char): Unit = {
        debug(toString(x))
      }

      override def println(x: Int): Unit = {
        debug(toString(x))
      }

      override def println(x: Long): Unit = {
        debug(toString(x))
      }

      override def println(x: Float): Unit = {
        debug(toString(x))
      }

      override def println(x: Double): Unit = {
        debug(toString(x))
      }

      override def println(x: Array[Char]): Unit = {
        debug(toString(x))
      }

      override def println(x: String): Unit = {
        debug(toString(x))
      }

      override def println(x: Any): Unit = {
        debug(toString(x))
      }
    })

    Console.setErr(new PrintStream(originalOut) {
      private def toString(obj: Any) = obj.toString

      override def println(): Unit = {
        error("")
      }

      override def println(x: Boolean): Unit = {
        error(toString(x))
      }

      override def println(x: Char): Unit = {
        error(toString(x))
      }

      override def println(x: Int): Unit = {
        error(toString(x))
      }

      override def println(x: Long): Unit = {
        error(toString(x))
      }

      override def println(x: Float): Unit = {
        error(toString(x))
      }

      override def println(x: Double): Unit = {
        error(toString(x))
      }

      override def println(x: Array[Char]): Unit = {
        error(toString(x))
      }


      override def println(x: String): Unit = {
        error(toString(x))
      }


      override def println(x: Any): Unit = {
        error(toString(x))
      }
    })
  }

  def info(message: String): Unit = {
    originalOut.println("[INFO] " + message)
  }

  def debug(message: String): Unit = {
    originalOut.println("[DEBUG] " + message)
  }

  def error(message: String): Unit = {
    originalOut.println("[ERROR] " + message)
  }

  def warning(message: String): Unit = {
    originalOut.println("[WARNING] " + message)
  }
}
