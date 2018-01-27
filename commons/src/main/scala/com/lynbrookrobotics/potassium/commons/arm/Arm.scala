package com.lynbrookrobotics.potassium.commons.arm

import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericValue}
import squants.{Angle, Dimensionless, Percent}
import squants.motion.AngularVelocity
import squants.Angle
import squants.space.{Angle, Degrees}


trait ArmProperties {
  def positionGains: PIDConfig[Angle,
    GenericValue[Angle],
    Angle,
    AngularVelocity,
    Dimensionless,
    Dimensionless]
}

trait ArmHardware {
  def angle: Stream[Angle]
}

abstract class Arm {
  type Properties <: ArmProperties
  type Hardware <: ArmHardware

  def positionControl(target: Stream[Angle])
                     (implicit properties: Signal[Properties],
                      hardware: Hardware): (Stream[Angle], Stream[Dimensionless]) = {
    val error = hardware.angle.zipAsync(target).map(t => t._2 - t._1)
    (error, PIDF.pid(
      hardware.angle,
      target,
      properties.map(_.positionGains)
    ).map(_ max Percent(0)))
  }


  // while at position
  // while above postion - 100 down until above position
  //while below position - 100 up until below position
  object positionTasks {

    class WhileAtPosition(angle: Stream[Angle], tolerance: Angle)
                         (arm: Comp)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = positionControl(angle)
        arm.setController(control.withCheckZipped(error) { error =>
          if (error.abs < tolerance) {
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        arm.resetToDefault()
      }
    }

    class WhileAbovePosition(angle: Stream[Angle])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.angle.zipAsync(angle).map(t => t._2 - t._1), 100)

        arm.setController(control { error: Angle =>
          if (error < Degrees(0)) { //if the arm is above the position then put full power down
            readyToRunInner()
          }
        })


      }

      override def onEnd(): Unit = ???
    }

    class WhileBelowPosition(angle: Stream[Angle])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.angle.zipAsync(angle).map(t => t._2 - t._1), 100)
        arm.setController(control { error: Angle =>
          if (error > Degrees(0)) { //if the arm is below the position then put full power down
            readyToRunInner()
          }
        })


      }

      override def onEnd(): Unit = ???
    }

  }



  type Comp <: Component[Dimensionless]
}
