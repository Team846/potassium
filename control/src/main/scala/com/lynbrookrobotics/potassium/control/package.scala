package com.lynbrookrobotics.potassium

import squants.Quantity
import squants.time.{TimeDerivative, TimeIntegral}

package object control {
  type PIDFProperUnitsConfig[S <: Quantity[S] with TimeIntegral[D] with TimeDerivative[I],
                             D <: Quantity[D] with TimeDerivative[S],
                             I <: Quantity[I] with TimeIntegral[S],
                             U <: Quantity[U]] = control.PIDFConfig[S, S, S, D, I, U]
}
