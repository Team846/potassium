package com.lynbrookrobotics.potassium

import squants.Time

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
    applySignal(currentController.currentValue(dt))
  }
}
