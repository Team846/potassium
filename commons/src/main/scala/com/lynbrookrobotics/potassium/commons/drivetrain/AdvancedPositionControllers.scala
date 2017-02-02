package com.lynbrookrobotics.potassium.commons.drivetrain



import com.lynbrookrobotics.potassium.commons.{Point, Segment}
import com.lynbrookrobotics.potassium.control.PIDF
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import squants.{Angle, Dimensionless}
import squants.space.{Feet, Length, Radians}

class AdvancedPositionControllers {
  /**
    * Given a 2 segment path, calculates the look ahead point by finding the
    * intersection of the circle of radius lookAheadDistance and center
    * currentPosition with the given path. Returns the intersection with the
    * second segment if there is an intersection with both segments.
    * see: https://www.mathworks.com/help/robotics/ug/pure-pursuit-controller.html?requestedDomain=www.mathworks.com
    * @param biSegmentPath
    * @param currPosition
    * @param lookAheadDistance
    * @return
    */
  protected def getLookAheadPoint(biSegmentPath: (Segment,  Option[Segment]),
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
      firstLookAheadPoint.getOrElse(
        getLookAheadPoint(biSegmentPath, currPosition, 1.1 * lookAheadDistance)
      )
    )
  }

  def headingToTarget(currentPosition: Signal[Point],
                      biSegmentPath: (Segment, Option[Segment]))
                     (implicit properties: Signal[UnicycleProperties]): Signal[Angle] = {
    val lookAheadPoint = currentPosition.zip(properties).map{
      case(currentPosition, properties) =>
        getLookAheadPoint(
          biSegmentPath,
          currentPosition,
          properties.defaultLookAheadDistance)}

    currentPosition.zip(lookAheadPoint).map {
      case (pos, lookAhead) => Radians(Math.atan((pos zip lookAhead).xySlope))
    }
  }

  // Kind of strange. I need to be doing PID, but that returns a dimensionless
  // So to clarify that all this controller does is calculate the turn output,
  // I specify it in the name. Any better ideas?
  def purePursuitControllerTurnOutput(
      angle: Signal[Angle],
      currentPosition: Signal[Point],
      biSegmentPath: (Segment, Option[Segment]))
     (implicit properties: Signal[UnicycleProperties]): PeriodicSignal[Dimensionless] = {
    PIDF.pidf(
      angle.toPeriodic,
      headingToTarget(currentPosition, biSegmentPath).toPeriodic,
      properties.map(_.turnControlGainsFull))
  }
}
