package com.lynbrookrobotics.potassium.streams

class EagerZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  private[this] var parentAUnsubscribe: Cancel = null
  private[this] var parentBUnsubscribe: Cancel = null

  override def subscribeToParents(): Unit = {
    parentAUnsubscribe = parentA.foreach(this.receiveA)
    parentBUnsubscribe = parentB.foreach(this.receiveB)
  }

  override def unsubscribeFromParents(): Unit = {
    parentAUnsubscribe.cancel(); parentAUnsubscribe = null
    parentBUnsubscribe.cancel(); parentBUnsubscribe = null
  }

  override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic

  override val originTimeStream = null

  private[this] var lastASlot : Option[A] = None
  private[this] var lastBSlot : Option[B] = None

  def attemptPublish(): Unit = {
    if (lastASlot.isDefined && lastBSlot.isDefined) {
      publishValue((lastASlot.get, lastBSlot.get))
    }
  }

  def receiveA(value: A): Unit = {
    lastASlot = Some(value)
    attemptPublish()
  }

  def receiveB(value: B): Unit = {
    lastBSlot = Some(value)
    attemptPublish()
  }
}
