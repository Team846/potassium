package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock

class AsyncZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  private[this] var parentAUnsubscribe: Cancel = null
  private[this] var parentBUnsubscribe: Cancel = null

  override def subscribeToParents(): Unit = {
    parentAUnsubscribe = parentA.foreach(this.receivePrimary)
    parentBUnsubscribe = parentB.foreach(this.receiveSecondary)
  }

  override def unsubscribeFromParents(): Unit = {
    parentAUnsubscribe.cancel(); parentAUnsubscribe = null
    parentBUnsubscribe.cancel(); parentBUnsubscribe = null
  }

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
