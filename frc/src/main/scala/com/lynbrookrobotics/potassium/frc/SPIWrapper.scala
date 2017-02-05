package com.lynbrookrobotics.potassium.frc

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPITrait
import edu.wpi.first.wpilibj.SPI

class SPIWrapper(spi: SPI) extends SPITrait {
  override def read(initiate: Boolean, dataReceived: ByteBuffer, size: Int): Int = {
<<<<<<< HEAD
    spi.read(initiate, dataReceived.array, size)
=======
    try {
      spi.read(initiate, dataReceived, size)
    }
    catch {
      case e: java.lang.UnsupportedOperationException =>
        e.printStackTrace()

        print("initiate Boolean: ", initiate, "dataReceived ByteBuffer: ", dataReceived, "size Int: ", size)
        0
    }
>>>>>>> a586bc425ba99301edc4b8d07b34aa2b157f4f04
  }

  override def setClockRate(hz: Int): Unit = {
    spi.setClockRate(hz)
  }

  override def write(byte: ByteBuffer, size: Int): Int = {
<<<<<<< HEAD
    spi.write(byte.array, size)
=======
    spi.write(byte, size)
>>>>>>> a586bc425ba99301edc4b8d07b34aa2b157f4f04
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
