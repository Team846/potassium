package com.lynbrookrobotics.potassium.commons.drivetrain.purePursuit

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleCoreControllers, UnicycleHardware, UnicycleProperties, UnicycleSignal}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import squants.Dimensionless
import squants.space.{Angle, _}

trait PurePursuitControllers extends UnicycleCoreControllers {
  /**
    * use pid control on the distance from the target point
    * @param target the target point to reach
    * @param properties unicycle properties
    * @param hardware unicycle hardware
    * @return dimensionless, forward output value and distance to target
    */
  def pointDistanceControl(position: Stream[Point],
                           target: Stream[Point])
                          (implicit properties: Signal[UnicycleProperties],
                           hardware: UnicycleHardware): (Stream[Dimensionless],
                                                   Stream[Length]) = {
    val distanceToTarget = position.zip(target).map { p =>
//      println(s"position: ${p._1} target: ${p._2}")
      p._1 distanceTo p._2
    }

    (PIDF.pid(
      distanceToTarget.mapToConstant(Feet(0)),
      distanceToTarget,
      properties.map(_.forwardPositionControlGains)),
      distanceToTarget)
  }

  def headingToPoint(start: Point, end: Point): Angle = {
      val diff = end - start
      Radians(Math.atan2(
        diff.y.toFeet,
        diff.x.toFeet
      ))
  }

  /**
    * returns an angle limited from -180 to 180 equivalent to the input
    * @param degrees
    * @return angle betwee -90 and 90
    */
  def limitToPlusMinus90(degrees: Angle): Angle = {
    if (degrees < Degrees(-90)) {
      limitToPlusMinus90(degrees + Degrees(180))
    } else if (degrees > Degrees(90)) {
      limitToPlusMinus90(degrees - Degrees(180))
    } else {
      degrees
    }
  }
  /**
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html
    * @param position the current x,y,z position of the robot
    * @param biSegmentPath the path to follow
    * @param turnPosition the current (relative) compass angle
    * @return turn output, what to multiply forward output by (1 for forward, -1 for backwards),
    *         lookAheadPoint
    */
  def purePursuitControllerTurn(turnPosition: Stream[Angle],
                                position: Stream[Point],
                                biSegmentPath: Stream[(Segment, Option[Segment])],
                                maxTurnOutput: Dimensionless)
                               (implicit props: Signal[DrivetrainProperties], hardware: DrivetrainHardware): (Stream[Dimensionless],
                                                                                   Stream[Double],
                                                                                   Stream[Point]) = {
    val lookAheadPoint = position.zip(biSegmentPath).map{ case (pose, path) =>
      getExtrapolatedLookAheadPoint(
        path,
        pose,
        props.get.defaultLookAheadDistance
      )
    }

    val headingToTarget = position.zip(lookAheadPoint).map{p =>
      headingToPoint(p._1, p._2)
    }

    val trigHeadingToTarget = headingToTarget.map(MathUtilities.swapTrigonemtricAndCompass)
    val compassHeadingToLookAhead = trigHeadingToTarget.map(h => limitToPlusMinus90(h))

    val forwardMultiplier = position.zip(turnPosition).zip(lookAheadPoint).zip(biSegmentPath).map {p =>
      val (((pose, currAngle), lookAhead), path) = p
      val lastSegment = path._2.getOrElse(path._1)

      if (lookAhead.onLine(lastSegment, Feet(0.1))) {
        val currTrigAngle = MathUtilities.swapTrigonemtricAndCompass(currAngle)
        val angleErrorToLastWayPoint = headingToPoint(pose, lastSegment.end) - currTrigAngle
        if (angleErrorToLastWayPoint.abs >= Degrees(90)) {
          -1D
        } else {
          1D
        }
      } else {
        1D
      }
    }
    val limitedTurn = PIDF.pid(
      turnPosition,
      compassHeadingToLookAhead,
      props.map(_.turnPositionControlGains)
    ).map(MathUtilities.clamp(_, maxTurnOutput))

    (limitedTurn, forwardMultiplier, lookAheadPoint)
  }


  def getExtrapolatedLookAheadPoint(biSegmentPath: (Segment, Option[Segment]),
                                    currPosition: Point,
                                    lookAheadDistance: Length): Point = {
    import MathUtilities._
    val firstLookAheadPoint = intersectionLineCircleFurthestFromStart(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val secondLookAheadPoint = biSegmentPath._2.flatMap { s =>
      intersectionLineCircleFurthestFromStart(s, currPosition, lookAheadDistance)
    }

    secondLookAheadPoint.getOrElse(
      firstLookAheadPoint.getOrElse(
        getExtrapolatedLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
      )
    )
  }

  def followWayPointsController(wayPoints: Seq[Point],
                                position: Stream[Point],
                                turnPosition: Stream[Angle],
                                steadyOutput: Dimensionless,
                                maxTurnOutput: Dimensionless)
                                (implicit hardware: DrivetrainHardware,
                                props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Option[Length]]) = {
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

    var currPath = biSegmentPaths.next()
    var previousLookAheadPoint: Option[Point] = None

    val selectedPath = position.map { _ =>
      if (previousLookAheadPoint.exists{ p =>
        currPath._2.exists(_.containsInXY(p, Feet(0.1)))
      }) {
        if (biSegmentPaths.hasNext) {
          println("********** advancing path ***********")
          currPath = biSegmentPaths.next()
        }
      }

      currPath
    }

    val (turnOutput, multiplier, lookAheadPoint) = purePursuitControllerTurn(
      turnPosition,
      position,
      selectedPath,
      maxTurnOutput)
    val lookAheadHandle = lookAheadPoint.foreach{ p =>
      previousLookAheadPoint = Some(p)
    }

    val forwardOutput = pointDistanceControl(
      position,
      selectedPath.map(p => p._2.getOrElse(p._1).end)
    )._1
    val distanceToLast = position.map{ pose =>
      pose distanceTo wayPoints.last
    }

    val limitedForward = forwardOutput.map{ s =>
      if ( !biSegmentPaths.hasNext ) {
        s min steadyOutput
      } else {
        steadyOutput
      }
    }

    val errorToLast = distanceToLast.map { d =>
      // error does not exist if we are not on our last segment
      if (!biSegmentPaths.hasNext) {
        Some(d)
      } else {
        None
      }
    }

    (
      limitedForward.zip(turnOutput).zip(multiplier).map { case ((forward, turn), fdMultiplier) =>
        UnicycleSignal(forward * fdMultiplier, turn)
      },
      errorToLast
    )
  }
}

