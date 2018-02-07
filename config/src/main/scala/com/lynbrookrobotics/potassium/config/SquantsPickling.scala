package com.lynbrookrobotics.potassium.config

import argonaut.Argonaut._
import argonaut._
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral}
import squants.motion.Velocity
import squants.{Dimension, Quantity}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object SquantsPickling {
  implicit def quantityWriter[Q <: Quantity[Q]]: EncodeJson[Q] = (q: Q) => {
    (q.value, q.unit.getClass.getSimpleName).jencode
  }

  def dimension_impl[Q <: Quantity[Q] : c.WeakTypeTag](c: blackbox.Context): c.Expr[Dimension[Q]] = {
    import c.universe._

    c.Expr[Dimension[Q]](q"${weakTypeTag[Q].tpe.typeSymbol.companion}")
  }

  def quantityReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
  (c: blackbox.Context): c.Expr[DecodeJson[Q]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[DecodeJson[Q]](
      q"""
      new argonaut.DecodeJson[$qExpr] {
        override def decode(c: argonaut.HCursor): DecodeResult[$qExpr] = {
          val dimension = $dimension
          val (value, uom) = c.jdecode[(Double, String)].getOr(null)
          val unit = dimension.units.find(_.getClass.getSimpleName == uom).getOrElse(throw new Exception(uom))
          argonaut.DecodeResult.ok(unit(value))
        }
      }
      """
    )
  }

  implicit def quantityReader[Q <: Quantity[Q]]: DecodeJson[Q] = macro quantityReader_impl[Q]

  implicit def genericDerivativeWriter[Q <: Quantity[Q]]: EncodeJson[GenericDerivative[Q]] =
    (q: GenericDerivative[Q]) => (q.value, q.uom.getClass.getSimpleName + " / s").jencode

  def genericDerivativeReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
  (c: blackbox.Context): c.Expr[DecodeJson[GenericDerivative[Q]]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[DecodeJson[GenericDerivative[Q]]](
      q"""
      new upickle.default.Reader[com.lynbrookrobotics.potassium.units.GenericDerivative[$qExpr]] {
        override def read0: PartialFunction[upickle.Js.Value, com.lynbrookrobotics.potassium.units.GenericDerivative[$qExpr]] = {
          case v: upickle.Js.Value =>
            val dimension = $dimension
            val (value, uom) = upickle.default.readJs[(Double, String)](v)
            val unit = dimension.units.find(_.getClass.getSimpleName == uom.dropRight(4)).getOrElse(throw new Exception(uom))
            new com.lynbrookrobotics.potassium.units.GenericDerivative(value, unit)
        }
      }
      """
    )
  }

  implicit def genericDerivativeReader[Q <: Quantity[Q]]: DecodeJson[GenericDerivative[Q]] =
  macro genericDerivativeReader_impl[Q]

  implicit def genericIntegralWriter[Q <: Quantity[Q]]: EncodeJson[GenericIntegral[Q]] =
    (q: GenericIntegral[Q]) => (q.value, q.uom.getClass.getSimpleName + " * s").jencode

  def genericIntegralReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
  (c: blackbox.Context): c.Expr[DecodeJson[GenericIntegral[Q]]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[DecodeJson[GenericIntegral[Q]]](
      q"""
      new upickle.default.Reader[com.lynbrookrobotics.potassium.units.GenericIntegral[$qExpr]] {
        override def read0: PartialFunction[upickle.Js.Value, com.lynbrookrobotics.potassium.units.GenericIntegral[$qExpr]] = {
          case v: upickle.Js.Value =>
            val dimension = $dimension
            val (value, uom) = upickle.default.readJs[(Double, String)](v)
            val unit = dimension.units.find(_.getClass.getSimpleName == uom.dropRight(4)).getOrElse(throw new Exception(uom))
            new com.lynbrookrobotics.potassium.units.GenericIntegral(value, unit)
        }
      }
      """
    )
  }

  implicit def genericIntegralReader[Q <: Quantity[Q]]: DecodeJson[GenericIntegral[Q]] =
  macro genericIntegralReader_impl[Q]
}
