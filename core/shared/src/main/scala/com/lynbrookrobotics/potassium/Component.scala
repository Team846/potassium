package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time
<<<<<<< HEAD
import com.lynbrookrobotics.potassium.streams.{Cancel, NonPeriodic, Stream}
=======
import com.lynbrookrobotics.potassium.streams.{NonPeriodic, Periodic, Stream}

import scala.collection.immutable.Queue
>>>>>>> detected overrun stuff

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
abstract class Component[T] (printsOnOverflow: Boolean = false) {
  def defaultController: Stream[T]
  private var currentControllerHandle: Option[Cancel] = None

  private var lastControlSignal: Option[T] = None

  def shouldComponentUpdate(previousSignal: T, newSignal: T): Boolean = true

  /**
    * Sets the controller to be used by the component during updates.
    * @param controller the new controller to use
    */
  def setController(controller: Stream[T]): Unit = {
    if (controller.expectedPeriodicity == NonPeriodic) {
      throw new IllegalArgumentException("Controller must be periodic")
    }
    controller.originTimeStream.get.zipWithDt.foreach{ case (_, dt: Time) =>
      if (controller.expectedPeriodicity.asInstanceOf[Periodic].period > 2 * dt ) {
        println("Loop took twice as long as expected, $dt")
        println("bad bad bad")
        println("overrun bad bad bad")
      }
    }

    currentControllerHandle.foreach(_.cancel())

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
    * @param signal the signal value to act on
    */
  def applySignal(signal: T): Unit
}
