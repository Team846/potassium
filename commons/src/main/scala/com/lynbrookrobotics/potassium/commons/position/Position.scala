package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask}
import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}

trait PositionProperties[S <: Quantity[S],
                         SWithD <: Quantity[SWithD] with TimeIntegral[D],
                         SWithI <: Quantity[SWithI] with TimeDerivative[I],
                         D <: Quantity[D] with TimeDerivative[SWithD],
                         I <: Quantity[I] with TimeIntegral[SWithI],
                         U <: Quantity[U]] {
  def positionGains: PIDFConfig[S, SWithD, SWithI, D, I, U]
}

trait PositionHardware[S <: Quantity[S]] {
  def position: Stream[S]
}

abstract class Position[S <: Quantity[S],
               SWithD <: Quantity[SWithD] with TimeIntegral[D],
               SWithI <: Quantity[SWithI] with TimeDerivative[I],
               D <: Quantity[D] with TimeDerivative[SWithD],
               I <: Quantity[I] with TimeIntegral[SWithI],
               U <: Quantity[U]](implicit exD: S => SWithD, exI: S => SWithI) {
  type Properties <: PositionProperties[S, SWithD, SWithI, D, I, U]
  type Hardware <: PositionHardware[S]

  object positionControllers {
    def positionControl(target: S)
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Stream[S], Stream[U]) = {
      val error = hardware.position.map(target - _)
      (
        error,
        PIDF.pidf(
          hardware.position,
          hardware.position.mapToConstant(target),
          properties.map(_.positionGains)))
    }
  }

  object positionTasks {
    class MoveToPosition(pos: S,
                         tolerance: S)
                        (implicit properties: Signal[Properties],
                         hardware: Hardware, comp: Comp) extends FiniteTask {
      override def onStart(): Unit = {
        val (error, control) = positionControllers.positionControl(pos)
        comp.setController(control.withCheckZipped(error) { error =>
          if (error.abs < tolerance) {
            finished()
          }
        })
      }

      override def onEnd(): Unit = {
        comp.resetToDefault()
      }

      override val dependencies: Set[Component[_]] = Set(comp)
    }

    class HoldPosition(pos: S)(implicit properties: Signal[Properties], hardware: Hardware, comp: Comp) extends ContinuousTask {
      override def onStart(): Unit = {
        val (_, control) = positionControllers.positionControl(pos)
        comp.setController(control)
      }

      override def onEnd(): Unit = {
        comp.resetToDefault()
      }

      override val dependencies: Set[Component[_]] = Set(comp)
    }
  }

  type Comp <: Component[U]
}
