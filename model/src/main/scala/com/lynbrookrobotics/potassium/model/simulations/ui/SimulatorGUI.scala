package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt.Dimension
import javax.swing.JFrame

import com.lynbrookrobotics.potassium.units.Point
import squants.space.{Angle, Length}

class SimulatorGUI(val fieldWidth: Length,
                   val fieldHeight: Length,
                   val boxSize: Int = 50,
                   val maxSize: Dimension = new Dimension(600, 400)) {

  val frameWidth: Int = Math.min(fieldWidth.toFeet.toInt * boxSize, maxSize.width)
  val frameHeight: Int = Math.min(fieldHeight.toFeet.toInt * boxSize, maxSize.height)

  val canvas = new Canvas(frameWidth, frameHeight)

  val frame = new JFrame("Simulator")
  frame.setContentPane(canvas)
  frame.setSize(new Dimension(frameWidth, frameHeight))

  def pointToPixels(p: Point): (Int, Int) = {
    (frameWidth / fieldWidth.toFeet.toInt * p.x.toFeet.toInt, frameHeight / fieldHeight.toFeet.toInt * p.y.toFeet.toInt)
  }

  def startGUI(): Unit = {
    frame.setVisible(true)
  }

  def update(p: Point, a: Angle): Unit = {
    val (x, y) = pointToPixels(p)
    canvas.setPoint(x, y)
    canvas.setAngle(a)
  }
}
