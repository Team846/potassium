package com.lynbrookrobotics.potassium.streams

import squants.time.Time

class ZippedStream[A, B](parentA: Stream[A], parentB: Stream[B], skipTimestampCheck: Boolean) extends Stream[(A, B)] {
  override private[potassium] val parents = Seq(parentA, parentB)
  override private[potassium] val streamType = "zip"

  private[this] var parentAUnsubscribe: Cancel = null
  private[this] var parentBUnsubscribe: Cancel = null

  override def subscribeToParents(): Unit = {
    if (skipTimestampCheck || expectedPeriodicity == NonPeriodic
        || parentA.originTimeStream.isEmpty || parentB.originTimeStream.isEmpty) {
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

  override val expectedPeriodicity: ExpectedPeriodicity =
    (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
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

  private[this] var aSlot: Option[(A, Time)] = None
  private[this] var bSlot: Option[(B, Time)] = None
  private[this] var ignoreBadTimestamp = false

  override def tryFix(): Unit = {
    ignoreBadTimestamp = true
  }

  def attemptPublish(): Unit = {
    aSlot.zip(bSlot).foreach {
      case (a, b) =>
        if (ignoreBadTimestamp || a._2 == b._2 || expectedPeriodicity == NonPeriodic) {
          publishValue((a._1, b._1))
          aSlot = None
          bSlot = None
        }
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
