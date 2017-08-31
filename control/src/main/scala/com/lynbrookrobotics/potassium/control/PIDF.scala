package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.streams._
import com.lynbrookrobotics.potassium.units._
import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}
import sun.reflect.generics.reflectiveObjects.NotImplementedException

case class PIDConfig[S <: Quantity[S],
                     SWithD <: Quantity[SWithD] with TimeIntegral[D],
                     SWithI <: Quantity[SWithI] with TimeDerivative[I],
                     D <: Quantity[D] with TimeDerivative[SWithD],
                     I <: Quantity[I] with TimeIntegral[SWithI],
                     U <: Quantity[U]](kp: Ratio[U, S], ki: Ratio[U, I], kd: Ratio[U, D]) {
  type Full = PIDFConfig[S, SWithD, SWithI, D, I, U]

  def withF(kf: Ratio[U, S]): Full = {
    PIDFConfig(kp, ki, kd, kf)
  }
}

case class PIDFConfig[S <: Quantity[S],
                      SWithD <: Quantity[SWithD] with TimeIntegral[D],
                      SWithI <: Quantity[SWithI] with TimeDerivative[I],
                      D <: Quantity[D] with TimeDerivative[SWithD],
                      I <: Quantity[I] with TimeIntegral[SWithI],
                      U <: Quantity[U]](kp: Ratio[U, S], ki: Ratio[U, I], kd: Ratio[U, D], kf: Ratio[U, S])

object PIDF {
  // TODO: Use zipAsync instead of zip as used in minus
  def proportionalControl[S <: Quantity[S], U <: Quantity[U]](current: Stream[S],
                                                              target: Stream[S], gain: Signal[Ratio[U, S]]): Stream[U] = {
    target.minus(current).map { v =>
      v ** gain.get
    }
  }

  def derivativeControl[S <: Quantity[S] with TimeIntegral[D],
                        U <: Quantity[U],
                        D <: Quantity[D] with TimeDerivative[S]](current: Stream[S],
                                                                 target: Stream[S],
                                                                 gain: Signal[Ratio[U, D]]): Stream[U] = {
    target.minus(current).derivative.map { d =>
      d ** gain.get
    }
  }

  def integralControl[S <: Quantity[S] with TimeDerivative[I],
                      U <: Quantity[U],
                      I <: Quantity[I] with TimeIntegral[S]](current: Stream[S],
                                                             target: Stream[S],
                                                             gain: Signal[Ratio[U, I]]): Stream[U] = {
    target.minus(current).integral.map(d => {
      d ** gain.get
    })
  }



  def feedForwardControl[S <: Quantity[S], U <: Quantity[U]](current: Stream[S], gain: Signal[Ratio[U, S]]): Stream[U] = {
    current.map { v =>
      v ** gain.get
    }
  }

  def pid[S <: Quantity[S],
          SWithD <: Quantity[SWithD] with TimeIntegral[D],
          SWithI <: Quantity[SWithI] with TimeDerivative[I],
          D <: Quantity[D] with TimeDerivative[SWithD],
          I <: Quantity[I] with TimeIntegral[SWithI],
          U <: Quantity[U]](current: Stream[S], target: Stream[S], config: Signal[PIDConfig[S, SWithD, SWithI, D, I, U]])
                   (implicit exD: S => SWithD, exI: S => SWithI): Stream[U] = {
    proportionalControl(current, target, config.map(_.kp)).drop(1).
      zip(integralControl(current.map(exI), target.map(exI), config.map(_.ki))).
      zip(derivativeControl(current.map(exD), target.map(exD), config.map(_.kd))).map { pid =>
      val ((p, i), d) = pid
      p + i + d
    }
  }

  def pidf[S <: Quantity[S],
           SWithD <: Quantity[SWithD] with TimeIntegral[D],
           SWithI <: Quantity[SWithI] with TimeDerivative[I],
           D <: Quantity[D] with TimeDerivative[SWithD],
           I <: Quantity[I] with TimeIntegral[SWithI],
           U <: Quantity[U]](signal: Stream[S],
                             target: Stream[S], config: Signal[PIDFConfig[S, SWithD, SWithI, D, I, U]])
                            (implicit exD: S => SWithD, exI: S => SWithI): Stream[U] = {
    proportionalControl(signal, target, config.map(_.kp)).drop(1).
      zip(integralControl(signal.map(exI), target.map(exI), config.map(_.ki))).
      zip(derivativeControl(signal.map(exD), target.map(exD), config.map(_.kd))).
      zip(feedForwardControl(target, config.map(_.kf)).drop(1)).map { pidf =>
      val (((p, i), d), f) = pidf
      p + i + d + f
    }
  }
}
