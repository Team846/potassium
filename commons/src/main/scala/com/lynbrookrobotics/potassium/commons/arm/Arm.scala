package com.lynbrookrobotics.potassium.commons.arm

import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF}
import com.lynbrookrobotics.potassium.tasks.WrapperTask
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue}
import squants.{Dimensionless, Percent}
import squants.motion.AngularVelocity
import squants.space.{Angle, Degrees}


trait ArmProperties {
  def positionGains: PIDConfig[Angle,
    Angle,
    GenericValue[Angle],
    AngularVelocity,
    GenericIntegral[Angle],
    Dimensionless]
}

trait ArmHardware {
  def angle: Stream[Angle]
}

abstract class Arm {
  type Properties <: ArmProperties
  type Hardware <: ArmHardware
  type Comp <: Component[Dimensionless]

  def positionControl(target: Stream[Angle])
                     (implicit properties: Signal[Properties],
                      hardware: Hardware): (Stream[Angle], Stream[Dimensionless]) = {
    val error = hardware.angle.zipAsync(target).map(t => t._2 - t._1)
    (error, PIDF.pid(
      hardware.angle,
      target,
      properties.map(_.positionGains)
    ))
  }

  object positionTasks {

    //move arm to target using PID control. When the arm is at target (within the tolerance), run inner (finite) task.
    class WhileAtPosition(target: Stream[Angle], tolerance: Angle)
                         (arm: Comp)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = positionControl(target)

        arm.setController(control.withCheckZipped(error) { error =>
          if (error.abs < tolerance) { //check whether arm is close enough to target. If so, run inner.
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        arm.resetToDefault()
      }
    }

    //move arm above target using bangbang control. When it is above target, run inner (finite) task.
    class WhileAbovePosition(target: Stream[Angle])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.angle.zipAsync(target).map(t => t._2 - t._1), Percent(100))

        arm.setController(error.mapToConstant(control).withCheckZipped(error) { error: Angle =>
          if (error <= Degrees(0)) { //check whether arm is above target. If so, run inner.
            readyToRunInner()
          }
        })
      }
      override def onEnd(): Unit = arm.resetToDefault()
    }

    //move arm below target using bangbang control. When it is below target, run inner (finite) task.
    class WhileBelowPosition(target: Stream[Angle])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.angle.zipAsync(target).map(t => t._2 - t._1), Percent(-100))
        arm.setController(hardware.angle.mapToConstant(control).withCheckZipped(error) { error: Angle =>
          if (error > Degrees(0)) { //check whether arm is below target. If so, run inner.
            readyToRunInner()
          }
        })
      }
      override def onEnd(): Unit = arm.resetToDefault()
    }
  }
}
