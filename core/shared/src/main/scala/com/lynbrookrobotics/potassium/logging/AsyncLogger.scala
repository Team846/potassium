package com.lynbrookrobotics.potassium.logging

import com.lynbrookrobotics.potassium.clock.Clock
import squants.time.Milliseconds

import scala.collection.mutable

trait AsyncLogger {
  val clock: Clock

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

  private val gapBetweenFlush = Milliseconds(20)

  private val toLog = new mutable.SynchronizedQueue[Loggable]()

  def debug(msg: String): Unit = {
    toLog.+=(new DebugLog(msg))
  }

  def warn(msg: String): Unit = {
    toLog.+=(new WarningLog(msg))
  }

  def error(msg: String): Unit = {
    toLog.+=(new ErrorLog(msg))
  }

  def info(msg: String): Unit = {
    toLog.+=(new InfoLog(msg))
  }

  clock.apply(gapBetweenFlush)(
    _ =>
      while (toLog.nonEmpty) {
        toLog.dequeue().log()
    }
  )
}
