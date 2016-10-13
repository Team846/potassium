package com.lynbrookrobotics.potassium
import com.lynbrookrobotics.potassium.events.{ContinuousEvent, EventPolling}
import squants.Time

abstract class Signal[T] { self =>
  def get: T

  def map[U](t: T => U): Signal[U] = new Signal[U] {
    def get: U = t(self.get)
  }

  def zip[O](other: Signal[O]): Signal[(T, O)] = new Signal[(T, O)] {
    def get = (self.get, other.get)
  }

  def filter(condition: T => Boolean)(implicit polling: EventPolling): ContinuousEvent = {
    new ContinuousEvent(condition(get))
  }

  def toPeriodic: PeriodicSignal[T] = new PeriodicSignal[T] {
    val check = None
    val parent = None

    def calculateValue(dt: Time): T = get
  }
}

object Signal {
  def apply[T](f: => T) = new Signal[T] {
    def get: T = f
  }

  def constant[T](v: T) = new Signal[T] {
    def get: T = v
  }
}
