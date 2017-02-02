package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
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
  def position: Signal[S]
}

trait Position[S <: Quantity[S],
               SWithD <: Quantity[SWithD] with TimeIntegral[D],
               SWithI <: Quantity[SWithI] with TimeDerivative[I],
               D <: Quantity[D] with TimeDerivative[SWithD],
               I <: Quantity[I] with TimeIntegral[SWithI],
               U <: Quantity[U]] {
  type Properties <: PositionProperties[S, SWithD, SWithI, D, I, U]
  type Hardware <: PositionHardware[S]

  implicit val exD: S => SWithD
  implicit val exI: S => SWithI

  def outputSignal(s: S)(implicit hardware: Hardware): Unit

  object positionControllers {
    def positionControl(target: S)(implicit properties: Signal[Properties], hardware: Hardware): (Signal[S], PeriodicSignal[U]) = {
      val error = hardware.position.map(target - _)
      (error, PIDF.pidf(hardware.position.toPeriodic, Signal.constant(target).toPeriodic, properties.map(_.positionGains)))
    }
  }

  object positionTasks {
    class MoveToPosition(pos: S, tolerance: S)(implicit properties: Signal[Properties], hardware: Hardware, comp: Comp) extends FiniteTask {
      override def onStart(): Unit = {
        val (error, control) = positionControllers.positionControl(pos)
        comp.setController(control.withCheck { _ =>
          if (error.get.abs < tolerance) {
            finished()
          }
        })
      }

      override def onEnd(): Unit = {
        comp.resetToDefault()
      }
    }

    class HoldPosition(pos: S)(implicit properties: Signal[Properties], hardware: Hardware, comp: Comp) extends ContinuousTask {
      override def onStart(): Unit = {
        val (_, control) = positionControllers.positionControl(pos)
        comp.setController(control)
      }

      override def onEnd(): Unit = {
        comp.resetToDefault()
      }
    }
  }

  type Comp <: Component[U]
}
