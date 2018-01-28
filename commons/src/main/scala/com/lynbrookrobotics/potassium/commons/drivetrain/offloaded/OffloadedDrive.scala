package com.lynbrookrobotics.potassium.commons.drivetrain.offloaded

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.{TwoSided, TwoSidedDrive}
import com.lynbrookrobotics.potassium.commons.drivetrain.{ForwardPositionGains, ForwardVelocityGains}
import com.lynbrookrobotics.potassium.control.OffloadedSignal.{EscPositionGains, EscVelocityGains}
import com.lynbrookrobotics.potassium.control._
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.GenericIntegral
import squants.motion.Acceleration
import squants.space.Length
import squants.time.{Milliseconds, Seconds}
import squants.{Dimensionless, Velocity}

abstract class OffloadedDrive extends TwoSidedDrive {
  override type DriveSignal = TwoSided[OffloadedSignal]
  override type Properties <: OffloadedProperties

  override def velocityControl(target: Stream[TwoSided[Velocity]])
                              (implicit hardware: Hardware,
                               props: Signal[Properties]): Stream[DriveSignal] = target.map {
    case TwoSided(left, right) =>
      implicit val curProps: Properties = props.get
      TwoSided(
        VelocityControl(
          forwardToAngularVelocityGains(curProps.leftVelocityGainsFull), ticks(left)
        ),
        VelocityControl(
          forwardToAngularVelocityGains(curProps.rightVelocityGainsFull), ticks(right)
        )
      )
  }

  def positionControl(target: Stream[TwoSided[Length]])
                     (implicit hardware: Hardware,
                      props: Signal[Properties]): Stream[DriveSignal] = target.map {
    case TwoSided(left, right) =>
      implicit val curProps: Properties = props.get
      TwoSided(
        PositionControl(
          forwardToAngularPositionGains(curProps.forwardPositionGains), ticks(left)
        ),
        PositionControl(
          forwardToAngularPositionGains(curProps.forwardPositionGains), ticks(right)
        )
      )
  }

  def forwardToAngularVelocityGains(g: ForwardVelocityGains#Full)
                                   (implicit props: Properties): EscVelocityGains = {
    val outOvrPercent = props.escNativeOutputOverPercent.num.toEach / props.escNativeOutputOverPercent.den.toEach
    val ftOvrTicks = props.floorPerTick.num.toFeet / props.floorPerTick.den.toEach
    val escTimeOvrSec = 1 /*native time unit*/ / props.escTimeConst.toSeconds
    val millisOvrSec = 1000 /*milliseconds*/ / 1 /*seconds*/
    import g.{kd, kf, ki, kp}

    EscVelocityGains(
      p = (kp.num.toEach / kp.den.toFeetPerSecond) * ftOvrTicks * escTimeOvrSec * outOvrPercent,
      i = (ki.num.toEach / ki.den.toFeet) * ftOvrTicks * outOvrPercent,
      d = (kd.num.toEach / kd.den.toFeetPerSecondSquared) * ftOvrTicks * escTimeOvrSec * millisOvrSec * outOvrPercent,
      f = (kf.num.toEach / kf.den.toFeetPerSecond) * ftOvrTicks * escTimeOvrSec * outOvrPercent
    )
  }

  def forwardToAngularPositionGains(g: ForwardPositionGains)
                                   (implicit props: Properties): EscPositionGains = {
    val outOvrPercent = props.escNativeOutputOverPercent.num.toEach / props.escNativeOutputOverPercent.den.toEach
    val ftOvrTicks = props.floorPerTick.num.toFeet / props.floorPerTick.den.toEach
    val escTimeOvrSec = 1 /*native time unit*/ / props.escTimeConst.toSeconds
    val millisOvrSec = 1000 /*milliseconds*/ / 1
    /*seconds*/
    val t = Seconds(1)
    import g.{kd, ki, kp}

    EscPositionGains(
      p = (kp.num.toEach / kp.den.toFeet) * ftOvrTicks * outOvrPercent,
      i = ((ki.num.toEach / t).toHertz / (ki.den / t).toFeet) * ftOvrTicks * outOvrPercent / millisOvrSec,
      d = (kd.num.toEach / kd.den.toFeetPerSecond) * ftOvrTicks * outOvrPercent * escTimeOvrSec
    )
  }

  override protected def openLoopToDriveSignal(openLoopInput: TwoSided[Dimensionless]): TwoSided[OffloadedSignal] =
    TwoSided(OpenLoop(openLoopInput.left), OpenLoop(openLoopInput.right))

  def ticks(x: GenericIntegral[Length])(implicit p: Properties): Dimensionless = ticks(x / Milliseconds(1))

  def ticks(x: Length)(implicit p: Properties): Dimensionless = p.floorPerTick.recip * x

  def ticks(x: Velocity)(implicit p: Properties): Dimensionless = ticks(x * p.escTimeConst)

  def ticks(x: Acceleration)(implicit p: Properties): Dimensionless = ticks(x * Milliseconds(1))
}