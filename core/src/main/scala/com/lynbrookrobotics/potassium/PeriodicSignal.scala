package com.lynbrookrobotics.potassium

import squants.Time

/**
  * Represents a signal that requires a fixed update interval
  *
  * This type of signal is used in cases where history must be tracked,
  * as it prevents collisions between multiple value requests and includes
  * a dt value for use in calculations.
  *
  * @tparam T the type of values the signal will produce
  */
abstract class PeriodicSignal[T] { self =>
  private var currentTickSource: Option[AnyRef] = None

  protected val parent: Option[PeriodicSignal[_]]
  protected val check: Option[T => Unit]

  private var lastCalculated: Option[(T, Int)] = None

  protected def calculateValue(dt: Time, token: Int): T

  /**
    * Gets the latest value from the signal
    * @param dt the time since the last value request
    * @param requestToken an optional token to prevent recalculations of a
    *                     value in a branched dependency
    * @return the latest value of the signal
    */
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

  /**
    * Transforms the periodic signal by applying a function to all values
    * @param f the function to transform values with
    * @tparam U the type of the resulting signal
    * @return a new signal with values transformed by the function
    */
  def map[U](f: (T, Time) => U): PeriodicSignal[U] = new PeriodicSignal[U] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time, token: Int): U = {
      f(self.currentValue(dt, token), dt)
    }
  }

  /**
    * Combines the signal with another signal into a signal of tuples
    * @param other the signal to combine with
    * @tparam O the type of values of the other signal
    * @return a new signal that returns tuples with one value from each signal
    */
  def zip[O](other: PeriodicSignal[O]): PeriodicSignal[(T, O)] = new PeriodicSignal[(T, O)] {
    val parent = Some(self)
    val check = None

    def calculateValue(dt: Time, token: Int): (T, O) = {
      (self.currentValue(dt, token), other.currentValue(dt, token))
    }
  }

  /**
    * Creates a new periodic signal that returns the same value but also invokes the given callback
    * @param checkCallback the callback to run with each value calculated by the signal
    * @return a periodic signal with the callback invocations
    */
  def withCheck(checkCallback: T => Unit): PeriodicSignal[T] = new PeriodicSignal[T] {
    val parent = Some(self)
    val check = Some(checkCallback)

    def calculateValue(dt: Time, token: Int): T = self.currentValue(dt, token)
  }

  /**
    * Marks the object that is producing ticks for the signal.
    *
    * This is needed in order to prevent tick collisions with multiple sources of ticks.
    *
    * @param source an object to mark as a tick source
    */
  def attachTickSource(source: AnyRef): Unit = {
    if (currentTickSource.isEmpty) {
      currentTickSource = Some(source)
      parent.foreach(_.attachTickSource(source))
    } else if (!currentTickSource.get.eq(source)) {
      throw new IllegalStateException("Cannot attach a periodic signal to two different clocks")
    }
  }

  /**
    * Removes any attached tick source from the signal and its parents.
    * @param source the tick source to remove
    */
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