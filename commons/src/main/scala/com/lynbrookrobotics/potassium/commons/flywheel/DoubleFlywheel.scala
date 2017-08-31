package com.lynbrookrobotics.potassium.commons.flywheel

import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
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
  def leftVelocity: Stream[Frequency]
  def rightVelocity: Stream[Frequency]
}

abstract class DoubleFlywheel {
  type Properties <: DoubleFlywheelProperties
  type Hardware <: DoubleFlywheelHardware

  case class DoubleFlywheelSignal(left: Dimensionless, right: Dimensionless)

  object velocityControllers {
    def velocityControl(leftTarget: Stream[Frequency],
                        rightTarget: Stream[Frequency])
                       (implicit properties: Signal[Properties],
                        hardware: Hardware): (Stream[Frequency], Stream[Frequency],
                                              Stream[DoubleFlywheelSignal]) = {
      val errorLeft = hardware.leftVelocity.zip(leftTarget).map(t => t._2 - t._1)
      val errorRight = hardware.rightVelocity.zip(rightTarget).map(t => t._2 - t._1)

      val controlLeft = PIDF.pidf(
        hardware.leftVelocity,
        leftTarget,
        properties.map(_.velocityGainsLeftFull)
      ).map(_ max Percent(0))

      val controlRight = PIDF.pidf(
        hardware.rightVelocity,
        rightTarget,
        properties.map(_.velocityGainsRightFull)
      ).map(_ max Percent(0))

      (errorLeft, errorRight, controlLeft.zip(controlRight).map { t =>
        DoubleFlywheelSignal(t._1, t._2)
      })
    }
  }

  object velocityTasks {
    class WhileAtVelocity(vel: Stream[Frequency], tolerance: Frequency)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) =
          velocityControllers.velocityControl(vel, vel)

        val zippedError = errorLeft.zip(errorRight)
        component.setController(control.withCheckZipped(zippedError){ case (eLeft, eRight) =>
          if (eLeft.abs < tolerance && eRight.abs < tolerance) {
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        component.resetToDefault()
      }
    }

    class WhileAtDoubleVelocity(leftVel: Stream[Frequency],
                          rightVel: Stream[Frequency],
                          tolerance: Frequency)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware, component: Comp) extends WrapperTask {
      override def onStart(): Unit = {
        val (errorLeft, errorRight, control) =
          velocityControllers.velocityControl(leftVel, rightVel)
        component.setController(control.withCheckZipped(errorLeft.zip(errorRight)){ case (eLeft, eRight) =>
          if (eLeft.abs < tolerance && eRight.abs < tolerance) {
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
