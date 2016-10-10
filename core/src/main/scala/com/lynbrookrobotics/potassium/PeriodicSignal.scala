package com.lynbrookrobotics.potassium

import squants.Time

abstract class PeriodicSignal[T] { self =>
  private var currentTickSource: Option[AnyRef] = None

  val parent: Option[PeriodicSignal[_]]

  def currentValue(dt: Time): T

  def map[U](f: (T, Time) => U): PeriodicSignal[U] = new PeriodicSignal[U] {
    val parent = Some(self)
    override def currentValue(dt: Time): U = {
      f(self.currentValue(dt), dt)
    }
  }

  def zip[O](other: PeriodicSignal[O]): PeriodicSignal[(T, O)] = new PeriodicSignal[(T, O)] {
    val parent = Some(self)
    override def currentValue(dt: Time): (T, O) = {
      (self.currentValue(dt), other.currentValue(dt))
    }
  }

  def attachTickSource(source: AnyRef): Unit = {
    if (currentTickSource.isEmpty) {
      currentTickSource = Some(source)
      parent.foreach(_.attachTickSource(source))
    } else if (!currentTickSource.get.eq(source)) {
      throw new IllegalStateException("Cannot attach a controller to two different clocks")
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
