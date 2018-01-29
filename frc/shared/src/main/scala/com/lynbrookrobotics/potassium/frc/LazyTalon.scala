package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import com.lynbrookrobotics.potassium.control.{OffloadedSignal, OpenLoop, PositionControl, VelocityControl}

class LazyTalon(val t: TalonSRX, val idx: Int, val timeout: Int) {
  private var last: Option[OffloadedSignal] = None

  def applyCommand(s: OffloadedSignal): Unit = s match {
    case it: OpenLoop => applyCommand(it)
    case it: PositionControl => applyCommand(it)
    case it: VelocityControl => applyCommand(it)
    case _ => println(s"Unknown type $s")
  }

  def applyCommand(s: PositionControl): Unit = {
    import s.gains._
    import s.signal
    last match {
      case Some(last: PositionControl) =>
        if (p != last.gains.p) t.config_kP(idx, p, timeout)
        if (i != last.gains.i) t.config_kI(idx, i, timeout)
        if (d != last.gains.d) t.config_kD(idx, d, timeout)
        if (signal != last.signal) t.set(ControlMode.Position, signal.toEach)
      case _ =>
        t.config_kP(idx, p, timeout)
        t.config_kI(idx, i, timeout)
        t.config_kD(idx, d, timeout)
        t.set(ControlMode.Position, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: VelocityControl): Unit = {
    import s.gains._
    import s.signal
    last match {
      case Some(last: VelocityControl) =>
        if (p != last.gains.p) t.config_kP(idx, p, timeout)
        if (i != last.gains.i) t.config_kI(idx, i, timeout)
        if (d != last.gains.d) t.config_kD(idx, d, timeout)
        if (f != last.gains.f) t.config_kF(idx, f, timeout)
        if (signal != last.signal) t.set(ControlMode.Velocity, signal.toEach)
      case _ =>
        t.config_kP(idx, p, timeout)
        t.config_kI(idx, i, timeout)
        t.config_kD(idx, d, timeout)
        t.config_kF(idx, f, timeout)
        t.set(ControlMode.Velocity, signal.toEach)
    }
    last = Some(s)
  }

  def applyCommand(s: OpenLoop): Unit = {
    import s.signal
    last match {
      case Some(last: OpenLoop) =>
        if (signal != last.signal) t.set(ControlMode.PercentOutput, signal.toEach)
      case _ =>
        t.set(ControlMode.PercentOutput, signal.toEach)
    }
    last = Some(s)
  }
}