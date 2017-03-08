package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.{Angle, Dimensionless}
import squants.space.{Angle, _}

trait PurePursuitControllers {
  /**
    * use pid control on the distance from the target point
    * @param target the target point to reach
    * @param properties unicycle properties
    * @param hardware unicycle hardware
    * @return dimensionless, forward output value and distance to target
    */
  def pointDistanceControl(position: PeriodicSignal[Point],
                           target: PeriodicSignal[Point])
                          (implicit properties: Signal[UnicycleProperties],
                           hardware: UnicycleHardware): (PeriodicSignal[Dimensionless],
                                                   PeriodicSignal[Length]) = {
    val distanceToTarget = position.zip(target).map { p =>
      p._1 distanceTo p._2
    }

    (PIDF.pid(
      Signal.constant(Feet(0)).toPeriodic,
      distanceToTarget,
      properties.map(_.forwardPositionControlGains)),
      distanceToTarget)
  }

  /**
    * Given a 2 segment path, calculates the look ahead point by finding the
    * intersection of the circle of radius lookAheadDistance and center
    * currentPosition with the given path. Returns the intersection with the
    * second segment if there is an intersection with both segments.
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html
    *
    * @param biSegmentPath the path to find given intersection on
    * @param currPosition
    * @param lookAheadDistance
    * @return the look ahead point for the given path
    */
  def getLookAheadPoint(biSegmentPath: (Segment, Option[Segment]),
                                  currPosition: Point,
                                  lookAheadDistance: Length): Point = {
    import MathUtilities._
    val firstLookAheadPoint = intersectionClosestToEnd(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val secondLookAheadPoint = biSegmentPath._2.flatMap { s =>
      intersectionClosestToEnd(s, currPosition, lookAheadDistance)
    }

    secondLookAheadPoint.getOrElse(
      firstLookAheadPoint.getOrElse {
        println(s"expanding look ahead distance to ${1.1 * lookAheadDistance}")
        println(s"path $biSegmentPath")
        println(s"current position $currPosition")
        getLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
      }
    )
  }

  def headingToPoint(start: Point, end: Point): Angle = {
      val diff = end - start
      Radians(Math.atan2(
        diff.y.toFeet,
        diff.x.toFeet
      ))
  }

  def trigonemtricToCompass(angle: Angle): Angle = {
    Degrees(90) - angle
  }

  /**
    * returns an angle limited from -180 to 180 equivalent to the input
    * @param degrees
    * @return angle betwee -270 and 90
    */
  def limitTo180Range(degrees: Angle): Angle = {
    if (degrees < Degrees(-270)) {
      limitTo180Range(degrees + Degrees(360))
    } else if (degrees > Degrees(90)) {
      limitTo180Range(degrees - Degrees(360))
    } else {
      degrees
    }
  }
  /**
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html
    * @param angle the current compass angle, with the0 degrees defined at the y axis.
    * @param position the current x,y,z position of the robot
    * @param biSegmentPath the path to follow
    * @return turn output, what to multiply forward output by (1 for forward, -1 for backwards),
    *         lookAheadPoint
    */
  def purePursuitControllerTurn(angle: Signal[Angle],
                                position: PeriodicSignal[Point],
                                biSegmentPath: Signal[(Segment, Option[Segment])])
                               (implicit properties: Signal[UnicycleProperties]): (PeriodicSignal[Dimensionless],
                                                                                   PeriodicSignal[Double],
                                                                                   PeriodicSignal[Point]) = {
    val lookAheadPoint = position.zip(properties).zip(biSegmentPath).map{p =>
      val ((pose, props), path) = p
      getLookAheadPoint(
        path,
        pose,
        props.defaultLookAheadDistance
      )
    }

    val heading = position.zip(lookAheadPoint).map(
      p => headingToPoint(p._1, p._2)).map(trigonemtricToCompass(_))

    val clampedError = heading.zip(angle.map(limitTo180Range)).map { t =>
      val (heading, angle) = t
      val error = heading - angle

      println(s"error is $error, heading is $heading")

      if (error <= Degrees(-90)) {
        println(s"reversing direction, new error ${error + Degrees(180)}")
        (error + Degrees(180), -1D)
      } else if (error >= Degrees(90)) {
        println(s"reversing direction, new error ${error - Degrees(180)}")
        (error - Degrees(180), -1D)
      } else {
        (error, 1D)
      }
    }

    (
      PIDF.pid(
        Signal.constant(Degrees(0)).toPeriodic,
        clampedError.map(_._1),
        properties.map(_.turnPositionControlGains)),
      clampedError.map(_._2),
      lookAheadPoint
    )
  }

  @deprecated
  def getLookExtrapolatedAheadPoint(biSegmentPath: (Segment, Option[Segment]),
    currPosition: Point,
    lookAheadDistance: Length): Point = {
    import MathUtilities._
    val firstLookAheadPoint = intersectionLineCircleFurthestFromStart(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val secondLookAheadPoint = biSegmentPath._2.flatMap { s =>
      intersectionClosestToEnd(s, currPosition, lookAheadDistance)
    }

    secondLookAheadPoint.getOrElse(
      firstLookAheadPoint.getOrElse {
        println(s"expanding look ahead distance to ${1.1 * lookAheadDistance}")
        println(s"path $biSegmentPath")
        println(s"current position $currPosition")
        getLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
      }
    )
  }

  def followWayPointsController(wayPoints: Seq[Point],
                                position: PeriodicSignal[Point],
                                turnPosition: Signal[Angle])
                               (implicit hardware: UnicycleHardware,
                                signal: Signal[UnicycleProperties]): (PeriodicSignal[UnicycleSignal], Signal[Option[Length]]) = {
    var previousLookAheadPoint: Option[Point] = None

    val biSegmentPaths = wayPoints.sliding(3).map { points =>
      (
        Segment(points(0), points(1)),
        if (points.size == 3) {
          Some(Segment(points(1), points(2)))
        } else {
          None
        }
      )
    }

    var currPath: (Segment, Option[Segment]) = biSegmentPaths.next()

    val selectedPath = Signal {
      if (currPath._2.isEmpty) {
      } else if (previousLookAheadPoint.exists(p => currPath._2.get.containsInXY(p, Feet(0.1)))) {
        if (biSegmentPaths.hasNext) {
          println("**********advancing path ***********")
          currPath = biSegmentPaths.next()
        }
      }

      currPath
    }

    val (turnOutput, multiplier, lookAheadPoint) = purePursuitControllerTurn(
      turnPosition,
      position,
      selectedPath)
    val historyUpdatingLookAheadPoint = lookAheadPoint.withCheck{p =>
      previousLookAheadPoint = Some(p)
    }

    val forwardOutput = pointDistanceControl(
      position,
      historyUpdatingLookAheadPoint)._1
    val distanceToLast = position.map(_ distanceTo wayPoints.last)

    val errorToLast = distanceToLast.peek.map { d =>
      (if (!biSegmentPaths.hasNext) {
        Some(d)
      } else {
        None
      }).flatten
    }

    (forwardOutput.zip(turnOutput).zip(multiplier).zip(distanceToLast).map { o =>
      val (((forward, turn), fdMultiplier), _) = o
      UnicycleSignal(forward * fdMultiplier, turn)
    }, errorToLast)
  }
}

