package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.{Angle, Degrees, Feet, Length}
import squants.{Dimensionless, Each}

trait PurePursuitTasks extends UnicycleCoreTasks {
  import controllers._

  val origin = new Point(
    Feet(0),
    Feet(0)
  )

  def compassToTrigonometric(angle: Angle): Angle = {
    Degrees(90) - angle
  }

  def clamp(toClamp: Dimensionless, maxMagnitude: Dimensionless): Dimensionless = {
    toClamp min maxMagnitude max -maxMagnitude
  }

  class FollowWayPoints(wayPoints: Seq[Point], tolerance: Length)
                       (implicit drive: Drivetrain,
                        properties: Signal[UnicycleProperties],
                        hardware: UnicycleHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val initialTurnPosition = hardware.turnPosition.get

      val turnPosition = hardware.turnPosition.map(_ - initialTurnPosition)

      val position = XYPosition(
        turnPosition.map(compassToTrigonometric),
        hardware.forwardPosition
      )

      val (unicycle, error) = followWayPointsController(wayPoints, position, turnPosition)

      drive.setController(lowerLevelOpenLoop(unicycle.withCheck { _ =>
        if (error.get.exists(_ < tolerance)) {
          finished()
        }
      }))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class FollowWayPointsWithPosition(wayPoints: Seq[Point],
                                    tolerance: Length,
                                    position: PeriodicSignal[Point],
                                    turnPosition: Signal[Angle])
                                   (implicit drive: Drivetrain,
                                    properties: Signal[UnicycleProperties],
                                    hardware: UnicycleHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val (unicycle, error) = followWayPointsController(wayPoints, position, turnPosition)

      drive.setController(lowerLevelOpenLoop(unicycle.withCheck { _ =>
        if (error.get.exists(_ < tolerance)) {
          println("finished")
          finished()
        }
      }))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }
}
