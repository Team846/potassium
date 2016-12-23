package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.PeriodicSignal
import com.lynbrookrobotics.potassium.units._
import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}

case class PIDConfig[S <: Quantity[S] with TimeDerivative[_] with TimeIntegral[_],
                     D <: Quantity[D] with TimeDerivative[S],
                     I <: Quantity[I] with TimeIntegral[S],
                     U <: Quantity[U]](kp: Ratio[U, S], kd: Ratio[U, D], ki: Ratio[U, I], kf: Ratio[U, S])

object PID {
  def proportionalControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S], target: PeriodicSignal[S], gain: Ratio[U, S]): PeriodicSignal[U] = {
    signal.zip(target).map((p, _) => (p._2 - p._1) ** gain)
  }

  def derivativeControl[S <: Quantity[S] with TimeIntegral[D],
                        U <: Quantity[U],
                        D <: Quantity[D] with TimeDerivative[S]](signal: PeriodicSignal[S],
                                                                 gain: Ratio[U, D]): PeriodicSignal[U] = {
    signal.derivative.map((d, _) => d ** gain)
  }

  def integralControl[S <: Quantity[S] with TimeDerivative[I],
                      U <: Quantity[U],
                      I <: Quantity[I] with TimeIntegral[S]](signal: PeriodicSignal[S],
                                           gain: Ratio[U, I]): PeriodicSignal[U] = {
    signal.integral.map((d, _) => d ** gain)
  }

  def feedForwardControl[S <: Quantity[S], U <: Quantity[U]](signal: PeriodicSignal[S], gain: Ratio[U, S]): PeriodicSignal[U] = {
    signal.map((v, _) => v ** gain)
  }

  def pid[S <: Quantity[S] with TimeDerivative[I] with TimeIntegral[D],
          D <: Quantity[D] with TimeDerivative[S],
          I <: Quantity[I] with TimeIntegral[S],
          U <: Quantity[U]](signal: PeriodicSignal[S], target: PeriodicSignal[S], config: PIDConfig[S, D, I, U]): PeriodicSignal[U] = {
    proportionalControl(signal, target, config.kp).
      zip(integralControl(signal, config.ki)).
      zip(derivativeControl(signal, config.kd)).
      zip(feedForwardControl(signal, config.kf)).map { case ((((p, i), d), f), _) =>
      p + i + d + f
    }
  }
}
