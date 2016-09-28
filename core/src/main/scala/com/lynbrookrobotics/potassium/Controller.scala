package com.lynbrookrobotics.potassium

import squants.Time

abstract class Controller[T] { self =>
  private var currentTickSource: Option[AnyRef] = None

  val parent: Option[Controller[_]]

  def currentValue(dt: Time): T

  def map[U](f: (T, Time) => U): Controller[U] = new Controller[U] {
    val parent = Some(self)
    override def currentValue(dt: Time): U = {
      f(self.currentValue(dt), dt)
    }
  }

  def zip[O](other: Controller[O]): Controller[(T, O)] = new Controller[(T, O)] {
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
