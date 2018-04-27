package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal._

class LazyTalon(
  val t: TalonSRX,
  val idx: Int = 0,
  val timeout: Int = 0,
  val defaultPeakOutputReverse: Double = -1,
  val defaultPeakOutputForward: Double = 1
) {
  private var last: Option[OffloadedSignal] = None
  def getLastCommand: Option[OffloadedSignal] = last

  def applyCommand(s: OffloadedSignal): Unit = s match {
    case it: OpenLoop         => applyCommand(it)
    case it: PositionBangBang => applyCommand(it)
    case it: VelocityBangBang => applyCommand(it)
    case it: PositionPID      => applyCommand(it)
    case it: VelocityPIDF     => applyCommand(it)
    case _                    => println(s"Unknown type $s")
  }

  t.configPeakOutputReverse(defaultPeakOutputReverse, timeout)
  t.configPeakOutputForward(defaultPeakOutputForward, timeout)

  private var lastPeakOutputReverse = defaultPeakOutputReverse
  private var lastPeakOutputForward = defaultPeakOutputForward

  private def setPeakAndNominalOutputs(
    peakOutputReverse: Double = defaultPeakOutputReverse,
    peakOutputForward: Double = defaultPeakOutputForward
  ): Unit = {
    if (lastPeakOutputReverse != peakOutputReverse) {
      t.configPeakOutputReverse(peakOutputReverse, timeout)
      lastPeakOutputReverse = peakOutputReverse
    }
    if (lastPeakOutputForward != peakOutputForward) {
      t.configPeakOutputForward(peakOutputForward, timeout)
      lastPeakOutputForward = peakOutputForward
    }
  }

  def applyCommand(s: PositionPID): Unit = {
    import s.gains._
    import s.signal
    setPeakAndNominalOutputs()
    last match {
      case Some(last: PositionPID) =>
        if (p != last.gains.p) t.config_kP(idx, p, timeout)
        if (i != last.gains.i) t.config_kI(idx, i, timeout)
        if (d != last.gains.d) t.config_kD(idx, d, timeout)
        if (signal != last.signal) t.set(ControlMode.Position, signal.toEach)
      case _ =>
        setPeakAndNominalOutputs()
        t.config_kP(idx, p, timeout)
        t.config_kI(idx, i, timeout)
        t.config_kD(idx, d, timeout)
        t.set(ControlMode.Position, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: VelocityPIDF): Unit = {
    import s.gains._
    import s.signal
    setPeakAndNominalOutputs()
    last match {
      case Some(last: VelocityPIDF) =>
        if (p != last.gains.p) t.config_kP(idx, p, timeout)
        if (i != last.gains.i) t.config_kI(idx, i, timeout)
        if (d != last.gains.d) t.config_kD(idx, d, timeout)
        if (f != last.gains.f) t.config_kF(idx, f, timeout)
        if (signal != last.signal) t.set(ControlMode.Velocity, signal.toEach)
      case _ =>
        setPeakAndNominalOutputs()
        t.config_kP(idx, p, timeout)
        t.config_kI(idx, i, timeout)
        t.config_kD(idx, d, timeout)
        t.config_kF(idx, f, timeout)
        t.set(ControlMode.Velocity, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: PositionBangBang): Unit = {
    import s._
    setPeakAndNominalOutputs(
      peakOutputForward = if (forwardWhenBelow) defaultPeakOutputForward else 0,
      peakOutputReverse = if (reverseWhenAbove) defaultPeakOutputReverse else 0
    )
    last match {
      case Some(last: PositionBangBang) =>
        if (signal != last.signal) t.set(ControlMode.Position, signal.toEach)
      case _ =>
        t.config_kP(idx, 1023, timeout)
        t.set(ControlMode.Position, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: VelocityBangBang): Unit = {
    import s._
    setPeakAndNominalOutputs(
      peakOutputForward = if (forwardWhenBelow) defaultPeakOutputForward else 0,
      peakOutputReverse = if (reverseWhenAbove) defaultPeakOutputReverse else 0
    )
    last match {
      case Some(last: VelocityBangBang) =>
        if (signal != last.signal) t.set(ControlMode.Velocity, signal.toEach)
      case _ =>
        t.config_kP(idx, 1023, timeout)
        t.set(ControlMode.Velocity, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: OpenLoop): Unit = {
    import s.signal
    setPeakAndNominalOutputs()
    last match {
      case Some(last: OpenLoop) =>
        if (signal != last.signal) t.set(ControlMode.PercentOutput, signal.toEach)
      case _ =>
        t.set(ControlMode.PercentOutput, signal.toEach)
    }
    last = Some(s)
  }
}
