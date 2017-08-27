package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock

class AsyncZippedStream[A, B](private val parentA: Stream[A], private val parentB: Stream[B]) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = parentA.expectedPeriodicity

  override val originTimeStream = parentA.originTimeStream

  private[this] var secondarySlot: Option[B] = None

  def receivePrimary(primaryValue: A): Unit = {
    if (secondarySlot.isDefined) {
      publishValue((primaryValue, secondarySlot.get))
    }
  }

  def receiveSecondary(secondaryValue: B): Unit = {
    secondarySlot = Some(secondaryValue)
  }
}
