package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt.{Color, Dimension, Graphics}
import javax.swing.JPanel

import squants.Angle

class Canvas(val w: Int, val h: Int) extends JPanel {

  var x: Int = _
  var y: Int = _
  var angle: Int = _

  var positionChanged = false
  var angleChanged = false

  def setPoint(x: Int, y: Int): Unit = {
    this.x = x
    this.y = y
    positionChanged = true
  }

  def setAngle(a: Angle): Unit = {
    angle = a.toDegrees.toInt
    angleChanged = true
  }

  def rotatePointByAngleAroundOrigin(x: Int, y: Int, a: Int): (Double, Double) = {
    val sin = math.sin(a)
    val cos = math.cos(a)

    (x * cos - y * sin, x * sin + y * cos)
  }

  override def paintComponent(g: Graphics): Unit = {
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, w, h)

    if (positionChanged && angleChanged) {
      g.setColor(Color.BLACK)
      val (x1, y1) = rotatePointByAngleAroundOrigin(x, y, angle)
      val (x2, y2) = rotatePointByAngleAroundOrigin(x - 5, y + 10, angle)
      val (x3, y3) = rotatePointByAngleAroundOrigin(x + 5, y + 10, angle)
      val xValues = Array(x1, x2, x3)
      val yValues = Array(y1, y2, y3)
      g.fillPolygon(xValues, yValues, 3)
    }
  }

  override def getPreferredSize: Dimension = new Dimension(w, h)
}
