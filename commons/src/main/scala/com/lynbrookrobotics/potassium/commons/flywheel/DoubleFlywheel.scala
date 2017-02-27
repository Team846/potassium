package com.lynbrookrobotics.potassium.commons.flywheel

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF, PIDFConfig}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue, Ratio}
import squants.{Dimensionless, Percent}
import squants.time.Frequency

trait DoubleFlywheelProperties {
  def maxVelocityLeft: Frequency
  def maxVelocityRight: Frequency

  def velocityGainsLeft: PIDConfig[Frequency,
                                   GenericValue[Frequency],
                                   Frequency,
                                   GenericDerivative[Frequency],
                                   Dimensionless,
                                   Dimensionless]

  def velocityGainsRight: PIDConfig[Frequency,
                                    GenericValue[Frequency],
                                    Frequency,
                                    GenericDerivative[Frequency],
                                    Dimensionless,
                                    Dimensionless]

  lazy val velocityGainsLeftFull =
    velocityGainsLeft.withF(Ratio(Percent(100), maxVelocityLeft))

  lazy val velocityGainsRightFull =
    velocityGainsRight.withF(Ratio(Percent(100), maxVelocityRight))
}

trait DoubleFlywheelHardware {
  def leftVelocity: Signal[Frequency]
  def rightVelocity: Signal[Frequency]
}

abstract class DoubleFlywheel {
  type Properties <: DoubleFlywheelProperties
  type Hardware <: DoubleFlywheelHardware

  case class DoubleFlywheelSignal(left: Dimensionless, right: Dimensionless)

  object velocityControllers {
    def velocityControl(leftTarget: Signal[Frequency],
                        rightTarget: Signal[Frequency])
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Signal[Frequency], Signal[Frequency],
                                              PeriodicSignal[DoubleFlywheelSignal]) = {
      val errorLeft = hardware.leftVelocity.zip(leftTarget).map(t => t._2 - t._1)
      val errorRight = hardware.rightVelocity.zip(rightTarget).map(t => t._2 - t._1)

      val controlLeft = PIDF.pidf(
        hardware.leftVelocity.toPeriodic,
        leftTarget.toPeriodic,
        properties.map(_.velocityGainsLeftFull)
      ).map(_ max Percent(0))

      val controlRight = PIDF.pidf(
        hardware.rightVelocity.toPeriodic,
        rightTarget.toPeriodic,
        properties.map(_.velocityGainsRightFull)
      ).map(_ max Percent(0))

      (errorLeft, errorRight, controlLeft.zip(controlRight).map { t =>
        DoubleFlywheelSignal(t._1, t._2)
      })
    }
  }

  object velocityTasks {
    class WhileAtVelocity(vel: Signal[Frequency], tolerance: Frequency)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) =
          velocityControllers.velocityControl(vel, vel)
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

    class WhileAtDoubleVelocity(leftVel: Signal[Frequency],
                          rightVel: Signal[Frequency],
                          tolerance: Frequency)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) =
          velocityControllers.velocityControl(leftVel, rightVel)
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
  }

  type Comp <: Component[DoubleFlywheelSignal]
}
