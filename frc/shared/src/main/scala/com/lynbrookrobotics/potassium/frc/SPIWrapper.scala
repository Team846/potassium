package com.lynbrookrobotics.potassium.frc

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPITrait
import edu.wpi.first.wpilibj.SPI

class SPIWrapper(spi: SPI) extends SPITrait {
  override def read(initiate: Boolean, dataReceived: ByteBuffer, size: Int): Int = {
    spi.read(initiate, dataReceived, size)
  }

  override def setClockRate(hz: Int): Unit = {
    spi.setClockRate(hz)
  }

  override def write(byte: ByteBuffer, size: Int): Int = {
    spi.write(byte, size)
  }

  override def setMSBFirst(): Unit = {
    spi.setMSBFirst()
  }

  override def setSampleDataOnFalling(): Unit = {
    spi.setSampleDataOnFalling()
  }

  override def setClockActiveLow(): Unit = {
    spi.setClockActiveLow()
  }

  override def setChipSelectActiveLow(): Unit = {
    spi.setChipSelectActiveLow()
  }
}
