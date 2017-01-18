package com.lynbrookrobotics.potassium.lighting

import java.io.{IOException, InputStream, OutputStream}
import java.util

import gnu.io.{CommPortIdentifier, NoSuchPortException, SerialPort}

import scala.annotation.tailrec
import scala.collection.mutable

import scala.collection.JavaConverters._

/**
  * RXTX communication protocol for communicating with serial devices.
  */
class TwoWayComm {
  protected val dataQueue = mutable.Queue[Int]()
  protected val logQueue = mutable.Queue[String]()
  protected lazy val portName = systemPort
  protected lazy val portIdentifier = CommPortIdentifier.getPortIdentifier(portName)
  var isConnected = false

  /**
    * Connect to serial device
    */
  def connect(): Boolean = {
    if (portIdentifier.isCurrentlyOwned) {
      logQueue.enqueue("Error: Port currently in use")
    } else {
      val commPort = portIdentifier.open(getClass().getName(), 2000)
      commPort match {
        case serialPort: SerialPort =>
          serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)
          val in = serialPort.getInputStream
          val out = serialPort.getOutputStream
          startThreads(in, out)
          isConnected = true
        case _ => logQueue.enqueue("Error: Comm port not a serial port")
      }
    }

    isConnected
  }

  protected def startThreads(in: InputStream, out: OutputStream): Unit = {
    new Thread(new SerialReader(in)).start()
    new Thread(new SerialWriter(out)).start()
  }

  class SerialReader(var in: InputStream) extends Runnable {
    def run {
      val buffer: Array[Byte] = new Array[Byte](1024)
      var len: Int = -1
      try {
        def helper(): Boolean = {
          len = in.read(buffer)
          len > -1
        }

        while (helper()) {
          logQueue.enqueue(new String(buffer, 0, len))
        }
      } catch {
        case e: IOException => {
          e.printStackTrace()
        }
      }
    }
  }

  class SerialWriter(var out: OutputStream) extends Runnable {
    def run {
        var c: Int = 0
        def helper():Boolean = {
          c = dataQueue.dequeue()
          c > -1
        }

        while (helper()) {
          this.out.write(c)
        }
    }
  }

  /**
    * Pushes a number to the data queue to be processed
    *
    * @param data integer to be sent
    */
  def pushDataToQueue(data: String): Unit = {
    data.foreach(s => {
      dataQueue.enqueue(s.asDigit)
    })
  }

  /**
    * Pulls data from the queue with data created by the serial device
    *
    * @return First point of data on the Queue
    */
  def pullLog: String = {
    if (logQueue.nonEmpty) {
      logQueue.dequeue
    }else {
      "No data to show"
    }
  }

  private val PortNames = List[String](
    "/dev/tty.usbserial-A9007UX1",
    "/dev/ttyACM0",
    "/dev/ttyUSB0",
    "COM3");

  /**
    * Finds out the port of your system
    *
    * @return the port to use in connect
    */
  def systemPort: String = {
    val portEnum = CommPortIdentifier.getPortIdentifiers.asScala
    portEnum.map(_.asInstanceOf[CommPortIdentifier].getName).
      find(PortNames.contains).getOrElse(throw new NoSuchElementException)
  }

  @tailrec
  final def dumpLog: Unit = {
    if (logQueue.nonEmpty) {
      logQueue.dequeue()
      dumpLog
    }
  }
}
