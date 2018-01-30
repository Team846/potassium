package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt.Dimension
import javax.swing.JFrame

import squants.space.Length

class SimulatorGUI(val fieldWidth: Length,
                   val fieldHeight: Length,
                   val boxSize: Int = 50,
                   val maxSize: Dimension = new Dimension(600, 400)) {
  def startGUI(): Unit = {
    var frameWidth: Int = Math.min(fieldWidth.toFeet.toInt * boxSize, maxSize.width)
    var frameHeight: Int = Math.min(fieldHeight.toFeet.toInt * boxSize, maxSize.height)

    val canvas = new Canvas(frameWidth, frameHeight)

    val frame = new JFrame("Simulator")
    frame.setContentPane(canvas)
    frame.setSize(new Dimension(frameWidth, frameHeight))
    frame.setVisible(true)
  }
}
