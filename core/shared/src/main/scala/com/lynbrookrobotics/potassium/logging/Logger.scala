package com.lynbrookrobotics.potassium.logging

object Logger {
  def info(message: String): Unit = {
    Console.out.println("[INFO] " + message)
  }

  def debug(message: String): Unit = {
    Console.out.println("[DEBUG] " + message)
  }

  def error(message: String): Unit = {
    Console.out.println("[ERROR] " + message)
  }

  def warning(message: String): Unit = {
    Console.out.println("[WARNING] " + message)
  }
}
