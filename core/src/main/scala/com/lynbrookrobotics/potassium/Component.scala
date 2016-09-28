package com.lynbrookrobotics.potassium

import squants.Time

abstract class Component[T](period: Time)(implicit val clock: Clock) {
  def defaultController: Controller[T]
  private var currentController: Controller[T] = defaultController

  def setController(controller: Controller[T]): Unit = {
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
