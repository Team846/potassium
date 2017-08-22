package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock

class AsyncZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = parentA.expectedPeriodicity

  // TODO: Review please
  override val originClock: Clock = parentA.originClock

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
