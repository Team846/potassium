package com.lynbrookrobotics.potassium.sensors

import java.nio.ByteBuffer

trait SPITrait {
  def setClockRate(hz: Int): Unit
  def setMSBFirst(): Unit
  def setSampleDataOnFalling(): Unit
  def setClockActiveLow(): Unit
  def setChipSelectActiveLow(): Unit
  def write(byte: ByteBuffer, size: Int): Int
  def read(initiate: Boolean, dataReceived: ByteBuffer, size: Int): Int
}
