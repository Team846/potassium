package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.units.{Point, Segment}
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.{Angle, Dimensionless}
import squants.space.{Degrees, Feet, Length, Radians}

trait AdvancedPositionControllers {
  /**
    * use pid control on the distance from the target point
    * @param target the target point to reach
    * @param properties unicycle properties
    * @param hardware unicycle hardware
    * @return dimensionless, forward output value and distance to target
    */
  def pointForwardControl(position: PeriodicSignal[Point],
                          target: PeriodicSignal[Point])
                         (implicit properties: Signal[UnicycleProperties],
                          hardware: UnicycleHardware): (PeriodicSignal[Dimensionless],
                                                   PeriodicSignal[Length]) = {
    val distanceToTarget = position.zip(target).map { p =>
      (p._1 - p._2).magnitude
    }

    (PIDF.pid(
      Signal.constant(Feet(0)).toPeriodic,
      distanceToTarget,
      properties.map(_.forwardPositionControlGains)), distanceToTarget)
  }

  /**
    * Given a 2 segment path, calculates the look ahead point by finding the
    * intersection of the circle of radius lookAheadDistance and center
    * currentPosition with the given path. Returns the intersection with the
    * second segment if there is an intersection with both segments.
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html
    *
    * @param biSegmentPath
    * @param currPosition
    * @param lookAheadDistance
    * @return
    */
  def getLookAheadPoint(biSegmentPath: (Segment, Option[Segment]),
                                  currPosition: Point,
                                  lookAheadDistance: Length): Point = {
    import MathUtilities._
    val firstLookAheadPoint = intersectionClosestToEnd(
      biSegmentPath._1,
      currPosition,
      lookAheadDistance)

    val secondLookAheadPoint = biSegmentPath._2.flatMap(
      intersectionClosestToEnd(_, currPosition, lookAheadDistance))

    secondLookAheadPoint.getOrElse(
      firstLookAheadPoint.getOrElse {
        println(s"expanding look ahead distance to ${1.1 * lookAheadDistance}")
        getLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
      }
    )
  }

  def headingToTarget(currentPosition: PeriodicSignal[Point],
                      target: PeriodicSignal[Point])
                     (implicit properties: Signal[UnicycleProperties]): PeriodicSignal[Angle] = {
    currentPosition.zip(target).map { p =>
      val diff = p._2 - p._1
      Radians(Math.atan2(
        diff.y.toFeet,
        diff.x.toFeet
      ))
    }
  }

  /**
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html
    * @param angle the current angle, with the0 degrees defined at the y axis.
    * @param position the current x,y,z position of the robot
    * @param target the look ahead point to track
    * @return
    */
  def purePursuitControllerTurn(angle: Signal[Angle],
                                position: PeriodicSignal[Point],
                                target: PeriodicSignal[Point])
                               (implicit properties: Signal[UnicycleProperties]): (PeriodicSignal[Dimensionless], PeriodicSignal[(Angle, Double)]) = {
    val heading = headingToTarget(position, target).map(-_ + Degrees(90))

    val control = heading.zip(angle).map { t =>
      val (heading, angle) = t
      val error = heading - angle

      if (error <= Degrees(-90)) {
        (heading + Degrees(180), -1D)
      } else if (error >= Degrees(90)) {
        (heading - Degrees(180), -1D)
      } else {
        (heading, 1D)
      }
    }

    (PIDF.pid(
      angle.toPeriodic,
      control.map(_._1),
      properties.map(_.turnPositionControlGains)), control)
  }
}

