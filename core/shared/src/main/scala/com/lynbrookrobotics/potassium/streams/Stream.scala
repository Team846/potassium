package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Quantity
import squants.time.{Milliseconds, Time, TimeDerivative, TimeIntegral}

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
  def zipWithTime(implicit clock: Clock): Stream[(T, Time)] = {
    map(v => (v, clock.currentTime))
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
    * Applies an accumulator to the stream, combining the accumulated value
    * with each emitted value of this stream to produce a new value
    * @param initialValue the initial accumulated value
    * @param f the accumulator to combine the value built and the latest emitted value
    * @tparam U the type of the accumulated value
    * @return a stream accumulating according to the given function
    */
  def scanLeft[U](initialValue: U)(f: (U, T) => U): Stream[U] = {
    var latest = initialValue

    map { v =>
      latest = f(latest, v)
      latest
    }
  }

  /**
    * Zips the stream with the periods between when values were published
    * @return a stream of values and times in tuples
    */
  def zipWithDt(implicit clock: Clock): Stream[(T, Time)] = {
    zipWithTime.sliding(2).map { q =>
      (q.last._1, q.last._2 - q.head._2)
    }
  }

  /**
    * Calculates the derivative of the stream, producing units of
    * the derivative of the stream's units
    * @return a stream producing values that are the derivative of the stream
    */
  def derivative[D <: Quantity[D] with TimeDerivative[_]](implicit intEv: T => TimeIntegral[D], clock: Clock): Stream[D] = {
    zipWithTime.sliding(2).map { q =>
      val dt = q.last._2 - q.head._2
      (q.last._1 / dt) - (q.head._1 / dt)
    }
  }

  /**
    * Calculates the integral of the signal, producing units of
    * the integral of the signal's units
    * @return a signal producing values that are the integral of the signal
    */
  def integral[I <: Quantity[I] with TimeIntegral[_]](implicit derivEv: T => TimeDerivative[I], clock: Clock): Stream[I] = {
    // We use null as a cheap way to handle the initial value since there is
    // no way to get a "zero" for I

    // scalastyle:off
    zipWithDt.scanLeft(null.asInstanceOf[I]) { case (acc, (cur, dt)) =>
      if (acc != null) {
        acc + (cur: TimeDerivative[I]) * dt
      } else {
        (cur: TimeDerivative[I]) * dt
      }
    }
    // scalastyle:on
  }

  /**
    * Produces a stream that emits values at the same rate as the given
    * stream through polling
    * @param o the stream to use as a reference for emission triggers
    * @return a stream producing values in-sync with the given stream
    */
  def syncTo(o: Stream[_]): Stream[T] = {
    (expectedPeriodicity, o.expectedPeriodicity) match {
      case (Periodic(a), Periodic(b)) if a eq b =>
        this

      case _ =>
        o.zipAsync(this).map(_._2)
    }
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
