package com.lynbrookrobotics.potassium.commons.position

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.cartesianPosition.XYPosition
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.tasks.FiniteTask
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.Each
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
        println(s"look ahead: ${
          getLookAheadPoint(
            path,
            pose,
            props.defaultLookAheadDistance)
        }")
        getLookAheadPoint(
          path,
          pose,
          props.defaultLookAheadDistance)
      }

      val (forwardOutput, error) = pointForwardControl(position, Signal.constant(target).toPeriodic)
      val (turnOutput, heading) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint,
        false
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(lookAheadPoint).zip(heading).map { o =>
        val (((forward, turn), _), (_, fdMultiplier)) = o
        println(s"forward output ${forward.toEach}")
        UnicycleSignal((forward min Each(0.4) max Each(-0.4))* fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          println(s"error ${e.toFeet}")
          val lookAhead = lookAheadPoint.peek.get.getOrElse(origin)
          val currentPosition = position.peek.get.getOrElse(origin)
          println(s"curr pos ${currentPosition}  lookAhead $lookAhead")
          if (e.abs <= tolerance) {
            println("finished!")
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

    val defaultPath = Segment(
      origin,
      new Point(Feet(0), Feet(0))
    )

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
        println(s"look ahead is $ahead")
        ahead
      }

      val (forwardOutput, error) = pointForwardControl(
        position, lookAheadPoint)
      val (turnOutput, heading) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint,
        selectedPath.get._2.getOrElse(defaultPath).containsInXY(
          lookAheadPoint.peek.get.getOrElse(origin), Feet(0.1))
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), (_, fdMultiplier)) = o
        UnicycleSignal((forward min Each(0.4) max Each(-0.4))* fdMultiplier, turn)
      }.withCheck { _ =>
        error.peek.get.foreach { e =>
          if (e.abs <= tolerance) {
            println("finished follow 2 way points")
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
      val pathsToPrint = (origin :: wayPoints).sliding(3).map { points =>
        (
          Segment(points.head, points(1)),
          Some(Segment(points(1), points(2)))
        )
      }.toList
      println("**********filler ************")
      println(s"paths")
      println(s"${pathsToPrint.foreach(print)}")
      val biSegmentPaths = pathsToPrint.toIterator

      val initialTurnPosition = hardware.turnPosition.get

      val position = XYPosition(
        hardware.turnPosition.map(_ - initialTurnPosition).map(compassToTrigonometric),
        hardware.forwardPosition
      )

      var previousLookAheadPoint = origin

      var currPath: (Segment, Option[Segment]) = (origin segmentTo wayPoints(0), Some(wayPoints(0) segmentTo wayPoints(1)))
      val selectedPath = Signal {
        implicit val tolerance = Feet(0.01)
//        println(s"passing ${previousLookAheadPoint} to contains")
        if (currPath._2.get.containsInXY(previousLookAheadPoint, Feet(0.1))) {
          if (biSegmentPaths.hasNext) {
            println("**********advancing path ***********")
            println(s"current point ${position.peek.get.get}")
            println(s"previous look $previousLookAheadPoint")
            println(s"previous curr path $currPath")
            currPath = biSegmentPaths.next()
            println(s"new path: $currPath")
            println(s"look ahead is $previousLookAheadPoint")
          }

          currPath
        } else {
          currPath
        }
      }

      val lookAheadPoint = position.zip(properties).zip(selectedPath).map { p =>
        val ((pose, props), path) = p
        val point = getLookAheadPoint(
          path,
          pose,
          props.defaultLookAheadDistance
        )
        println(s"look ahead $point pose $pose")
        implicit val tolerance = Feet(0.1)
        if ((point.x ~= Feet(0)) && (point.y ~= Feet(5))) {
          println(s"jumped to (0, 5). selected Path $path  position $pose look ahead distance${props.defaultLookAheadDistance}")
        }
        point
      }.withCheck(p => previousLookAheadPoint = p)

      val (forwardOutput, error) = pointForwardControl(position, lookAheadPoint)
      val distanceToLast = position.map(_ distanceTo wayPoints.last)

      val (turnOutput, heading) = purePursuitControllerTurn(
        hardware.turnPosition.map(_ - initialTurnPosition),
        position,
        lookAheadPoint,
        biSegmentPaths.hasNext
      )

      val unicycleOutput = forwardOutput.zip(turnOutput).zip(heading).map { o =>
        val ((forward, turn), (_, fdMultiplier)) = o
        UnicycleSignal((forward min Each(0.4) max Each(-0.4))* fdMultiplier, turn)
      }.withCheck { _ =>
        distanceToLast.peek.get.foreach { e =>
          if (!biSegmentPaths.hasNext) {
            if (e <= tolerance) {
              println(s"finished, with error $e")
              finished()
            }
          }
        }
      }

      drive.setController(toDriveSignal(unicycleOutput))
    }

    override def onEnd(): Unit = {
      println("follow way points ended")
      drive.resetToDefault()
    }
  }
}
