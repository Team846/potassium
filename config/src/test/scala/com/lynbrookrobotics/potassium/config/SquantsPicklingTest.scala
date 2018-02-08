package com.lynbrookrobotics.potassium.config

import org.scalatest.FunSuite
import argonaut.Argonaut._
import argonaut._
import ArgonautShapeless._
import SquantsPickling._
import com.lynbrookrobotics.potassium.units.GenericIntegral
import com.lynbrookrobotics.potassium.units.GenericDerivative
import squants.motion._
import com.lynbrookrobotics.potassium.units.GenericValue._

import scala.language.experimental.macros
import squants.space._
import squants.time.{Milliseconds, Minutes}

class SquantsPicklingTest extends FunSuite {
  test("Basic squants values can be encoded and decoded") {
    val a = FeetPerSecond(3.14)
    val aJs = "[3.14,\"FeetPerSecond\"]"
    assert(a.jencode.toString() == aJs)
    assert(aJs.decodeOption[Velocity].get == a)

    val b = DegreesPerSecondSquared(123.4)
    val bJs = "[123.4,\"DegreesPerSecondSquared\"]"
    assert(b.jencode.toString() == bJs)
    assert(bJs.decodeOption[AngularAcceleration].get == b)
  }

  test("Generic integrals can be encoded and decoded") {
    val a = Feet(3.14) * Minutes(2)
    val aJs = "[376.8,\"Feet * s\"]"
    assert(a.jencode.toString() == aJs)
    assert(aJs.decodeOption[GenericIntegral[Length]].get == a)

    val b = Degrees(31.4) * Milliseconds(2)
    val bJs = "[0.0628,\"Degrees * s\"]"
    assert(b.jencode.toString() == bJs)
    assert(bJs.decodeOption[GenericIntegral[Angle]].get == b)
  }

  test("Generic derivatives can be encoded and decoded") {
    val a = FeetPerSecondCubed(12.3) / Minutes(5)
    val aJs = "[0.041,\"FeetPerSecondCubed / s\"]"
    assert(a.jencode.toString() == aJs)
    assert(aJs.decodeOption[GenericDerivative[Jerk]].get == a)

    val b = DegreesPerSecondSquared(31.141) / Milliseconds(2)
    assert(b.jencode.toString() == "[15570.5,\"DegreesPerSecondSquared / s\"]")
    assert("[15570.5,\"DegreesPerSecondSquared / s\"]".decodeOption[GenericDerivative[AngularAcceleration]].get == b)
  }
}
