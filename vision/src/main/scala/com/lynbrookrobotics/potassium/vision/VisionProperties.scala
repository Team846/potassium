package com.lynbrookrobotics.potassium.vision

import squants.Angle
import squants.space.Length

case class VisionProperties(cameraAngleRelativeToFront: Angle,
                            reciprocalRootAreaToDistanceConversion: Length)
