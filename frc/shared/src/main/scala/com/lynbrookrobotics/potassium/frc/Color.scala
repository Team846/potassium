package com.lynbrookrobotics.potassium.frc

case class Color(red: Int, green: Int, blue: Int)

object Color {
  def rgb(red: Int, green: Int, blue: Int): Color = {
    new Color(math.min(math.max(red, 0), 255),
      math.min(math.max(green, 0), 255),
      math.min(math.max(blue, 0), 255))
  }

  def hsv(h: Double, s: Double, v: Double): Color = {
    val c  = s * v
    val h1 = (h * 360.0) / 60.0
    val x  = c * (1.0 - math.abs((h1 % 2) - 1.0))
    val (r,g,b) = if (h1 < 1.0) {
      (c, x, 0.0)
    } else if (h1 < 2.0) {
      (x, c, 0.0)
    } else if (h1 < 3.0) {
      (0.0, c, x)
    } else if (h1 < 4.0) {
      (0.0, x, c)
    } else if (h1 < 5.0) {
      (x, 0.0, c)
    } else {
      (c, 0.0, x)
    }
    val m = v - c
    Color((r + m).toInt, (g + m).toInt, (b + m).toInt)
  }
}
