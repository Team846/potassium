package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt.{BorderLayout, Dimension}
import java.io.File
import java.util
import java.util.Scanner
import javax.swing.event.ChangeEvent
import javax.swing.{JFrame, JSlider}

import com.lynbrookrobotics.potassium.units.Point
import squants.space.{Angle, Degrees, Feet, Length}
import squants.time.Seconds

class SimulatorGUI(val fieldWidth: Length,
                   val fieldHeight: Length,
                   val boxSize: Int = 50,
                   val maxSize: Dimension = new Dimension(600, 400)) {

  /**
    * This is the frame size, in pixels
    */
    val frameSize = new Dimension(
    Math.min(fieldWidth.toFeet.toInt * boxSize, maxSize.width),
    Math.min(fieldHeight.toFeet.toInt * boxSize, maxSize.height))

  /**
    * This is the Canvas (customized) which we draw with
    */
  val canvas = new Canvas(frameSize.width, frameSize.height)

  /**
    * This is the frame for our simulator
    */
  val frame = new JFrame("Simulator")
  frame.setLayout(new BorderLayout())
  frame.add(canvas)

  private val timeSlider = new JSlider()
  private val layout = new BorderLayout()
  frame.add(timeSlider)

  /**
    * This contains the data read from a log file
    */
  private var data: util.ArrayList[(Double, DataChunk)] = _

  /*****************
   * CONFIGURATION *
   *****************/
  frame.setLayout(layout)
  frame.setSize(frameSize)

  timeSlider.setMinimum(0)
  timeSlider.setValue(0)
  timeSlider.addChangeListener((e: ChangeEvent) => {
    val (_, dataChunk) = data.get(timeSlider.getValue)
    update(dataChunk.point, dataChunk.angle)
  })
  timeSlider.setToolTipText("Time")

  layout.addLayoutComponent(timeSlider, BorderLayout.SOUTH)
  layout.addLayoutComponent(canvas, BorderLayout.CENTER)

  def useLogFile(log: File, doNotLoadIntoMemory: Boolean = false): Unit = {
    val scanner = new Scanner(log)
    val times = new util.ArrayList[(Double, DataChunk)]
    scanner.nextLine()
    while (scanner.hasNextLine) {
      val line = scanner.nextLine.replace(",", "").split("\\s+")
      val dataChunk = DataChunk(Seconds(line(0).toDouble),
        Point(Feet(line(1).toDouble), Feet(line(2).toDouble)),
        Degrees(line(4).toDouble))
      times.add((dataChunk.time.toSeconds, dataChunk))
    }
    var hadData = false
    if (data != null) {
      hadData = true
    }
    data = times
    timeSlider.setMaximum(data.size)
  }

  def pointToPixels(p: Point): (Int, Int) = {
    (frameSize.width / fieldWidth.toFeet.toInt * p.x.toFeet.toInt, frameSize.height / fieldHeight.toFeet.toInt * p.y.toFeet.toInt)
  }

  def startGUI(): Unit = {
    frame.setVisible(true)
  }

  def update(p: Point, a: Angle): Unit = {
    val (x, y) = pointToPixels(p)
    canvas.setPoint(x, y)
    canvas.setAngle(a)
    canvas.repaint()
  }
}
