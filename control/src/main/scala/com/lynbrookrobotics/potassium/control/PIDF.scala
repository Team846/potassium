package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.PeriodicSignal
import com.lynbrookrobotics.potassium.units._
import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}

case class PIDFConfig[S <: Quantity[S],
                      SWithD <: Quantity[SWithD] with TimeIntegral[D],
                      SWithI <: Quantity[SWithI] with TimeDerivative[I],
                      D <: Quantity[D] with TimeDerivative[SWithD],
                      I <: Quantity[I] with TimeIntegral[SWithI],
                      U <: Quantity[U]](kp: Ratio[U, S], kd: Ratio[U, D], ki: Ratio[U, I], kf: Ratio[U, S])

object PIDF {
  def proportionalControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S], target: PeriodicSignal[S], gain: Ratio[U, S]): PeriodicSignal[U] = {
    signal.zip(target).map(p => (p._2 - p._1) ** gain)
  }

  def derivativeControl[S <: Quantity[S] with TimeIntegral[D],
                        U <: Quantity[U],
                        D <: Quantity[D] with TimeDerivative[S]](signal: PeriodicSignal[S],
                                                                 gain: Ratio[U, D]): PeriodicSignal[U] = {
    signal.derivative.map(d => d ** gain)
  }

  def integralControl[S <: Quantity[S] with TimeDerivative[I],
                      U <: Quantity[U],
                      I <: Quantity[I] with TimeIntegral[S]](signal: PeriodicSignal[S],
                                           gain: Ratio[U, I]): PeriodicSignal[U] = {
    signal.integral.map(d => d ** gain)
  }

  def feedForwardControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S], gain: Ratio[U, S]): PeriodicSignal[U] = {
    signal.map(v => v ** gain)
  }

  def pidf[S <: Quantity[S],
           SWithD <: Quantity[SWithD] with TimeIntegral[D],
           SWithI <: Quantity[SWithI] with TimeDerivative[I],
           D <: Quantity[D] with TimeDerivative[SWithD],
           I <: Quantity[I] with TimeIntegral[SWithI],
           U <: Quantity[U]](signal: PeriodicSignal[S], target: PeriodicSignal[S], config: PIDFConfig[S, SWithD, SWithI, D, I, U])
                            (implicit exD: S => SWithD, exI: S => SWithI): PeriodicSignal[U] = {
    proportionalControl(signal, target, config.kp).
      zip(integralControl(signal.map(exI), config.ki)).
      zip(derivativeControl(signal.map(exD), config.kd)).
      zip(feedForwardControl(target, config.kf)).map { pidf =>
      val (((p, i), d), f) = pidf
      p + i + d + f
    }
  }
}
