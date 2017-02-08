package com.lynbrookrobotics.potassium.commons.flywheel

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue}
import squants.{Angle, Dimensionless}
import squants.motion.AngularVelocity

trait FlywheelProperties {
  def velocityGains: PIDFConfig[AngularVelocity,
                                GenericValue[AngularVelocity],
                                AngularVelocity,
                                GenericDerivative[AngularVelocity],
                                Angle,
                                Dimensionless]
}

trait FlywheelHardware {
  def velocity: Signal[AngularVelocity]
}

abstract class Flywheel {
  type Properties <: FlywheelProperties
  type Hardware <: FlywheelHardware

  object velocityControllers {
    def velocityControl(target: AngularVelocity)
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Signal[AngularVelocity], PeriodicSignal[Dimensionless]) = {
      val error = hardware.velocity.map(target - _)
      (error, PIDF.pidf(
        hardware.velocity.toPeriodic,
        Signal.constant(target).toPeriodic,
        properties.map(_.velocityGains)
      ))
    }
  }

  object velocityTasks {
    class WaitForVelocity(vel: AngularVelocity, tolerance: AngularVelocity)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends FiniteTask {
      override def onStart(): Unit = {
        val (error, control) = velocityControllers.velocityControl(vel)
        component.setController(control.withCheck { _ =>
          if (error.get.abs < tolerance) {
            finished()
          }
        })
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }

    class WhileAtVelocity(vel: AngularVelocity, tolerance: AngularVelocity)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = velocityControllers.velocityControl(vel)
        component.setController(control.withCheck { _ =>
          if (error.get.abs < tolerance) {
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }

    class SpinAtVelocity(vel: AngularVelocity)
                        (implicit properties: Signal[Properties],
                         hardware: Hardware, component: Comp) extends ContinuousTask {
      override def onStart(): Unit = {
        val (_, control) = velocityControllers.velocityControl(vel)
        component.setController(control)
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }
  }

  type Comp <: Component[Dimensionless]
}
