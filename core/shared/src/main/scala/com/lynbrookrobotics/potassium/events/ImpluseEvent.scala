package com.lynbrookrobotics.potassium.events

/**
  * A source of impulse events, which is used to trigger an event
  *
  * This is like [[scala.concurrent.Promise]] for [[scala.concurrent.Future]], where
  * the source can be triggered with `fire()` and the event can be distributed to
  * listeners.
  */
class ImpulseEventSource {
  /**
    * An event that is triggered when `fire()` is called
    */
  val event = new ImpulseEvent

  def fire(): Unit = event.fire()
}

/**
  * An impulse event, which is fired and immediately ends
  *
  * Unlike continuous events, impulse events do not have a running phase
  */
class ImpulseEvent private[events]() {
  private var listeners: List[() => Unit] = List.empty

  private[events] def fire(): Unit = {
    listeners.foreach(_.apply())
  }

  /**
    * Adds a listener to be called when the impulse event is fired
    * @param listener the listener to be fired
    */
  def foreach(listener: () => Unit): Unit = {
    listeners = listener :: listeners
  }
}
