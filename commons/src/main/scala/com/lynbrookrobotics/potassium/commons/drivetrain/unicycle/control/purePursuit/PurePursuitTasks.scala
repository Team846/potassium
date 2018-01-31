package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.UnicycleCoreTasks
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.{FiniteTask, FiniteTaskFinishedListener}
import com.lynbrookrobotics.potassium.units.Point
import squants.Dimensionless
import squants.space.{Angle, Degrees, Feet, Length}


trait PurePursuitTasks extends UnicycleCoreTasks {
  import controllers._

  def compassToTrigonometric(angle: Angle): Angle = {
    Degrees(90) - angle
  }

  class FollowWayPoints(wayPoints: Seq[Point],
                        tolerance: Length,
                        maxTurnOutput: Dimensionless,
                        targetTicksWithingTolerance: Int = 1,
                        backwards: Boolean = false)
                       (drive: Drivetrain)
                       (implicit properties: Signal[controllers.DrivetrainProperties],
                        hardware: controllers.DrivetrainHardware) extends FiniteTask with FiniteTaskFinishedListener {
    private var absoluteFollow: FollowWayPointsWithPosition = null

    override def onFinished(task: FiniteTask): Unit = {
      finished()
    }

    override def onStart(): Unit = {
      val turnPosition = hardware.turnPosition.relativize(
        (initialTurnPosition, curr) => curr - initialTurnPosition)

      val position = XYPosition(
        turnPosition.map(compassToTrigonometric),
        hardware.forwardPosition
      )

      absoluteFollow = new FollowWayPointsWithPosition(
        wayPoints,
        tolerance,
        position,
        turnPosition,
        maxTurnOutput,
        targetTicksWithingTolerance,
        backwards
      )(drive)

      absoluteFollow.setFinishedListener(this)
      absoluteFollow.init()
    }

    override def onEnd(): Unit = {
      if (absoluteFollow.isRunning) {
        absoluteFollow.abort()
      }

      absoluteFollow = null
    }
  }

  class FollowWayPointsWithPosition(wayPoints: Seq[Point],
                                    tolerance: Length,
                                    position: Stream[Point],
                                    turnPosition: Stream[Angle],
                                    maxTurnOutput: Dimensionless,
                                    targetTicksWithingTolerance: Int = 1,
                                    backwards: Boolean = false)
                                   (drive: Drivetrain)
                                   (implicit properties: Signal[DrivetrainProperties],
                                    hardware: DrivetrainHardware) extends FiniteTask {
    override def onStart(): Unit = {
      var ticksWithinTolerance = 0

      val (unicycle, error) = followWayPointsController(
        wayPoints,
        position,
        turnPosition,
        maxTurnOutput,
        backwards
      )

      drive.setController(childVelocityControl(speedControl(unicycle.withCheckZipped(error) { e =>
        if (e.exists(_ < tolerance)) {
          ticksWithinTolerance += 1

          if (ticksWithinTolerance >= targetTicksWithingTolerance) {
            finished()
          }
        } else {
          ticksWithinTolerance = 0
        }
      })))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }
}
