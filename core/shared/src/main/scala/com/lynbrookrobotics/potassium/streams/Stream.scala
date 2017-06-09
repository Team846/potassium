package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock
import squants.time.Time

import scala.collection.mutable
import scala.ref.WeakReference

abstract class Stream[T] {
  val expectedPeriodicity: ExpectedPeriodicity
  private[this] val listeners = mutable.Queue.empty[T => Unit]

  protected def publishValue(value: T): Unit = {
    listeners.foreach(_(value))
    // TODO: more stuff maybe
  }

  /**
    * Merges two streams, with a value published from the resulting stream
    * whenever the parent streams have both published a new value
    * @param other the stream to merge with
    * @tparam O the type of values from the other stream
    * @return a stream with the values from both streams brought together
    */
  def zip[O](other: Stream[O]): Stream[(T, O)] = {
    val ret = new ZippedStream[T, O](expectedPeriodicity, other.expectedPeriodicity)
    val ptr = WeakReference(ret)

    var aCancel: Cancel = null
    aCancel = this.foreach { a =>
      ptr.get match {
        case Some(s) =>
          s.receiveA(a)
        case None =>
          aCancel()
      }
    }

    var bCancel: Cancel = null

    bCancel = other.foreach { b =>
      ptr.get match {
        case Some(s) =>
          s.receiveB(b)
        case None =>
          bCancel()
      }
    }

    ret
  }

  /**
    * Merges two streams with the second stream included asynchronously, with
    * a value published from the resulting stream whenever this stream
    * publishes a value. The periodicity of the resulting stream matches
    * the periodicity of this stream.
    * @param other the stream to merge with
    * @tparam O the type of values from the other stream
    * @return a stream with the values from both streams brought together
    */
  def zipAsync[O](other: Stream[O]): Stream[(T, O)] = {
    val ret = new AsyncZippedStream[T, O](expectedPeriodicity)
    val ptr = WeakReference(ret)

    var aCancel: Cancel = null
    aCancel = this.foreach { a =>
      ptr.get match {
        case Some(s) =>
          s.receiveMaster(a)
        case None =>
          aCancel()
      }
    }

    var bCancel: Cancel = null

    bCancel = other.foreach { b =>
      ptr.get match {
        case Some(s) =>
          s.receiveFollower(b)
        case None =>
          bCancel()
      }
    }

    ret
  }

  /**
    * Adds a listener for elements of this Signal. Callbacks will be executed
    * whenever a new value is published in order of when the callbacks were added.
    * Callbacks added first will be called first and callbacks added last will
    * be called last.
    *
    * @param thunk the listener to be called on each published value
    * @return a function to call to remove the listener
    */
  def foreach(thunk: T => Unit): Cancel = {
    listeners.enqueue(thunk)

    () => {
      listeners.dequeueFirst(_ eq thunk)
    }
  }
}

object Stream {
  def periodic[T](period: Time)(value: => T)(implicit clock: Clock): Stream[T] = {
    new Stream[T] {
      val expectedPeriodicity = Periodic(period)

      clock(period) { _ =>
        publishValue(value)
      }
    }
  }

  def manual[T]: (Stream[T], T => Unit) = {
    var publish: T => Unit = null

    val signal = new Stream[T] {
      override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic

      publish = publishValue
    }

    (signal, publish)
  }
}
