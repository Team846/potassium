package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Quantity
import squants.time.{Time, TimeDerivative, TimeIntegral}

import scala.collection.immutable.Queue
import com.lynbrookrobotics.potassium.events.ContinuousEvent

import scala.annotation.unchecked.uncheckedVariance

abstract class Stream[+T] { self =>
  private[potassium] var lastPublishTime = 0L

  private[potassium] val parents: Seq[Stream[_]]

  private[potassium] val streamType: String

  def tryFix() = {
    println(s"Oh noes trying to fix an unfixable $streamType stream")
  }

  val expectedPeriodicity: ExpectedPeriodicity

  val originTimeStream: Option[Stream[Time]]

  private[this] var listeners = Vector.empty[T => Unit]

  private var hasShutdown = false
  def subscribeToParents(): Unit
  def unsubscribeFromParents(): Unit
  def checkRelaunch(): Unit = {}

  protected def publishValue(value: T @uncheckedVariance): Unit = {
    lastPublishTime = System.currentTimeMillis()
    listeners.foreach { l =>
      try {
        l.apply(value)
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
    // TODO: more stuff maybe
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
    self.synchronized {
      listeners = listeners :+ thunk
      if (listeners.size == 1) {
        // first listener, boot up!
        if (hasShutdown) {
          checkRelaunch()
        }

        subscribeToParents()
      }
    }

    new Cancel {
      private var hasCalledCancel = false
      override def cancel(): Unit = self.synchronized {
        if (hasCalledCancel) {
          throw new IllegalStateException("You have already cancelled this listener")
        }

        listeners = listeners.filterNot(_ eq thunk)

        if (listeners.isEmpty) {
          // we are shutting down!
          unsubscribeFromParents()
          hasShutdown = true
        }
      }
    }
  }

  /**
    * Creates a stream that automatically boots up and never unsubscribes from its parent
    * @return the new stream
    */
  def preserve: Stream[T] = {
    this.foreach(_ => {})
    this
  }

  /**
    * Transforms a stream using a single value function from original stream
    * input to new stream emission
    * @param f the function to use to transform values
    * @tparam O the type of values in the new stream
    * @return a stream with values transformed by the given function
    */
  def map[O](f: T => O): Stream[O] = {
    new MappedStream[T, O](this) {
      override def applyTransform(in: T): O = {
        f(in)
      }
    }
  }

  def mapToConstant[O](output: O): Stream[O] = {
    map(_ => output)
  }

  /**
    * Relativizes the stream according to the given function, which takes a base value
    * and the current value and returns the meaningful difference between them
    * @param f the function producing a difference between the base and latest value
    * @tparam O the type of values in the new stream
    * @return a new stream with values relativized against the next value from this stream
    */
  def relativize[O](f: (T, T) => O): Stream[O] = {
    new MappedStream[T, O](this) {
      var firstValue: Option[T] = None

      override def applyTransform(in: T): O = {
        if (firstValue.isEmpty) {
          firstValue = Some(in)
        }

        f(firstValue.get, in)
      }

      override def checkRelaunch(): Unit = {
        throw new IllegalStateException("Relativized streams cannot be relaunched")
      }
    }
  }

  /**
    *
    * @return returns a stream of first value this stream will publish
    *         from the time this method is called
    */
  def currentValue: Stream[T] = {
    relativize((firstValue, _) => firstValue)
  }

  /**
    * Merges two streams, with a value published from the resulting stream
    * whenever the parent streams have both published a new value
    * @param other the stream to merge with
    * @tparam O the type of values from the other stream
    * @return a stream with the values from both streams brought together
    */
  def zip[O](other: Stream[O]): Stream[(T, O)] = {
    new ZippedStream[T, O](this, other, skipTimestampCheck = false)
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
    (this.expectedPeriodicity, other.expectedPeriodicity) match {
      case (Periodic(a, s1), Periodic(b, s2)) if a == b && s1 == s2 =>
        zip(other)
      case _ =>
        new AsyncZippedStream[T, O](this, other)
    }
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
    new EagerZippedStream[T, O](this, other)
  }

  /**
    * Zips a stream with the time of value emission, for use in situations such as
    * synchronizing data sent over the network or calculating time deltas
    *
    * @return a stream with tuples of emitted values and the time of emission
    */
  def zipWithTime: Stream[(T, Time)] = {
    new ZippedStream[T, Time](this, originTimeStream.get, skipTimestampCheck = true)
  }

  /**
    * Applies a fixed size sliding window over the stream
    * @param size the size of the window
    * @return a stream returning complete windows
    */
  def sliding(size: Int): Stream[Queue[T]] = {
    new Stream[Queue[T]] {
      override private[potassium] val parents = Seq(self)
      override private[potassium] val streamType = "sliding"

      var unsubscribe: Cancel = null
      var last = Queue.empty[T]

      override def subscribeToParents(): Unit = {
        unsubscribe = self.foreach { v =>
          if (last.size == size) {
            last = last.tail
          }

          last = last :+ v

          if (last.size == size) {
            this.publishValue(last)
          }
        }
      }

      override def unsubscribeFromParents(): Unit = {
        unsubscribe.cancel(); unsubscribe = null
      }

      override def checkRelaunch(): Unit = {
        throw new IllegalStateException("Sliding streams cannot be relaunched")
      }

      override val expectedPeriodicity = self.expectedPeriodicity
      override val originTimeStream = self.originTimeStream.map { o =>
        o.sliding(size).map(_.last)
      }
    }
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
    new MappedStream[T, U](this) {
      var latest = initialValue

      override def applyTransform(in: T): U = {
        latest = f(latest, in)
        latest
      }

      override def checkRelaunch(): Unit = {
        throw new IllegalStateException("Scanleft streams cannot be relaunched")
      }
    }
  }

  /**
    * Zips the stream with the periods between when values were published
    * @return a stream of values and times in tuples
    */
  def zipWithDt: Stream[(T, Time)] = {
    zipWithTime.sliding(2).map { q =>
      (q.last._1, q.last._2 - q.head._2)
    }
  }

  /**
    * Calculates the derivative of the stream, producing units of
    * the derivative of the stream's units
    * @return a stream producing values that are the derivative of the stream
    */
  def derivative[D <: Quantity[D] with TimeDerivative[_]](implicit intEv: T => TimeIntegral[D]): Stream[D] = {
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
  def integral[I <: Quantity[I] with TimeIntegral[_]](implicit derivEv: T => TimeDerivative[I]): Stream[I] = {
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

  def simpsonsIntegral[I <: Quantity[I] with TimeIntegral[_]](implicit derivEv: T => TimeDerivative[I]): Stream[I] = {
    val previousValues = sliding(3)

    // scalastyle:off
    previousValues.zipWithDt.scanLeft(null.asInstanceOf[I]){case (acc, (current3Values, dt)) =>
      if (acc != null) {
        acc + (dt * current3Values(0) + 4 * dt * current3Values(1) + dt * current3Values(2)) / 6
      } else {
        (current3Values.last: TimeDerivative[I]) * dt
      }
    }
    //scalastyle:on
  }

  /**
    * Subtracts toSubtract from this
    * @param toSubtract how much to subtract
    * @return stream where every value is the minued minus toSubtract
    */
  def minus[Q <: Quantity[Q]](toSubtract: Stream[Q])(implicit intEv: T => Quantity[Q]): Stream[Q] = {
    zip(toSubtract).map{ case (minuend, subtractand) =>
      minuend - subtractand
    }
  }

  /**
    * Produces a stream that emits values at the same rate as the given
    * stream through polling
    * @param o the stream to use as a reference for emission triggers
    * @return a stream producing values in-sync with the given stream
    */
  def syncTo(o: Stream[_]): Stream[T] = {
    (expectedPeriodicity, o.expectedPeriodicity) match {
      case (Periodic(a, s1), Periodic(b, s2)) if a == b && s1 == s2 =>
        this

      case _ =>
        o.zipAsync(this).map(_._2)
    }
  }

  /**
    * Creates a stream that emits values at the given rate by polling
    * from the original stream
    * @param period the period to poll at
    * @param clock the clock to use to schedule periodic events
    * @return a stream emitting values from the original stream at the new rate
    */
  def pollPeriodic(period: Time)(implicit clock: Clock): Stream[T] = {
    syncTo(Stream.periodic(period)(()))
  }

  /**
    * Adds a "check" in the stream pipeline, which can be used for things
    * such as ending a task when a condition is met
    * @param check the function to run on each value of the stream
    * @return a stream that contains the check in the pipeline
    */
  def withCheck(check: T => Unit): Stream[T] = {
    map { v =>
      check(v)
      v
    }
  }

  def withCheckZipped[O](checkingStream: Stream[O])(check: O => Unit): Stream[T] = {
    zipAsync(checkingStream).withCheck { case (v, checkedValue) =>
      check(checkedValue)
    }.map(_._1)
  }

  /**
    * Filters a stream to only emit values that pass a certain condition
    * @param condition the condition to filter stream values with
    * @return a stream that only emits values that pass the condition
    */
  def filter(condition: T => Boolean): Stream[T] = {
    new Stream[T] {
      override private[potassium] val parents = Seq(self)
      override private[potassium] val streamType = "filter"

      var unsubscribe: Cancel = null

      override def subscribeToParents(): Unit = {
        unsubscribe = self.foreach { v =>
          if (condition(v)) {
            this.publishValue(v)
          }
        }
      }

      override def unsubscribeFromParents(): Unit = {
        unsubscribe.cancel(); unsubscribe = null
      }

      private val parent = self
      override val expectedPeriodicity = NonPeriodic
      // TODO: optimize
      override val originTimeStream = self.originTimeStream.map(
        _.zip(self).filter(t => condition(t._2)).map(_._1)
      )
    }
  }

  /**
    * Returns a continuous even that is true when condition about the value of
    * the stream is true
    * @param condition condition
    * @return
    */
  def eventWhen(condition: T => Boolean): ContinuousEvent = {
    new ContinuousEvent {
      val cancel = self.foreach(v => updateEventState(condition.apply(v)))
    }
  }
}

object Stream {
  def periodic[T](period: Time)(value: => T)(implicit clock: Clock): Stream[T] = {
    new Stream[T] { self =>
      override private[potassium] val parents = Seq.empty
      override private[potassium] val streamType = "periodic"

      override def subscribeToParents(): Unit = {}
      override def unsubscribeFromParents(): Unit = {}

      override val expectedPeriodicity = Periodic(period, this)

      override val originTimeStream = Some(new Stream[Time] {
        override private[potassium] val parents = Seq()
        override private[potassium] val streamType = "periodic"

        override def subscribeToParents(): Unit = {}
        override def unsubscribeFromParents(): Unit = {}

        override val expectedPeriodicity = self.expectedPeriodicity
        override val originTimeStream = None
      })

      override def publishValue(value: T): Unit = {
        originTimeStream.get.publishValue(clock.currentTime)
        super.publishValue(value)
      }

      clock(period) { _ =>
        publishValue(value)
      }
    }
  }

  def manual[T]: (Stream[T], T => Unit) = {
    manual(NonPeriodic)
  }

  def manual[T](periodicity: ExpectedPeriodicity): (Stream[T], T => Unit) = {
    val stream = new Stream[T] {
      override private[potassium] val parents = Seq.empty
      override private[potassium] val streamType = "manual"

      override def subscribeToParents(): Unit = {}
      override def unsubscribeFromParents(): Unit = {}

      override val expectedPeriodicity: ExpectedPeriodicity = periodicity

      override val originTimeStream = None

      override def publishValue(value: T): Unit = {
        super.publishValue(value)
      }
    }

    (stream, stream.publishValue)
  }

  def manualWithTime[T](implicit clock: Clock): (Stream[T], T => Unit) = {
    val stream = new Stream[T] {
      override private[potassium] val parents = Seq.empty
      override private[potassium] val streamType = "manual"

      override def subscribeToParents(): Unit = {}
      override def unsubscribeFromParents(): Unit = {}

      override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic

      override val originTimeStream = Some(new Stream[Time] {
        override private[potassium] val parents = Seq.empty
        override private[potassium] val streamType = "manual"

        override def subscribeToParents(): Unit = {}
        override def unsubscribeFromParents(): Unit = {}

        override val expectedPeriodicity: ExpectedPeriodicity = NonPeriodic
        override val originTimeStream = None
      })

      override def publishValue(value: T): Unit = {
        originTimeStream.get.publishValue(clock.currentTime)
        super.publishValue(value)
      }
    }

    (stream, stream.publishValue)
  }

  def manualWithTime[T](period: Time)(implicit clock: Clock): (Stream[T], T => Unit) = {
    val stream = new Stream[T] { self =>
      override private[potassium] val parents = Seq.empty
      override private[potassium] val streamType = "manual"

      override def subscribeToParents(): Unit = {}
      override def unsubscribeFromParents(): Unit = {}

      override val expectedPeriodicity: ExpectedPeriodicity = Periodic(period, this)

      override val originTimeStream = Some(new Stream[Time] {
        override private[potassium] val parents = Seq.empty
        override private[potassium] val streamType = "manual"

        override def subscribeToParents(): Unit = {}
        override def unsubscribeFromParents(): Unit = {}

        override val expectedPeriodicity: ExpectedPeriodicity = Periodic(period, self)
        override val originTimeStream = None
      })

      override def publishValue(value: T): Unit = {
        originTimeStream.get.publishValue(clock.currentTime)
        super.publishValue(value)
      }
    }

    (stream, stream.publishValue)
  }

  def traceBrokenStream(stream: Stream[_]): Unit = {
    def recurse(currentStream: Stream[_]): Option[Stream[_]] = {
      if (currentStream.lastPublishTime != 0) {
        None // we got data so this isn't the source of lost data
      } else if (currentStream.parents.nonEmpty) {
        Some(currentStream)
      } else {
        // if our parents also have lost data we pick the first parent, otherwise it's us
        currentStream.parents.flatMap(recurse).headOption.orElse(Some(currentStream))
      }
    }

    recurse(stream).map { str =>
      println(s"------ DETECTED LOST DATA -------")
      str.tryFix()
    }.getOrElse {
      println(s"------ COULD NOT TRACE LOST DATA TO A STREAM -------")
    }
  }
}
