package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.space.{Angle, Degrees, Feet, Length}

trait AdvancedPositionTasks extends UnicycleCoreTasks {
  import controllers._

  val origin = new Point(
    Feet(0),
    Feet(0)
  )

  def compassToTrigonometric(angle: Angle): Angle = {
    Degrees(90) - angle
  }

  class GoToPoint(target: Point, tolerance: Length)
                 (implicit drive: Drivetrain,
                         properties: Signal[UnicycleProperties],
                         hardware: UnicycleHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val initialTurnPosition = hardware.turnPosition.get

      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )

      val path = (Segment(origin, target), None)

      val lookAheadPoint = position.zip(properties).map { p =>
        val (pose, props) = p
        getLookAheadPoint(
          path,
          pose,
          props.defaultLookAheadDistance)
      }

      val (forwardOutput, error) = pointForwardControl(position, lookAheadPoint)
      val (turnOutput, heading) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(lookAheadPoint).zip(heading).map { o =>
        val (((forward, turn), _), (_, fdMultiplier)) = o
        UnicycleSignal(forward * fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          if (e.abs <= tolerance) {
            finished()
          }
        }
      }

      drive.setController(toDriveSignal(unicycleOutput))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class Follow2WayPoints(wayPoints: List[Point], tolerance: Length)
                        (implicit drive: Drivetrain,
                        properties: Signal[UnicycleProperties],
                        hardware: UnicycleHardware) extends FiniteTask {
    if (wayPoints.length > 2) {
      throw new IllegalArgumentException("Follow2WayPoints can only" +
        " take 2 way points!")
    }

    override def onStart(): Unit = {
      val selectedPath = Signal.constant {
        (Segment(origin, wayPoints.head), Some(Segment(wayPoints.head, wayPoints.last)))
      }

      val initialTurnPosition = hardware.turnPosition.get

      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )

      val lookAheadPoint = position.zip(properties).zip(selectedPath).map { p =>
        val ((pose, props), path) = p
        val ahead = getLookAheadPoint(
          path, pose,
          props.defaultLookAheadDistance
        )

        ahead
      }

      val (forwardOutput, error) = pointForwardControl(
        position, lookAheadPoint)
      val (turnOutput, heading) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), (_, fdMultiplier)) = o
        UnicycleSignal(forward * fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          if (e.abs <= tolerance) {
            finished()
          }
        }
      }

      drive.setController(toDriveSignal(unicycleOutput))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class FollowWayPoints(wayPoints: List[Point], tolerance: Length)
                       (implicit drive: Drivetrain,
                        properties: Signal[UnicycleProperties],
                        hardware: UnicycleHardware) extends FiniteTask {
    override def onStart(): Unit = {
      val biSegmentPaths = (origin :: wayPoints).sliding(3).map { points =>
        (
          Segment(points(0), points(1)),
          Some(Segment(points(1), points(2)))
        )
      }

      val initialTurnPosition = hardware.turnPosition.get

      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )

      var previousLookAheadPoint = origin

      var currPath: (Segment, Option[Segment]) = (origin segmentTo wayPoints(0), Some(wayPoints(0) segmentTo wayPoints(1)))
      val selectedPath = Signal {
        implicit val tolerance = Feet(0.01)
        if (currPath._2.get.containsInXY(previousLookAheadPoint, Feet(0.1))) {
          if (biSegmentPaths.hasNext) {
            println("advancing path")
            currPath = biSegmentPaths.next()
            println(s"path: $currPath")
            println(s"look ahead is $previousLookAheadPoint")
          }

          currPath
        } else {
          currPath
        }
      }

      val lookAheadPoint = position.zip(properties).zip(selectedPath).map { p =>
        val ((pose, props), path) = p
        getLookAheadPoint(
          path,
          pose,
          props.defaultLookAheadDistance
        )
      }.withCheck(p => previousLookAheadPoint = p)

      val (forwardOutput, error) = pointForwardControl(position, lookAheadPoint)

      val (turnOutput, heading)= purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), (_, fdMultiplier)) = o
        UnicycleSignal(forward * fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          if (!biSegmentPaths.hasNext) {
            if (e <= tolerance) {
              finished()
            }
          }
        }
      }

      drive.setController(toDriveSignal(unicycleOutput))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }
}
