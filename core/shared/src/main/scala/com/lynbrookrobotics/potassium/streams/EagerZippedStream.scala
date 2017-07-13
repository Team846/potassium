package com.lynbrookrobotics.potassium.streams

class EagerZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic

  private[this] var lastASlot : Option[A] = None
  private[this] var lastBSlot : Option[B] = None

  def maybePublish(): Unit = {
    if (lastASlot.isDefined && lastBSlot.isDefined) {
      publishValue((lastASlot.get, lastBSlot.get))
    }
  }

  def receiveA(value: A): Unit = {
    lastASlot = Some(value)
    maybePublish()
  }

  def receiveB(value: B): Unit = {
    lastBSlot = Some(value)
    maybePublish()
  }
}
