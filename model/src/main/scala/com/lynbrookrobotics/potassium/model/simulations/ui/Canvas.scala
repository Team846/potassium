package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt._
import javax.swing.JPanel

import squants.Angle

class Canvas(val w: Int, val h: Int) extends JPanel {

  var xPos: Int = _
  var yPos: Int = _
  var angle: Int = _

  var positionChanged = false
  var angleChanged = false

  def setPoint(x: Int, y: Int): Unit = {
    this.xPos = x
    this.yPos = y
    positionChanged = true
  }

  def setAngle(a: Angle): Unit = {
    angle = a.toDegrees.toInt
    angleChanged = true
  }

  def rotatePointByAngleAroundOrigin(x: Int, y: Int, a: Int): Tuple2[Int, Int] = {
    val sin = math.sin(a)
    val cos = math.cos(a)

    ((x * cos - y * sin).toInt, (x * sin + y * cos).toInt)
  }



  override def paintComponent(g: Graphics): Unit = {
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, w, h)


    if (positionChanged && angleChanged) {
      g.setColor(Color.BLACK)
      val (x1, y1) = (xPos, yPos)
      val point2 = rotatePointByAngleAroundOrigin(-5, 10, angle)
      val (x2, y2) = (point2._1 + xPos, point2._2 + yPos)
      val point3 = rotatePointByAngleAroundOrigin(5, 10, angle)
      val (x3, y3) = (point3._1 + xPos, point3._2 + yPos)
      val xValues = Array(x1, x2, x3)
      val yValues = Array(y1, y2, y3)
      g.fillPolygon(xValues, yValues, 3)
      println(x1, x2, x3)
      println(y1, y2, y3)

    }
  }

  override def getPreferredSize: Dimension = new Dimension(w, h)
}
