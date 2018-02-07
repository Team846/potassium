package com.lynbrookrobotics.potassium.config

import java.io.File

import argonaut.{DecodeJson, EncodeJson}
import argonaut.Argonaut._
import argonaut._
import ArgonautShapeless._

object TwoWayFileJSON {
  def apply[T](file: File)(implicit writer: EncodeJson[T], reader: DecodeJson[T]): TwoWaySignal[T] = {
    new TwoWayFile(file).map[T](string => string.decodeOption[T].get)(
      (_, newValue) => newValue.jencode.toString()
    )
  }
}
