package com.lynbrookrobotics.potassium.model

import breeze.linalg.{DenseMatrix, DenseVector}

class StateSpace(A: DenseMatrix[Double], B: DenseMatrix[Double]) {
  def step(state: DenseVector[Double], input: DenseVector[Double], dt: Double): DenseVector[Double] = {
    val xdot = (A * state) + (B * input)

    state + (xdot * dt)
  }
}
