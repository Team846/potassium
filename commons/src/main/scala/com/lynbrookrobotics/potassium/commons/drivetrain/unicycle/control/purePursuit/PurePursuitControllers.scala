package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.UnicycleCoreControllers
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleHardware, UnicycleProperties, UnicycleSignal, UnicycleVelocity}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.UnicycleMotionProfileControllers
import squants.{Dimensionless, Each, Percent}
import squants.space._
import MathUtilities._
import squants.motion._

import scala.annotation.tailrec

sealed trait ForwardBackwardMode
case object Auto extends ForwardBackwardMode
case object ForwardsOnly extends ForwardBackwardMode
case object BackwardsOnly extends ForwardBackwardMode

trait PurePursuitControllers extends UnicycleCoreControllers with UnicycleMotionProfileControllers {
  /**
    * use pid control on the distance from the target point
    * @param target the target point to reach
    * @param properties unicycle properties
    * @param hardware unicycle hardware
    * @return dimensionless, forward output value and distance to target
    */
  def pointDistanceControl(position: Stream[Point],
                           target: Stream[Point],
                           maxVelocity: Velocity,
                           acceleration: Acceleration,
                           deceleration: Acceleration)
                          (implicit properties: Signal[DrivetrainProperties],
                           hardware: DrivetrainHardware): Stream[Dimensionless] = {
    val distanceToTarget = position.zip(target).map { p =>
      p._1.distanceTo(p._2)
    }

    val targetVelocity = trapezoidalDriveControl(
      maxVelocity,
      FeetPerSecond(0),
      acceleration,
      deceleration,
      distanceToTarget.mapToConstant(Feet(0)),
      distanceToTarget,
      hardware.forwardVelocity)._1


    targetVelocity.map(v => Each(v / properties.get.maxForwardVelocity))
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
    * @param degrees the original, uncapped angle
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
                                maxTurnOutput: Dimensionless,
                                forwardBackwardMode: ForwardBackwardMode,
                                angleDeadband: Angle = Degrees(0))
                               (implicit props: Signal[DrivetrainProperties],
                                hardware: DrivetrainHardware): (Stream[Dimensionless], Stream[Double]) = {
    val lookAheadPoint = position.zip(biSegmentPath).map { case (pose, path) =>
      getExtrapolatedLookAheadPoint(
        path,
        pose,
        props.get.defaultLookAheadDistance
      )
    }

    val headingToTarget = position.zip(lookAheadPoint).map { p =>
      headingToPoint(p._1, p._2._1)
    }

    val errorToTarget = headingToTarget.map(convertTrigonometricAngleToCompass).zip(turnPosition).map { case (target, current) =>
      target - current
    }

    val compassHeadingToLookAheadAndReversed: Stream[(Angle, Boolean)] = errorToTarget
      .map(h => limitToPlusMinus90(h))
      .zip(lookAheadPoint.map(_._2)).map { case ((angle, autoReversed), purposefullyReversed) =>
      if ((forwardBackwardMode == Auto) ||
          (autoReversed && forwardBackwardMode == BackwardsOnly) ||
          (!autoReversed && forwardBackwardMode == ForwardsOnly) ||
          purposefullyReversed /* we are doing the opposite of what is requested due to overshoot */) {
        (angle, autoReversed)
      } else {
        (
          if (angle > Degrees(0)) {
            angle - Degrees(180)
          } else {
            angle + Degrees(180)
          },
          forwardBackwardMode == BackwardsOnly
        )
      }
    }

    val compassHeadingToLookAhead = compassHeadingToLookAheadAndReversed.map(_._1)

    val limitedTurn = PIDF.pid(
      errorToTarget.mapToConstant(Degrees(0)),
      compassHeadingToLookAhead,
      props.map(_.turnPositionGains)
    ).map(clamp(_, maxTurnOutput))

    val limitedAndWithinDeadband = limitedTurn.zip(errorToTarget).map{ case(turn, error) =>
      if (error.abs <= angleDeadband) {
        Percent(0)
      } else {
        turn
      }
    }

    val reversed = compassHeadingToLookAheadAndReversed.map(_._2)
    val forwardMultiplier = reversed.map(b => if (b) -1D else 1).withCheck(e => println(s"forward multiplier is $e"))

    (limitedAndWithinDeadband, forwardMultiplier)
  }


  /**
    * Finds the look ahead, extrapolating beyond the segment when needed
    * @param biSegmentPath a group of the current and next segments
    * @param currPosition the current position of the robot
    * @param lookAheadDistance the look ahead distance to find a point at
    * @return the look ahead point, and a boolean that is true when the robot is backing up after overshoot
    */
  @tailrec
  final def getExtrapolatedLookAheadPoint(biSegmentPath: (Segment, Option[Segment]),
                                    currPosition: Point,
                                    lookAheadDistance: Length): (Point, Boolean) = {
    val lookAheadOnCurrentSegment = intersectionRayCircleFurthestFromStart(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val lookAheadOnNextSegment = biSegmentPath._2.flatMap { s =>
      intersectionRayCircleFurthestFromStart(s, currPosition, lookAheadDistance)
    }

    implicit val tolerance: Angle = Radians(0.00001)

    if (lookAheadOnNextSegment.isDefined) {
      val laPoint = lookAheadOnNextSegment.get
      // We project the robot's location onto the segment line (not line segment) and verify that it is on the line
      // by comparing the angle of the projected point to the end versus the start to the end. If these are equal,
      // the robot has not overshot but if they are different (off by 180 degrees), the robot has overshot and we need
      // to forcibly reverse the direction of the robot.
      val angleRobotProjectionToEnd = Segment(biSegmentPath._2.get.pointClosestToOnLine(currPosition), biSegmentPath._2.get.end).angle
      (laPoint, !(angleRobotProjectionToEnd ~= biSegmentPath._2.get.angle))
    } else if (lookAheadOnCurrentSegment.isDefined) {
      val laPoint = lookAheadOnCurrentSegment.get
      val angleRobotProjectionToEnd = Segment(biSegmentPath._1.pointClosestToOnLine(currPosition), biSegmentPath._1.end).angle
      (laPoint, !(angleRobotProjectionToEnd ~= biSegmentPath._1.angle))
    } else {
      getExtrapolatedLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
    }
  }

  def followWayPointsController(wayPoints: Seq[Point],
                                position: Stream[Point],
                                turnPosition: Stream[Angle],
                                maxTurnOutput: Dimensionless,
                                cruisingVelocity: Velocity,
                                forwardBackwardMode: ForwardBackwardMode,
                                angleDeadband: Angle = Degrees(0))
                                (implicit hardware: DrivetrainHardware,
                                 props: Signal[DrivetrainProperties]): (Stream[UnicycleSignal], Stream[Option[Length]]) = {
    println("in follow way points controller")
    val biSegmentPaths = wayPoints.sliding(2).map { points =>
      Segment(
        points.head,
        points.tail.headOption.getOrElse(
          throw new IllegalArgumentException("There must be at least two waypoints. Did you forget to add the initial point?"
          )
        )
      )
    }.sliding(2).map(l => (l.head, l.tail.headOption))

    var currPath = biSegmentPaths.next()

    val selectedPath = position.map { currPos =>
      if (biSegmentPaths.hasNext &&
          currPath._2.exists(s => intersectionRayCircleFurthestFromStart(s, currPos, props.get.defaultLookAheadDistance).isDefined)) {
        println("********** Advancing pure pursuit path ***********")
        currPath = biSegmentPaths.next()
      }

      currPath
    }

    val (turnOutput, multiplier) = purePursuitControllerTurn(
      turnPosition,
      position,
      selectedPath,
      maxTurnOutput,
      forwardBackwardMode,
      angleDeadband
    )

    val forwardOutput = pointDistanceControl(
      position,
      selectedPath.map(p => p._2.getOrElse(p._1).end),
      cruisingVelocity,
      props.get.maxAcceleration,
      props.get.maxDeceleration
    )

    val distanceToLast = position.map { pose =>
      Segment(wayPoints.init.last, wayPoints.last).pointClosestToOnLine(pose).distanceTo(wayPoints.last)
    }

    val limitedAndReversedForward = forwardOutput.zip(multiplier).map(t => t._1 * t._2)

    val errorToLast = distanceToLast.map { d =>
      // error does not exist if we are not on our last segment
      if (!biSegmentPaths.hasNext) {
        Some(d)
      } else {
        None
      }
    }

    (
      limitedAndReversedForward.zip(turnOutput).withCheck(z => println(s"zipped is")).map { case (forward, turn) =>
        UnicycleSignal(forward, turn)
      },
      errorToLast
    )
  }
}

