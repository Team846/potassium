package com.lynbrookrobotics.potassium.model.simulations.ui

import java.awt.{BorderLayout, Color, Dimension}
import java.io.File
import java.util
import java.util.Scanner
import javax.swing.event.{ChangeEvent, ChangeListener}
import javax.swing.{JFrame, JPanel, JSlider}

import com.lynbrookrobotics.potassium.units.Point
import squants.space.{Angle, Degrees, Feet, Length}
import squants.time.Seconds

import scala.collection.mutable

class SimulatorGUI(val fieldWidth: Length,
                   val fieldHeight: Length,
                   val boxSize: Int = 50,
                   val maxSize: Dimension = new Dimension(600, 400)) {

  var slider: JSlider = _

  var data: util.ArrayList[(Double, DataChunk)] = _


  val frameWidth: Int = Math.min(fieldWidth.toFeet.toInt * boxSize, maxSize.width)
  val frameHeight: Int = Math.min(fieldHeight.toFeet.toInt * boxSize, maxSize.height)

  val canvas = new Canvas(frameWidth, frameHeight)

  val container = new JPanel()
  container.setLayout(new BorderLayout())
  container.add(canvas)

  val frame = new JFrame("Simulator")
  frame.setContentPane(container)
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
    canvas.repaint()
  }

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
    data = times
    var hadSlider = false
    slider = if (slider == null) new JSlider() else {
      hadSlider = true
      slider
    }
    slider.setMinimum(0)
    slider.setMaximum(data.size)
    slider.setValue(0)
    if (!hadSlider) {
      container.add(slider)
      slider.addChangeListener(new ChangeListener {
        override def stateChanged(e: ChangeEvent): Unit = {
          println(data.size)
          val (_, dataChunk) = data.get(slider.getValue)
          update(dataChunk.point, dataChunk.angle)
        }
      })

    }
  }
}
