package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.clock.Clock
import squants.Time

/**
  * Represents a single robotic component, which translates signal data into action
  *
  * Components can be though of as a function of command => Unit, which
  * is implemented in applySignal. The function is expected to take the latest
  * command and send it to hardware interfaces. In addition, the applySignal method
  * is the place to implement safeties, as it is the last layer of signal transformation.
  *
  * @param period the update rate of the component
  * @param clock the clock to use to schedule periodic updates
  * @tparam T the type of values produced by signals for the component
  */
abstract class Component[T](period: Time)(implicit val clock: Clock) {
  def defaultController: PeriodicSignal[T]
  private var currentController: PeriodicSignal[T] = defaultController

  def setController(controller: PeriodicSignal[T]): Unit = {
    currentController.detachTickSource(this)
    controller.attachTickSource(this)
    currentController = controller
  }

  def resetToDefault(): Unit = {
    setController(defaultController)
  }

  def applySignal(signal: T): Unit

  clock(period) { dt =>
    // Because of unusual initialization orders, currentController may sometimes be null,
    // so we make sure to have it initialized here.
    if (currentController == null) {
      currentController = defaultController
    }

    applySignal(currentController.currentValue(dt))
  }
}
