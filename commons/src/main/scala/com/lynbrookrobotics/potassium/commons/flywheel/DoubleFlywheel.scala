package com.lynbrookrobotics.potassium.commons.flywheel

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue}
import squants.{Angle, Dimensionless}
import squants.motion.AngularVelocity

trait DoubleFlywheelProperties {
  def velocityGains: PIDFConfig[AngularVelocity,
                                GenericValue[AngularVelocity],
                                AngularVelocity,
                                GenericDerivative[AngularVelocity],
                                Angle,
                                Dimensionless]
}

trait DoubleFlywheelHardware {
  def leftVelocity: Signal[AngularVelocity]
  def rightVelocity: Signal[AngularVelocity]
}

abstract class DoubleFlywheel {
  type Properties <: DoubleFlywheelProperties
  type Hardware <: DoubleFlywheelHardware

  case class DoubleFlywheelSignal(left: Dimensionless, right: Dimensionless)

  object velocityControllers {
    def velocityControl(target: AngularVelocity)
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Signal[AngularVelocity], Signal[AngularVelocity], PeriodicSignal[DoubleFlywheelSignal]) = {
      val errorLeft = hardware.leftVelocity.map(target - _)
      val errorRight = hardware.rightVelocity.map(target - _)

      val controlLeft = PIDF.pidf(
        hardware.leftVelocity.toPeriodic,
        Signal.constant(target).toPeriodic,
        properties.map(_.velocityGains)
      )

      val controlRight = PIDF.pidf(
        hardware.rightVelocity.toPeriodic,
        Signal.constant(target).toPeriodic,
        properties.map(_.velocityGains)
      )

      (errorLeft, errorRight, controlLeft.zip(controlRight).map { t =>
        DoubleFlywheelSignal(t._1, t._2)
      })
    }
  }

  object velocityTasks {
    class WaitForVelocity(vel: AngularVelocity, tolerance: AngularVelocity)
                         (implicit properties: Signal[Properties], hardware: Hardware, component: Comp) extends FiniteTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) = velocityControllers.velocityControl(vel)
        component.setController(control.withCheck { _ =>
          if (errorLeft.get.abs < tolerance && errorRight.get.abs < tolerance) {
            finished()
          }
        })
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }

    class WhileAtVelocity(vel: AngularVelocity, tolerance: AngularVelocity)
                         (implicit properties: Signal[Properties], hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) = velocityControllers.velocityControl(vel)
        component.setController(control.withCheck { _ =>
          if (errorLeft.get.abs < tolerance && errorRight.get.abs < tolerance) {
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }

    class SpinAtVelocity(vel: AngularVelocity)
                        (implicit properties: Signal[Properties], hardware: Hardware, component: Comp) extends ContinuousTask {
      override def onStart(): Unit = {
        val (_, _, control) = velocityControllers.velocityControl(vel)
        component.setController(control)
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }
  }

  type Comp <: Component[DoubleFlywheelSignal]
}
