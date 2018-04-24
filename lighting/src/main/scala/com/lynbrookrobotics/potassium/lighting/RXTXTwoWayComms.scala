package com.lynbrookrobotics.potassium.lighting

import java.io.{IOException, InputStream, OutputStream}

import gnu.io.{CommPortIdentifier, SerialPort}

import scala.collection.mutable

import scala.collection.JavaConverters._

/**
  * RXTX communication protocol for communicating with serial devices.
  */

class RXTXTwoWayComms extends TwoWayComm{
  protected var data: Int = -1
  protected val logQueue: mutable.Queue[String] = mutable.Queue[String]()
  protected var connected = false

  def isConnected: Boolean = connected

  def hasLog: Boolean = logQueue.nonEmpty

  def clearLog(): Unit = {
    while(logQueue.nonEmpty) {
      logQueue.dequeue()
    }
  }

  def pullLog: String = {
    if(logQueue.nonEmpty && data != -1) {
      logQueue.dequeue()
    } else {
      "No data to show"
    }
  }

  def newData(int: Int): Unit = data = int

  class SerialReader(var in: InputStream) extends Runnable {
    def run(): Unit = {
      val buffer = new Array[Byte](1024)
      var len = -1
      try{
        def helper: Boolean = {
          len = in.read(buffer)
          len > -1
        }
        while(helper) {
          val log = new String(buffer, 0, len)
          logQueue.enqueue(log)
        }
      }
      catch {
        case e: IOException => e.printStackTrace()

      }
    }
  }

  class SerialWriter(var out: OutputStream) extends Runnable {
    def run(): Unit = {
      try {
        while (true) {
          out.write(data)
        }
      }
      catch {
        case e: IOException => e.printStackTrace()
      }
    }
  }

  override def connect(): Unit = connect(systemPort)

  def connect(portName: String): Unit = {
    logQueue.enqueue("Connecting on port " + portName + "\n")
    val portIdentifier = CommPortIdentifier.getPortIdentifier(portName)
    if (portIdentifier.isCurrentlyOwned) {
      logQueue.enqueue("Error: Port is currently in use\n")
    }
    else {
      val commPort = portIdentifier.open(this.getClass.getName, 2000)
      commPort match {
        case serialPort: SerialPort =>
          serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)
          val in = serialPort.getInputStream
          val out = serialPort.getOutputStream
          data = 0
          new Thread(new SerialReader(in)).start()
          new Thread(new SerialWriter(out)).start()
          logQueue.enqueue("Successfully connected on port " + portName + "\n")
          connected = true
        case _ => logQueue.enqueue("Error: Not an instance of serial port\n")
      }
    }
  }

  private val PortNames = List[String](
    "/dev/tty.usbserial-A9007UX1",
    "/dev/ttyACM0",
    "/dev/ttyUSB0",
    "COM3")

  def systemPort: String = {
    val portEnum = CommPortIdentifier.getPortIdentifiers.asScala
    portEnum.map(_.asInstanceOf[CommPortIdentifier].getName).
      find(PortNames.contains).getOrElse(throw new NoSuchElementException("No ports found"))
  }
}
