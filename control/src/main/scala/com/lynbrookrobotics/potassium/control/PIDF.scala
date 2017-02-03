package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.units._
import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}

case class PIDFConfig[S <: Quantity[S],
                      SWithD <: Quantity[SWithD] with TimeIntegral[D],
                      SWithI <: Quantity[SWithI] with TimeDerivative[I],
                      D <: Quantity[D] with TimeDerivative[SWithD],
                      I <: Quantity[I] with TimeIntegral[SWithI],
                      U <: Quantity[U]](kp: Ratio[U, S], ki: Ratio[U, I], kd: Ratio[U, D], kf: Ratio[U, S])

object PIDF {
  def proportionalControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S],
                                                              target: PeriodicSignal[S],
                                                              gain: Signal[Ratio[U, S]]): PeriodicSignal[U] = {
    signal.zip(target).map(p => (p._2 - p._1) ** gain.get)
  }

  def derivativeControl[S <: Quantity[S] with TimeIntegral[D],
                        U <: Quantity[U],
                        D <: Quantity[D] with TimeDerivative[S]](signal: PeriodicSignal[S],
                                                                 gain: Signal[Ratio[U, D]]): PeriodicSignal[U] = {
    signal.derivative.map(d => d ** gain.get)
  }

  def integralControl[S <: Quantity[S] with TimeDerivative[I],
                      U <: Quantity[U],
                      I <: Quantity[I] with TimeIntegral[S]](signal: PeriodicSignal[S],
                                                             gain: Signal[Ratio[U, I]]): PeriodicSignal[U] = {
    signal.integral.map(d => d ** gain.get)
  }

  def feedForwardControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S], gain: Signal[Ratio[U, S]]): PeriodicSignal[U] = {
    signal.map(v => v ** gain.get)
  }

  def pidf[S <: Quantity[S],
           SWithD <: Quantity[SWithD] with TimeIntegral[D],
           SWithI <: Quantity[SWithI] with TimeDerivative[I],
           D <: Quantity[D] with TimeDerivative[SWithD],
           I <: Quantity[I] with TimeIntegral[SWithI],
           U <: Quantity[U]](signal: PeriodicSignal[S], target: PeriodicSignal[S], config: Signal[PIDFConfig[S, SWithD, SWithI, D, I, U]])
                            (implicit exD: S => SWithD, exI: S => SWithI): PeriodicSignal[U] = {
    proportionalControl(signal, target, config.map(_.kp)).
      zip(integralControl(signal.map(exI), config.map(_.ki))).
      zip(derivativeControl(signal.map(exD), config.map(_.kd))).
      zip(feedForwardControl(target, config.map(_.kf))).map { pidf =>
      val (((p, i), d), f) = pidf
      p + i + d + f
    }
  }
}
