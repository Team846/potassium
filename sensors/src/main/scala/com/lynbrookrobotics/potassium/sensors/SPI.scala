package com.lynbrookrobotics.potassium.sensors

import java.nio.ByteBuffer

trait SPI {
  def setClockRate(rate: Int): Unit
  def setMSBFirst(): Unit
  def setSampleDataOnFalling(): Unit
  def setClockActiveLow(): Unit
  def setChipSelectActiveLow(): Unit
  def write(byte: ByteBuffer, size: Int): Unit
  def read(Initiate: Boolean, dataReceived: ByteBuffer, size: Int): Unit
}
