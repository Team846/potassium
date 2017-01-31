package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.control.PIDF
import squants.Angle
import squants.motion.{AngularVelocity, Velocity}
import squants.space.{Area, Length}


class AdvancedPositionControllers {

  private def getLookAheadPoint (
                                  trajectory: (Segment, Segment),
                                  pose: Value3D[Length],
                                  lookAheadDistance: Length): Value3D[Length] = {

    val firstLookAheadPoint = intersectionClosestToEnd(
      trajectory._1,
      pose,
      lookAheadDistance)
    val secondLookAheadPoint = intersectionClosestToEnd(
      trajectory._2,
      pose,
      lookAheadDistance)

    secondLookAheadPoint.getOrElse(
      firstLookAheadPoint.getOrElse{
        getLookAheadPoint(trajectory, pose, 2 * lookAheadDistance)
      }
    )
  }

  def purePursuitDriveController(
                                  cruiseVelocity: Velocity,
                                  wayPoints: List[Value3D[Length]],
                                  angle: Signal[Angle],
                                  position: Signal[Value3D[Length]],
                                  biSegmentPath: (Segment, Segment))
                                (implicit properties: UnicycleProperties): PeriodicSignal[AngularVelocity] = {
    val lookAheadPoint = getLookAheadPoint(
      biSegmentPath,
      position.get,
      properties.defaultLookAheadDistance)

    val headingToLookAheadPoint = position.map { pos =>
      Math.atan(
        (lookAheadPoint.x - pos.x) / (lookAheadPoint.y - pos.y)
      )}

    PIDF.proportionalControl(
      angle.toPeriodic,
      headingToLookAheadPoint.toPeriodic,
      properties.turnGain)
  }

  case class Segment(start: Value3D[Length], end: Value3D[Length]) {
    def diff = end - start

    def length: Length = {
      (diff.x * diff.x + diff.y * diff.y + diff.z * diff.z).squareRoot
    }

    def dot(other: Segment): Area = {
      dot(end - start, other.end - other.start)
    }

    def dot(other: Value3D[Length]): Area = {
      dot(end - start, other)
    }

    def dot(a: Value3D[Length], b: Value3D[Length]): Area = {
      a.x * b.x + a.y * b.y + a.z * b.z
    }
  }

  /**
    * Finds intersection between segment and circle with given center and radius
    * lookAheadDistance that is closest to segment.end
    * @param segment
    * @param pos
    * @param lookAheadDistance
    * @return
    */
  def intersectionClosestToEnd(segment: Segment, pos: Value3D[Length], lookAheadDistance: Length): Option[Value3D[Length]] = {
    import Math._
    val dy = (segment.end.y - segment.start.y).toFeet
    val dx = (segment.end.y - segment.start.y).toFeet
    val diffEnd    = segment.end - pos
    val diffStart  = segment.start - pos
    val posX = pos.x.toFeet
    val posY = pos.y.toFeet
    val dr_squared = (segment.length * segment.length).toSquareFeet

    val det = (diffStart.x * diffEnd.y - diffEnd.x * diffStart.y).toSquareFeet
    val discriminant = dr_squared * lookAheadDistance.toFeet * lookAheadDistance.toFeet - det * det

    if (discriminant < 0) None
    else {
      val sqrtDiscriminant = sqrt(discriminant)
      val signDy = signum(dy)

      val posSolution = Value3D(
        Feet((det * dy + signDy * sqrtDiscriminant * dx ) / dr_squared + posX),
        Feet((-det * dx + abs(dy) * sqrtDiscriminant) / dr_squared + posY),
        Feet(0)
      )
      val negSolution = Value3D(
        Feet((det * dy - signDy * sqrtDiscriminant * dx ) / dr_squared + posX),
        Feet((-det * dx - abs(dy) * sqrtDiscriminant) / dr_squared + posY),
        Feet(0)
      )
      val negSolutionToEnd = Segment(negSolution, segment.end)
      val posSolutionToEnd = Segment(posSolution, segment.end)
      if (posSolutionToEnd.length >= negSolutionToEnd.length) {
        Option(posSolution)
      } else {
        Option(negSolution)
      }
    }
  }

  //
  //    val pos_dot_product = (segment dot posSolution).toSquareFeet
  //    val neg_dot_product = (segment dot posSolution).toSquareFeet
  //
  //    if (pos_dot_product < 0 && neg_dot_product < 0) {
  //      Option(negSolution)
  //    } else if (pos_dot_product >= 0 && neg_dot_product >= 0) {
  //      Option(posSolution)
  //    } else {
  //      if (abs(pos_dot_product) <= abs(neg_dot_product)) {
  //        Option(posSolution)
  //      } else {
  //        Option(negSolution)
  //      }
  //    }
}
