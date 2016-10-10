package com.lynbrookrobotics.potassium
import squants.Time

abstract class Signal[T] { self =>
  def get: T

  def map[U](t: T => U): Signal[U] = new Signal[U] {
    def get: U = t(self.get)
  }

  def zip[O](other: Signal[O]): Signal[(T, O)] = new Signal[(T, O)] {
    override def get: (T, O) = (self.get, other.get)
  }

  def toPeriodic: PeriodicSignal[T] = new PeriodicSignal[T] {
    val parent = None

    override def currentValue(dt: Time): T = get
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
