package com.lynbrookrobotics.potassium.model.examples

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.plot._
import com.lynbrookrobotics.potassium.model.StateSpace
import squants.mass.Kilograms
import squants.motion.MetersPerSecondSquared
import squants.time.Milliseconds

object DoubleForce extends App {
  val NumIterations = 100

  val times     = new Array[Double](NumIterations)
  val positions = new Array[Double](NumIterations)

  val Mass = Kilograms(1) // kg
  val GravityForce = Mass * MetersPerSecondSquared(9.8) // N
  val dt = Milliseconds(5)

  val input = DenseVector(5.0, 100.0)

  val stateSpace = new StateSpace(
    DenseMatrix(
      (0.0, 1.0),
      (0.0, 0.0)
    ),
    DenseMatrix(
      (0.0, 0.0),
      (1.0, -1.0)
    )
  )

  var state = DenseVector(0D, 0D)

  for (i <- 0 until NumIterations) {
    times(i) = i * dt.toSeconds
    positions(i) = state(0)

    state = stateSpace.step(state, input, dt.toSeconds)
  }

  val f = Figure()
  val p = f.subplot(0)
  p += plot(times, positions)
}
