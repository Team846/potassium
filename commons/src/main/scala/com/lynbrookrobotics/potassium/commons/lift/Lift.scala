package com.lynbrookrobotics.potassium.commons.lift

import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF}
import com.lynbrookrobotics.potassium.tasks.WrapperTask
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue}
import squants.{Dimensionless, Percent}
import squants.motion.{Velocity}
import squants.space.{Feet, Length}


trait LiftProperties {
  def positionGains: PIDConfig[Length,
    Length,
    GenericValue[Length],
    Velocity,
    GenericIntegral[Length],
    Dimensionless]
}

trait LiftHardware {
  def position: Stream[Length]
}

abstract class Lift {
  type Properties <: LiftProperties
  type Hardware <: LiftHardware
  type Comp <: Component[Dimensionless]

  def positionControl(target: Stream[Length])
                     (implicit properties: Signal[Properties],
                      hardware: Hardware): (Stream[Length], Stream[Dimensionless]) = {
    val error = hardware.position.zipAsync(target).map(t => t._2 - t._1)
    (error, PIDF.pid(
      hardware.position,
      target,
      properties.map(_.positionGains)
    ))
  }

  object positionTasks {

    //move lift to target using PID control. When the arm is at target (within the tolerance), run inner (finite) task.
    class WhileAtPosition(target: Stream[Length], tolerance: Length)
                         (lift: Comp)
                         (implicit properties: Signal[Properties],
                          hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = positionControl(target)

        lift.setController(control.withCheckZipped(error) { error =>
          if (error.abs < tolerance) { //check whether lift is close enough to target. If so, run inner.
            readyToRunInner()
          }
        })
      }

      override def onEnd(): Unit = {
        lift.resetToDefault()
      }
    }

    //move arm above target using bangbang control. When it is above target, run inner (finite) task.
    class WhileAbovePosition(target: Stream[Length])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.position.zipAsync(target).map(t => t._2 - t._1), Percent(100))

        arm.setController(error.mapToConstant(control).withCheckZipped(error) { error: Length =>
          if (error <= Feet(0)) { //check whether arm is above target. If so, run inner.
            readyToRunInner()
          }
        })
      }
      override def onEnd(): Unit = arm.resetToDefault()
    }

    //move arm below target using bangbang control. When it is below target, run inner (finite) task.
    class WhileBelowPosition(target: Stream[Length])
                            (arm: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        val (error, control) = (hardware.position.zipAsync(target).map(t => t._2 - t._1), Percent(-100))
        arm.setController(hardware.position.mapToConstant(control).withCheckZipped(error) { error: Length =>
          if (error > Feet(0)) { //check whether arm is below target. If so, run inner.
            readyToRunInner()
          }
        })
      }
      override def onEnd(): Unit = arm.resetToDefault()
    }
  }
}
