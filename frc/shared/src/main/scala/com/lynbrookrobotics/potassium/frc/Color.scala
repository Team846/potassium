package com.lynbrookrobotics.potassium.frc

case class Color(red: Int, green: Int, blue: Int)

object Color {
  /**
    * Generate an Color object based on RGB
    * If outside bounds, rounds to 255 if higher and 0 if lower
    * @param red from 0 to 255
    * @param green from 0 to 255
    * @param blue from 0 to 255
    * @return Color based on RGB
    */
  def rgb(red: Int, green: Int, blue: Int): Color = {
    new Color(math.min(math.max(red, 0), 255),
      math.min(math.max(green, 0), 255),
      math.min(math.max(blue, 0), 255))
  }

  /**
    * Generate an Color object based on HSV
    * @param h measured in degrees (0 to 1.0)
    * @param s measured in percent (0 to 1.0)
    * @param v measured in percent (0 to 1.0)
    * @return Color based on HSV
    */
  def hsv(h: Double, s: Double, v: Double): Color = {
    val c  = s * v
    val h1 = h / 60.0
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
    Color.rgb(((r + m) * 255).toInt, ((g + m) * 255).toInt, ((b + m) * 255).toInt)
  }
}
