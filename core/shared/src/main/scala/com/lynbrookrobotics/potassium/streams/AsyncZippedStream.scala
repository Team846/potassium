package com.lynbrookrobotics.potassium.streams

class AsyncZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = parentA.expectedPeriodicity

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
