package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.{Dimensionless, Each}
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

      val (forwardOutput, forwardError) = pointDistanceControl(
        position,
        Signal.constant(target).toPeriodic)

      val (turnOutput, heading, lookAheadPoint) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        Signal.constant((Segment(origin, target), None)))

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(lookAheadPoint).zip(heading).map { o =>
        val (((forward, turn), _), fdMultiplier) = o
        UnicycleSignal(fdMultiplier * clamp(forward, Each(0.4)), turn)
      }.withCheck { _ =>
        forwardError.peek.get.foreach { e =>
          if (e.abs <= tolerance) {
            println("finished GoToPoint!")
            finished()
          }
        }
      }

      drive.setController(lowerLevelOpenLoop(unicycleOutput))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class Follow2WayPoints(wayPoints: (Point, Point), tolerance: Length)
                        (implicit drive: Drivetrain,
                        properties: Signal[UnicycleProperties],
                        hardware: UnicycleHardware) extends FiniteTask {
    val defaultPath = Segment(
      origin,
      new Point(Feet(0), Feet(0))
    )

    override def onStart(): Unit = {
      val selectedPath = Signal.constant {
        (Segment(origin, wayPoints._1), Some(Segment(wayPoints._1, wayPoints._2)))
      }

      val initialTurnPosition = hardware.turnPosition.get

      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )

      val (turnOutput, heading, lookAheadPoint) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        selectedPath)
      val (forwardOutput, error) = pointDistanceControl(
        position, lookAheadPoint)

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), fdMultiplier) = o
        UnicycleSignal((forward min Each(0.4) max Each(-0.4)) * fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          if (e.abs <= tolerance) {
            println("finished follow 2 way points")
            finished()
          }
        }
      }

      drive.setController(lowerLevelOpenLoop(unicycleOutput))
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
      var previousLookAheadPoint = origin

      val biSegmentPaths = (origin :: wayPoints).sliding(3).map { points =>
        (
          Segment(points.head, points(1)),
          Some(Segment(points(1), points(2)))
        )
      }
      var currPath = (
        origin segmentTo wayPoints.head,
        Some(wayPoints.head segmentTo wayPoints(1))
      )
      val selectedPath = Signal {
        implicit val tolerance = Feet(0.01)
        if (currPath._2.get.containsInXY(previousLookAheadPoint, Feet(0.1))) {
          if (biSegmentPaths.hasNext) {
            println("**********advancing path ***********")
            currPath = biSegmentPaths.next()
          }
        }
        currPath
      }

      val initialTurnPosition = hardware.turnPosition.get
      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )


      val (turnOutput, heading, lookAheadPoint) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        selectedPath)
      val historyUpdatingLookAheadPoint = lookAheadPoint.withCheck{p =>
        println(s"look ahead point $p")
        previousLookAheadPoint = p
      }

      val forwardOutput = pointDistanceControl(
        position,
        historyUpdatingLookAheadPoint)._1
      val distanceToLast = position.map(_ distanceTo wayPoints.last)

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), fdMultiplier) = o
        UnicycleSignal((forward min Each(0.4) max Each(-0.4)) * fdMultiplier, turn)
      }.withCheck { _ =>
        distanceToLast.peek.get.foreach { distance =>
          if (!biSegmentPaths.hasNext) {
            if (distance <= tolerance) {
              println(s"finished, with error $distance")
              finished()
            }
          }
        }
      }

      drive.setController(lowerLevelOpenLoop(unicycleOutput))
    }

    override def onEnd(): Unit = {
      println("follow way points ended")
      drive.resetToDefault()
    }
  }
}
