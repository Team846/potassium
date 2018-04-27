package com.lynbrookrobotics.potassium

/**
 * A component that decides to update by checking for equality between old and new signals
 * @tparam T the type of values produced by signals for the component
 */
trait PureComponent[T] extends Component[T] {
  override def shouldComponentUpdate(previousSignal: T, newSignal: T): Boolean = {
    previousSignal != newSignal
  }
}
