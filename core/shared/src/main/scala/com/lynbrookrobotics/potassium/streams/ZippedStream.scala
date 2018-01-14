package com.lynbrookrobotics.potassium.streams

import squants.time.Time

class ZippedStream[A, B](parentA: Stream[A], parentB: Stream[B], skipTimestampCheck: Boolean) extends Stream[(A, B)] {
  var parentAUnsubscribe: Cancel = null
  var parentBUnsubscribe: Cancel = null

  override def subscribeToParents(): Unit = {
    if (skipTimestampCheck || expectedPeriodicity == NonPeriodic) {
      parentAUnsubscribe = parentA.foreach(t => this.receiveA(t, null))
      parentBUnsubscribe = parentB.foreach(t => this.receiveB(t, null))
    } else {
      parentAUnsubscribe = parentA.zipWithTime.foreach(t => this.receiveA(t._1, t._2))
      parentBUnsubscribe = parentB.zipWithTime.foreach(t => this.receiveB(t._1, t._2))
    }
  }

  override def unsubscribeFromParents(): Unit = {
    parentAUnsubscribe.cancel(); parentAUnsubscribe = null
    parentBUnsubscribe.cancel(); parentBUnsubscribe = null
  }

  override val expectedPeriodicity: ExpectedPeriodicity = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a eq b) {
        Periodic(a)
      } else {
        NonPeriodic
      }

    case _ => NonPeriodic
  }

  override val originTimeStream = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a eq b) {
        parentA.originTimeStream
      } else {
        None // TODO: alternatives?
      }

    case _ => None
  }

  private[this] var aSlot: Option[(A, Time)] = None
  private[this] var bSlot: Option[(B, Time)] = None

  def attemptPublish(): Unit = {
    if (aSlot.isDefined && bSlot.isDefined &&
      // if the streams are synced, make sure the timestamps match
      (aSlot.get._2 == bSlot.get._2 || expectedPeriodicity == NonPeriodic)) {
      publishValue((aSlot.get._1, bSlot.get._1))
      aSlot = None
      bSlot = None
    }
  }

  def receiveA(aValue: A, timestamp: Time): Unit = {
    aSlot = Some((aValue, timestamp))
    attemptPublish()
  }

  def receiveB(bValue: B, timestamp: Time): Unit = {
    bSlot = Some((bValue, timestamp))
    attemptPublish()
  }
}
