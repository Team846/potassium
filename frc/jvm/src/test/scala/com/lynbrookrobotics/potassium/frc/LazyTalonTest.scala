package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.ErrorCode
import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import com.lynbrookrobotics.potassium.control.offload.EscConfig.{NativePositionGains, NativeVelocityGains}
import com.lynbrookrobotics.potassium.control.offload.OffloadedSignal._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar
import squants.{Each, Percent}

class LazyTalonTest extends FunSuite with MockitoSugar {
  test("LazyTalons apply OpenLoop is lazy") {
    val mockedTalon = mock[TalonSRX]
    val lazyTalon = new LazyTalon(mockedTalon, 0, 0, -1, 1)

    var percOutCalled = 0

    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.PercentOutput), anyDouble())).then(_ => {
      percOutCalled += 1
    })
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Position), anyDouble())).thenThrow(MockTalonCalled)
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Velocity), anyDouble())).thenThrow(MockTalonCalled)
    when(mockedTalon.config_kP(anyInt(), anyDouble(), anyInt())).thenThrow(MockTalonCalled)
    when(mockedTalon.config_kI(anyInt(), anyDouble(), anyInt())).thenThrow(MockTalonCalled)
    when(mockedTalon.config_kD(anyInt(), anyDouble(), anyInt())).thenThrow(MockTalonCalled)
    when(mockedTalon.config_kF(anyInt(), anyDouble(), anyInt())).thenThrow(MockTalonCalled)

    val octrl1 = OpenLoop(Percent(1))
    val octrl2 = OpenLoop(Percent(2))

    lazyTalon.applyCommand(octrl1)
    lazyTalon.applyCommand(octrl1)
    lazyTalon.applyCommand(octrl1)
    assert(percOutCalled == 1)

    lazyTalon.applyCommand(octrl2)
    lazyTalon.applyCommand(octrl2)
    lazyTalon.applyCommand(octrl2)
    assert(percOutCalled == 2)

    lazyTalon.applyCommand(octrl1)
    lazyTalon.applyCommand(octrl1)
    lazyTalon.applyCommand(octrl1)
    assert(percOutCalled == 3)
  }

  test("LazyTalons apply PositionBangBang is lazy") {
    val mockedTalon = mock[TalonSRX]
    val lazyTalon = new LazyTalon(mockedTalon, 0, 0, -1, 1)

    var cpofCalled = 0
    var cporCalled = 0
    var kpSetCalled = 0
    var posOutCalled = 0

    when(mockedTalon.configPeakOutputForward(anyDouble(), anyInt())).then(_ => {
      cpofCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.configPeakOutputReverse(anyDouble(), anyInt())).then(_ => {
      cporCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kP(anyInt(), anyDouble(), anyInt())).then(_ => {
      kpSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Position), anyDouble())).then(_ => {
      posOutCalled += 1
    })

    val pctrl1 = PositionBangBang(forwardWhenBelow = true, reverseWhenAbove = true, Each(1))
    val pctrl2 = PositionBangBang(forwardWhenBelow = false, reverseWhenAbove = false, Each(2))
    val pctrl3 = PositionBangBang(forwardWhenBelow = true, reverseWhenAbove = false, Each(3))
    val pctrl4 = PositionBangBang(forwardWhenBelow = false, reverseWhenAbove = true, Each(4))

    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    assert(cpofCalled == 0)
    assert(cporCalled == 0)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 1)

    lazyTalon.applyCommand(pctrl2)
    lazyTalon.applyCommand(pctrl2)
    lazyTalon.applyCommand(pctrl2)
    assert(cpofCalled == 1)
    assert(cporCalled == 1)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 2)

    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    assert(cpofCalled == 2)
    assert(cporCalled == 2)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 3)

    lazyTalon.applyCommand(pctrl3)
    lazyTalon.applyCommand(pctrl3)
    lazyTalon.applyCommand(pctrl3)
    assert(cpofCalled == 2)
    assert(cporCalled == 3)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 4)

    lazyTalon.applyCommand(pctrl4)
    lazyTalon.applyCommand(pctrl4)
    lazyTalon.applyCommand(pctrl4)
    assert(cpofCalled == 3)
    assert(cporCalled == 4)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 5)
  }

  test("LazyTalons apply VelocityBangBang is lazy") {
    val mockedTalon = mock[TalonSRX]
    val lazyTalon = new LazyTalon(mockedTalon, 0, 0, -1, 1)

    var cpofCalled = 0
    var cporCalled = 0
    var kpSetCalled = 0
    var posOutCalled = 0

    when(mockedTalon.configPeakOutputForward(anyDouble(), anyInt())).then(_ => {
      cpofCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.configPeakOutputReverse(anyDouble(), anyInt())).then(_ => {
      cporCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kP(anyInt(), anyDouble(), anyInt())).then(_ => {
      kpSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Velocity), anyDouble())).then(_ => {
      posOutCalled += 1
    })

    val vctrl1 = VelocityBangBang(forwardWhenBelow = true, reverseWhenAbove = true, Each(1))
    val vctrl2 = VelocityBangBang(forwardWhenBelow = false, reverseWhenAbove = false, Each(2))
    val vctrl3 = VelocityBangBang(forwardWhenBelow = true, reverseWhenAbove = false, Each(3))
    val vctrl4 = VelocityBangBang(forwardWhenBelow = false, reverseWhenAbove = true, Each(4))

    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    assert(cpofCalled == 0)
    assert(cporCalled == 0)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 1)

    lazyTalon.applyCommand(vctrl2)
    lazyTalon.applyCommand(vctrl2)
    lazyTalon.applyCommand(vctrl2)
    assert(cpofCalled == 1)
    assert(cporCalled == 1)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 2)

    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    assert(cpofCalled == 2)
    assert(cporCalled == 2)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 3)

    lazyTalon.applyCommand(vctrl3)
    lazyTalon.applyCommand(vctrl3)
    lazyTalon.applyCommand(vctrl3)
    assert(cpofCalled == 2)
    assert(cporCalled == 3)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 4)

    lazyTalon.applyCommand(vctrl4)
    lazyTalon.applyCommand(vctrl4)
    lazyTalon.applyCommand(vctrl4)
    assert(cpofCalled == 3)
    assert(cporCalled == 4)
    assert(kpSetCalled == 1)
    assert(posOutCalled == 5)
  }

  test("LazyTalons apply PositionPID is lazy") {
    val mockedTalon = mock[TalonSRX]
    val lazyTalon = new LazyTalon(mockedTalon, 0, 0, -1, 1)

    var posOutCalled = 0
    var kpSetCalled = 0
    var kiSetCalled = 0
    var kdSetCalled = 0

    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.PercentOutput), anyDouble())).thenThrow(MockTalonCalled)
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Velocity), anyDouble())).thenThrow(MockTalonCalled)
    when(mockedTalon.config_kF(anyInt(), anyDouble(), anyInt())).thenThrow(MockTalonCalled)

    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Position), anyDouble())).then(_ => {
      posOutCalled += 1
    })
    when(mockedTalon.config_kP(anyInt(), anyDouble(), anyInt())).then(_ => {
      kpSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kI(anyInt(), anyDouble(), anyInt())).then(_ => {
      kiSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kD(anyInt(), anyDouble(), anyInt())).then(_ => {
      kdSetCalled += 1
      ErrorCode.OK
    })

    val pctrl1 = PositionPID(NativePositionGains(1, 2, 3), Each(4))
    val pctrl2 = PositionPID(NativePositionGains(5, 6, 7), Each(8))

    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    assert(posOutCalled == 1)
    assert(kpSetCalled == 1)
    assert(kiSetCalled == 1)
    assert(kdSetCalled == 1)

    lazyTalon.applyCommand(pctrl2)
    lazyTalon.applyCommand(pctrl2)
    lazyTalon.applyCommand(pctrl2)
    assert(posOutCalled == 2)
    assert(kpSetCalled == 2)
    assert(kiSetCalled == 2)
    assert(kdSetCalled == 2)

    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    lazyTalon.applyCommand(pctrl1)
    assert(posOutCalled == 3)
    assert(kpSetCalled == 3) // this will break once/if we implement idx
    assert(kiSetCalled == 3)
    assert(kdSetCalled == 3)
  }

  test("LazyTalons apply VelocityPIDF is lazy") {
    val mockedTalon = mock[TalonSRX]
    val lazyTalon = new LazyTalon(mockedTalon, 0, 0, -1, 1)

    var velOutCalled = 0
    var kpSetCalled = 0
    var kiSetCalled = 0
    var kdSetCalled = 0
    var kfSetCalled = 0

    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.PercentOutput), anyDouble())).thenThrow(MockTalonCalled)
    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Position), anyDouble())).thenThrow(MockTalonCalled)

    when(mockedTalon.set(ArgumentMatchers.eq(ControlMode.Velocity), anyDouble())).then(_ => {
      velOutCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kP(anyInt(), anyDouble(), anyInt())).then(_ => {
      kpSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kI(anyInt(), anyDouble(), anyInt())).then(_ => {
      kiSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kD(anyInt(), anyDouble(), anyInt())).then(_ => {
      kdSetCalled += 1
      ErrorCode.OK
    })
    when(mockedTalon.config_kF(anyInt(), anyDouble(), anyInt())).then(_ => {
      kfSetCalled += 1
      ErrorCode.OK
    })

    val vctrl1 = VelocityPIDF(NativeVelocityGains(1, 2, 3, 4), Each(5))
    val vctrl2 = VelocityPIDF(NativeVelocityGains(6, 7, 8, 9), Each(10))

    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    assert(velOutCalled == 1)
    assert(kpSetCalled == 1)
    assert(kiSetCalled == 1)
    assert(kdSetCalled == 1)
    assert(kfSetCalled == 1)

    lazyTalon.applyCommand(vctrl2)
    lazyTalon.applyCommand(vctrl2)
    lazyTalon.applyCommand(vctrl2)
    assert(velOutCalled == 2)
    assert(kpSetCalled == 2)
    assert(kiSetCalled == 2)
    assert(kdSetCalled == 2)
    assert(kfSetCalled == 2)

    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    lazyTalon.applyCommand(vctrl1)
    assert(velOutCalled == 3)
    assert(kpSetCalled == 3) // this will break once/if we implement idx
    assert(kiSetCalled == 3)
    assert(kdSetCalled == 3)
    assert(kfSetCalled == 3)
  }
}

object MockTalonCalled extends RuntimeException