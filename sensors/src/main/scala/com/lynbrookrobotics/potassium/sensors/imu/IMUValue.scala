package com.lynbrookrobotics.potassium.sensors.imu

import squants.Acceleration
import squants.electro.MagneticFlux
import squants.motion.AngularVelocity

/**
  * Constructs a single datapoint from the IMU.
  */
case class IMUValue(gyro: Value3D[AngularVelocity], accel: Value3D[Acceleration], magneto: Value3D[MagneticFlux]) {}
