package com.lynbrookrobotics.potassium.streams

class ZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
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

  override val expectedPeriodicity: ExpectedPeriodicity = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a, s1), Periodic(b, s2)) =>
      if (a == b && s1 == s2) {
        Periodic(a, s1)
      } else {
        NonPeriodic
      }

    case _ => NonPeriodic
  }

  override val originTimeStream = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a, s1), Periodic(b, s2)) =>
      if (a == b && s1 == s2) {
        parentA.originTimeStream
      } else {
        None // TODO: alternatives?
      }

    case _ => None
  }

  private[this] var aSlot: Option[A] = None
  private[this] var bSlot: Option[B] = None

  def attemptPublish(): Unit = {
    aSlot.zip(bSlot).foreach { case (a, b) =>
      publishValue((a, b))
      aSlot = None
      bSlot = None
    }
  }

  def receiveA(aValue: A): Unit = {
    aSlot = Some(aValue)
    attemptPublish()
  }

  def receiveB(bValue: B): Unit = {
    bSlot = Some(bValue)
    attemptPublish()
  }
}
