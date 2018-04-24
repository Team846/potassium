package com.lynbrookrobotics.potassium.commons.lift

import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDF}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.WrapperTask
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue}
import com.lynbrookrobotics.potassium.{Component, Signal}
import squants.motion.Velocity
import squants.space.Length
import squants.{Dimensionless, Percent}


trait LiftProperties {
  def positionGains: PIDConfig[Length,
    Length,
    GenericValue[Length],
    Velocity,
    GenericIntegral[Length],
    Dimensionless]
}

trait LiftHardware {
  val position: Stream[Length]
}

abstract class Lift {
  type LiftSignal
  type Properties <: LiftProperties
  type Hardware <: LiftHardware
  type Comp <: Component[LiftSignal]

  def positionControl(target: Stream[Length])
                     (implicit properties: Signal[Properties],
                      hardware: Hardware): (Stream[Length], Stream[LiftSignal]) = (
    hardware
      .position
      .zipAsync(target)
      .map(t => t._2 - t._1),

    PIDF.pid(
      hardware.position,
      target,
      properties.map(_.positionGains)
    ).map(openLoopToLiftSignal)
  )

  def stayAbove(target: Stream[Length])
               (implicit properties: Signal[Properties],
                hardware: Hardware): Stream[LiftSignal] = hardware
    .position
    .zipAsync(target)
    .map { case (p, t) => if (p < t) Percent(100) else Percent(0) }
    .map(openLoopToLiftSignal)

  def stayBelow(target: Stream[Length])
               (implicit properties: Signal[Properties],
                hardware: Hardware): Stream[LiftSignal] = hardware
    .position
    .zipAsync(target)
    .map { case (p, t) => if (p > t) Percent(-100) else Percent(0) }
    .map(openLoopToLiftSignal)

  def openLoopToLiftSignal(x: Dimensionless): LiftSignal

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

      override val dependencies: Set[Component[_]] = Set(lift)
    }

    //move arm above target using bangbang control. When it is above target, run inner (finite) task.
    class WhileAbovePosition(target: Stream[Length])
                            (lift: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        lift.setController(
          stayAbove(target)
            .withCheckZipped(hardware.position.zip(target)) {
              case (p, t) => if (p > t) readyToRunInner()
            }
        )
      }

      override def onEnd(): Unit = lift.resetToDefault()

      override val dependencies: Set[Component[_]] = Set(lift)
    }

    //move arm below target using bangbang control. When it is below target, run inner (finite) task.
    class WhileBelowPosition(target: Stream[Length])
                            (lift: Comp)
                            (implicit properties: Signal[Properties],
                             hardware: Hardware) extends WrapperTask {
      override def onStart(): Unit = {
        lift.setController(
          stayBelow(target)
            .withCheckZipped(hardware.position.zip(target)) {
              case (p, t) => if (p < t) readyToRunInner()
            }
        )
      }

      override def onEnd(): Unit = lift.resetToDefault()

      override val dependencies: Set[Component[_]] = Set(lift)
    }

  }

}
