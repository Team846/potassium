package com.lynbrookrobotics.potassium.commons.flywheel

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue}
import squants.{Angle, Dimensionless}
import squants.motion.AngularVelocity
import squants.time.Frequency

trait FlywheelProperties {
  def velocityGains: PIDFConfig[Frequency,
                                GenericValue[Frequency],
                                Frequency,
                                GenericDerivative[Frequency],
                                Dimensionless,
                                Dimensionless]
}

trait FlywheelHardware {
  def velocity: Signal[Frequency]
}

abstract class Flywheel {
  type Properties <: FlywheelProperties
  type Hardware <: FlywheelHardware

  object velocityControllers {
    def velocityControl(target: Signal[Frequency])
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Signal[Frequency], PeriodicSignal[Dimensionless]) = {
      val error = hardware.velocity.zip(target).map(t => t._2 - t._1)
      (error, PIDF.pidf(
        hardware.velocity.toPeriodic,
        target.toPeriodic,
        properties.map(_.velocityGains)
      ))
    }
  }

  object velocityTasks {
    class WaitForVelocity(vel: Signal[Frequency], tolerance: Frequency)
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

    class WhileAtVelocity(vel: Signal[Frequency], tolerance: Frequency)
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

    class SpinAtVelocity(vel: Signal[Frequency])
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
