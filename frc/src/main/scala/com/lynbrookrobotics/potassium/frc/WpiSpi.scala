package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.sensors.SPITrait
import edu.wpi.first.wpilibj.SPI
import edu.wpi.first.wpilibj.SPI.Port

class WpiSpi(port: Port) extends SPI(port) with SPITrait
