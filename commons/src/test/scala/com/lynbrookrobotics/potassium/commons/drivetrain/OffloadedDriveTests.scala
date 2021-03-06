package com.lynbrookrobotics.potassium.commons.drivetrain

import java.lang.Math.PI

import com.lynbrookrobotics.potassium.commons.drivetrain.offloaded.OffloadedDriveProperties
import com.lynbrookrobotics.potassium.control.offload.EscConfig
import com.lynbrookrobotics.potassium.units.Ratio
import org.scalatest.{Assertion, FunSuite}
import squants.motion.AngularVelocity
import squants.space.{Inches, Length, Turns}
import squants.time.Milliseconds
import squants.{Acceleration, Angle, Dimensionless, Each, Percent, Time, Velocity}

class OffloadedDriveTests extends FunSuite {
  test("OffloadedLiftProperties floorPerTick returns correct values") {
    def asserteq(a: Double, b: Double): Assertion = assert(Math.abs(a - b) < 0.0001)
    val props = new OffloadedDriveProperties {
      override val escConfig: EscConfig[Length] = null
      override val rightVelocityGains: ForwardVelocityGains = null
      override val maxRightVelocity: Velocity = null
      override val maxLeftVelocity: Velocity = null
      override val leftVelocityGains: ForwardVelocityGains = null
      override val defaultLookAheadDistance: Length = null
      override val forwardPositionGains: ForwardPositionGains = null
      override val turnPositionGains: TurnPositionGains = null
      override val maxTurnVelocity: AngularVelocity = null
      override val turnVelocityGains: TurnVelocityGains = null
      override val maxAcceleration: Acceleration = null
      override val maxDeceleration: Acceleration = null
      override val blendExponent: Double = 0
      override val track: Length = null

      /**
       * 5pi inches / 1 wheel rotation
       * 1 wheel rotation / 2 encoder rotations
       * 1 encoder rotation / 100 encoder ticks
       * => 5pi in / 200 tick
        **/
      override val wheelDiameter: Length = Inches(5)
      override val wheelOverEncoderGears: Ratio[Angle, Angle] = Ratio(Turns(1), Turns(2))
      override val encoderAngleOverTicks: Ratio[Angle, Dimensionless] = Ratio(Turns(1), Each(100))
    }
    import props.floorPerTick
    asserteq(
      floorPerTick.num.toInches / floorPerTick.den.toEach,
      5 * PI / 200
    )
  }
}
