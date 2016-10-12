package com.lynbrookrobotics.potassium

import squants.Time

abstract class PeriodicSignal[T] { self =>
  private var currentTickSource: Option[AnyRef] = None

  val parent: Option[PeriodicSignal[_]]
  val check: Option[T => Unit]

  protected def calculateValue(dt: Time): T

  def currentValue(dt: Time): T = {
    val ret = calculateValue(dt)
    check.foreach(_(ret))

    ret
  }

  def map[U](f: (T, Time) => U): PeriodicSignal[U] = new PeriodicSignal[U] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time): U = {
      f(self.currentValue(dt), dt)
    }
  }

  def zip[O](other: PeriodicSignal[O]): PeriodicSignal[(T, O)] = new PeriodicSignal[(T, O)] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time): (T, O) = {
      (self.currentValue(dt), other.currentValue(dt))
    }
  }

  def withCheck(checkCallback: T => Unit): PeriodicSignal[T] = new PeriodicSignal[T] {
    val parent = Some(self)
    val check = Some(checkCallback)

    def calculateValue(dt: Time): T = self.currentValue(dt)
  }

  def attachTickSource(source: AnyRef): Unit = {
    if (currentTickSource.isEmpty) {
      currentTickSource = Some(source)
      parent.foreach(_.attachTickSource(source))
    } else if (!currentTickSource.get.eq(source)) {
      throw new IllegalStateException("Cannot attach a periodic signal to two different clocks")
    }
  }

  def detachTickSource(source: AnyRef): Unit = {
    currentTickSource.foreach { s =>
      if (s.eq(source)) {
        currentTickSource = None
        parent.foreach(_.detachTickSource(source))
      }
    }
  }
}
