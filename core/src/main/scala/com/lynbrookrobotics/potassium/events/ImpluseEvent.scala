package com.lynbrookrobotics.potassium.events

class ImpulseEventSource {
  val event = new ImpulseEvent

  def fire(): Unit = event.fire()
}

class ImpulseEvent private[events]() {
  private var listeners: List[() => Unit] = List.empty

  private[events] def fire(): Unit = {
    listeners.foreach(_.apply())
  }

  def foreach(listener: () => Unit): Unit = {
    listeners = listener :: listeners
  }
}
