package com.team846.frc2015.logging

import java.util
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue


object AsyncLogger {
  abstract private class Loggable {
    def log(): Unit
  }

  private final class InfoLog(message: String) extends Loggable {
    override def log(): Unit = {
      Logger.info(message)
    }
  }

  private final class DebugLog(message: String) extends Loggable {
    override def log(): Unit = {
      Logger.debug(message)
    }
  }

  private final class ErrorLog(message: String) extends Loggable {
    override def log(): Unit = {
      Logger.error(message)
    }
  }

  private final class WarningLog(message: String) extends Loggable {
    override def log(): Unit = {
      Logger.warning(message)
    }
  }

  private val gapBetweenFlush = 20 // ms

  private val toLog = new ConcurrentLinkedQueue[Loggable]()

  def debug(msg: String): Unit = {
    toLog.add(new DebugLog(msg))
  }

  def warn(msg: String): Unit = {
    toLog.add(new WarningLog(msg))
  }

  def error(msg: String): Unit = {
    toLog.add(new ErrorLog(msg))
  }

  def info(msg: String): Unit = {
    toLog.add(new InfoLog(msg))
  }

  val timer = new Timer
  timer.scheduleAtFixedRate(new TimerTask() {
    override def run(): Unit = {
      while ( {
        !toLog.isEmpty
      }) {
        toLog.remove().log()
      }
    }
  }, 0, gapBetweenFlush)
}
