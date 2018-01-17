package com.lynbrookrobotics.potassium.commons.drivetrain.purePursuit

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.UnicycleCoreTasks
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.Point
import squants.Dimensionless
import squants.space.{Angle, Degrees, Feet, Length}


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

  class FollowWayPoints(wayPoints: Seq[Point],
                        tolerance: Length,
                        steadyOutput: Dimensionless,
                        maxTurnOutput: Dimensionless)
                       (drive: Drivetrain)
                       (implicit properties: Signal[controllers.DrivetrainProperties], hardware: controllers.DrivetrainHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val turnPosition = hardware.turnPosition.relativize(
        (initialTurnPosition, curr) => curr - initialTurnPosition)

      val position = XYPosition(
        turnPosition.map(compassToTrigonometric),
        hardware.forwardPosition
      )

      val (unicycle, error) = followWayPointsController(
        wayPoints,
        position,
        turnPosition,
        steadyOutput,
        maxTurnOutput)

      drive.setController(lowerLevelVelocityControl(unicycle.withCheckZipped(error) { e =>
        if (e.exists(_ < tolerance)) {
          println("finished")
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
                                    position: Stream[Point],
                                    turnPosition: Stream[Angle],
                                    steadyOutput: Dimensionless,
                                    maxTurnOutput: Dimensionless)
                                   (drive: Drivetrain)
                                   (implicit properties: Signal[DrivetrainProperties],
                                    hardware: DrivetrainHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val (unicycle, error) = followWayPointsController(
        wayPoints,
        position,
        turnPosition,
        steadyOutput,
        maxTurnOutput)

      drive.setController(lowerLevelOpenLoop(unicycle.withCheckZipped(error) {e =>
        if (e.exists(_ < tolerance)) {
          finished()
        }
      }))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }
}
