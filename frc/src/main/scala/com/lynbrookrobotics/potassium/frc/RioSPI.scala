package com.lynbrookrobotics.potassium.frc

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPI
import edu.wpi.first.wpilibj.{SPI => wpiSPI}

class RioSPI(spi: wpiSPI) extends SPI {
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
