package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.logging.{AsyncLogger, Histogram}
import com.lynbrookrobotics.potassium.streams.{Cancel, NonPeriodic, Periodic, Stream}
import squants.time.{Milliseconds, Time}


/**
  * Represents a single robotic component, which translates signal data into action
  *
  * Components can be though of as a function of command => Unit, which
  * is implemented in applySignal. The function is expected to take the latest
  * command and send it to hardware interfaces. In addition, the applySignal method
  * is the place to implement safeties, as it is the last layer of signal transformation.
  *
  * @tparam T the type of values produced by signals for the component
  */
abstract class Component[T] {

  def defaultController: Stream[T]
  private var currentControllerHandle: Option[Cancel] = None

  private var currentTimingHandle: Option[Cancel] = None

  private var lastControlSignal: Option[T] = None

  def shouldComponentUpdate(previousSignal: T, newSignal: T): Boolean = true

  val printOnOverflowAsyncLogger: Option[AsyncLogger] = None

  def logStuff(controller: Stream[T]): Unit = {
    printOnOverflowAsyncLogger.foreach { logger =>
      val streamPeriodicity = controller.expectedPeriodicity.asInstanceOf[Periodic].period
      val histogram = new Histogram(
        min = 0.8 * streamPeriodicity,
        max = 1.2 * streamPeriodicity,
        binCount = 10,
        asyncLogger = logger)

      currentTimingHandle.foreach(_.cancel())
      currentTimingHandle = Some(
        controller.zipWithDt.foreach { case (_, dt) =>
          histogram.apply(dt)
          if (controller.expectedPeriodicity.asInstanceOf[Periodic].period > 2 * dt ) {
            histogram.printStatus()
          }
        }
      )
    }
  }

  /**
    * Sets the controller to be used by the component during updates.
    * @param controller the new controller to use
    */
  def setController(controller: Stream[T]): Unit = {
    if (controller.expectedPeriodicity == NonPeriodic) {
      throw new IllegalArgumentException("Controller must be periodic")
    }

    logStuff(controller)

    currentControllerHandle.foreach(_.cancel())
    currentTimingHandle.foreach(_.cancel())


    currentControllerHandle = Some(controller.foreach { value =>
      val shouldUpdate = lastControlSignal.isEmpty ||
        shouldComponentUpdate(lastControlSignal.get, value)

      if (shouldUpdate) {
        applySignal(value)
      }

      lastControlSignal = Some(value)
    })
  }


  /**
    * Resets the component to use its default controller.
    */
  def resetToDefault(): Unit = {
    setController(defaultController)
  }

  /**
    * Applies the latest control signal value.
    * @param signal the signal values to act on
    */
  def applySignal(signal: T): Unit

}
