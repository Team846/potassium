package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.UnicycleCoreControllers
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleHardware, UnicycleProperties, UnicycleSignal}
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
                           hardware: UnicycleHardware): Stream[Dimensionless] = {
    val distanceToTarget = position.zip(target).map { p =>
      p._1 distanceTo p._2
    }

    PIDF.pid(
      distanceToTarget.mapToConstant(Feet(0)),
      distanceToTarget,
      properties.map(_.forwardPositionGains))
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
    * @return angle between -90 and 90
    */
  def limitToPlusMinus90(degrees: Angle): (Angle, Boolean) = {
    if (degrees < Degrees(-90)) {
      val (angle, reversed) = limitToPlusMinus90(degrees + Degrees(180))
      (angle, !reversed)
    } else if (degrees > Degrees(90)) {
      val (angle, reversed) = limitToPlusMinus90(degrees - Degrees(180))
      (angle, !reversed)
    } else {
      (degrees, false)
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
                               (implicit props: Signal[DrivetrainProperties],
                                hardware: DrivetrainHardware): (Stream[Dimensionless], Stream[Double], Stream[Point]) = {
    val lookAheadPoint = position.zip(biSegmentPath).map { case (pose, path) =>
      getExtrapolatedLookAheadPoint(
        path,
        pose,
        props.get.defaultLookAheadDistance
      )
    }

    val headingToTarget = position.zip(lookAheadPoint).map {p =>
      headingToPoint(p._1, p._2)
    }

    val errorToTarget = headingToTarget.map(MathUtilities.swapTrigonemtricAndCompass).zip(turnPosition).map { case (target, current) =>
      target - current
    }
    val compassHeadingToLookAheadAndReversed = errorToTarget.map(h => limitToPlusMinus90(h))
    val compassHeadingToLookAhead = compassHeadingToLookAheadAndReversed.map(_._1)
    val reversed = compassHeadingToLookAheadAndReversed.map(_._2)
    val forwardMultiplier = reversed.map(b => if (b) -1D else 1)

    val limitedTurn = PIDF.pid(
      errorToTarget.mapToConstant(Degrees(0)),
      compassHeadingToLookAhead,
      props.map(_.turnPositionGains)
    ).map(MathUtilities.clamp(_, maxTurnOutput))

    (limitedTurn, forwardMultiplier, lookAheadPoint)
  }


  def getExtrapolatedLookAheadPoint(biSegmentPath: (Segment, Option[Segment]),
                                    currPosition: Point,
                                    lookAheadDistance: Length): Point = {
    import MathUtilities._
    val lookAheadOnCurrentSegment = intersectionLineCircleFurthestFromStart(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val lookAheadOnNextSegment = biSegmentPath._2.flatMap { s =>
      intersectionLineCircleFurthestFromStart(s, currPosition, lookAheadDistance)
    }

    lookAheadOnNextSegment.getOrElse(
      lookAheadOnCurrentSegment.getOrElse(
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
    val biSegmentPaths = wayPoints.sliding(2).map { points =>
      Segment(points(0), points(1))
    }.sliding(2).map(l => (l.head, Some(l.last)))

    var currPath = biSegmentPaths.next()
    var previousLookAheadPoint: Option[Point] = None

    val selectedPath = position.map { _ =>
      if (previousLookAheadPoint.exists { p =>
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

    val forwardOutput = pointDistanceControl(
      position,
      selectedPath.map(p => p._2.getOrElse(p._1).end)
    )

    val distanceToLast = position.map { pose =>
      pose distanceTo wayPoints.last
    }

    val limitedAndReversedForward = forwardOutput.map { s =>
      if (!biSegmentPaths.hasNext) {
        s min steadyOutput
      } else {
        steadyOutput
      }
    }.zip(multiplier).map(t => t._1 * t._2)

    val errorToLast = distanceToLast.map { d =>
      // error does not exist if we are not on our last segment
      if (!biSegmentPaths.hasNext) {
        Some(d)
      } else {
        None
      }
    }.withCheckZipped(lookAheadPoint) { p =>
      previousLookAheadPoint = Some(p)
    }

    (
      limitedAndReversedForward.zip(turnOutput).map { case (forward, turn) =>
        UnicycleSignal(forward, turn)
      },
      errorToLast
    )
  }
}

