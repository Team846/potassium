package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock
import squants.time.{Milliseconds, Time}

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.ref.WeakReference

abstract class Stream[T] { self =>
  final type Value = T

  val expectedPeriodicity: ExpectedPeriodicity
  private[this] val listeners = mutable.Queue.empty[T => Unit]

  protected def publishValue(value: T): Unit = {
    listeners.foreach(_(value))
    // TODO: more stuff maybe
  }

  /**
    * Transforms a stream using a single value function from original stream
    * input to new stream emission
    * @param f the function to use to transform values
    * @tparam O the type of values in the new stream
    * @return a stream with values transformed by the given function
    */
  def map[O](f: T => O): Stream[O] = {
    val ret = new Stream[O] {
      override val expectedPeriodicity = self.expectedPeriodicity
    }

    val ptr = WeakReference(ret)

    var cancel: Cancel = null
    cancel = this.foreach { v =>
      ptr.get match {
        case Some(s) =>
          s.publishValue(f(v))
        case None =>
          cancel()
      }
    }

    ret
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
    * Merges two streams with the the resulting stream being updated whenever either
    * of its parents produce a new value. The resulting stream __does not__ have
    * a periodic nature.
    * @param other the stream to merge with
    * @tparam O the type of values from the other stream
    * @return a stream with the values from both streams brought together
    */
  def zipEager[O](other: Stream[O]): Stream[(T, O)] = {
    val ret = new EagerZippedStream[T, O]
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
    * Zips a stream with the time of value emission, for use in situations such as
    * synchronizing data sent over the network or calculating time deltas
    *
    * @return a stream with tuples of emitted values and the time of emission
    */
  def zipWithTime: Stream[(T, Time)] = {
    map(v => (v, Milliseconds(System.currentTimeMillis())))
  }

  /**
    * Applies a fixed size sliding window over the stream
    * @param size the size of the window
    * @return a stream returning complete windows
    */
  def sliding(size: Int): Stream[Queue[T]] = {
    var last = Queue.empty[T]

    val ret = new Stream[Queue[T]] {
      override val expectedPeriodicity = self.expectedPeriodicity
    }

    val ptr = WeakReference(ret)

    var cancel: Cancel = null
    cancel = this.foreach { v =>
      ptr.get match {
        case Some(s) =>
          if (last.size == size) {
            last = last.tail
          }

          last = last :+ v

          if (last.size == size) {
            s.publishValue(last)
          }
        case None =>
          cancel()
      }
    }

    ret
  }

  /**
    * Adds a listener for elements of this stream. Callbacks will be executed
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
    val stream = new Stream[T] {
      override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic
    }

    (stream, stream.publishValue)
  }
}
