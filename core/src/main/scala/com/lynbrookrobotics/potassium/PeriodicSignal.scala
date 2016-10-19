package com.lynbrookrobotics.potassium

import squants.Time

abstract class PeriodicSignal[T] { self =>
  private var currentTickSource: Option[AnyRef] = None

  val parent: Option[PeriodicSignal[_]]
  val check: Option[T => Unit]

  private var lastCalculated: Option[(T, Int)] = None

  protected def calculateValue(dt: Time, token: Int): T

  def currentValue(dt: Time, requestToken: Int = PeriodicSignal.requestTokens.next()): T = {
    lastCalculated match {
      case Some((v, t)) if t == requestToken =>
        v
      case _ =>
        val ret = calculateValue(dt, requestToken)
        check.foreach(_(ret))
        lastCalculated = Some((ret, requestToken))

        ret
    }
  }

  def map[U](f: (T, Time) => U): PeriodicSignal[U] = new PeriodicSignal[U] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time, token: Int): U = {
      f(self.currentValue(dt, token), dt)
    }
  }

  def zip[O](other: PeriodicSignal[O]): PeriodicSignal[(T, O)] = new PeriodicSignal[(T, O)] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time, token: Int): (T, O) = {
      (self.currentValue(dt, token), other.currentValue(dt, token))
    }
  }

  def withCheck(checkCallback: T => Unit): PeriodicSignal[T] = new PeriodicSignal[T] {
    val parent = Some(self)
    val check = Some(checkCallback)

    def calculateValue(dt: Time, token: Int): T = self.currentValue(dt, token)
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

object PeriodicSignal {
  private[PeriodicSignal] val requestTokens = Iterator.from(1)
}