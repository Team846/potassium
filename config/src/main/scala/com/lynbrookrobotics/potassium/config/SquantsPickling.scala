package com.lynbrookrobotics.potassium.config

import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral}
import squants.{Dimension, Quantity}
import upickle.Js.Value
import upickle.default.{Reader, Writer}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object SquantsPickling {
  implicit def quantityWriter[Q <: Quantity[Q]]: Writer[Q] = new Writer[Q] {
    override def write0: (Q) => Value = {
      (q: Q) => upickle.default.writeJs((q.value, q.unit.getClass.getSimpleName))
    }
  }

  def dimension_impl[Q <: Quantity[Q] : c.WeakTypeTag](c: blackbox.Context): c.Expr[Dimension[Q]] = {
    import c.universe._

    c.Expr[Dimension[Q]](q"${weakTypeTag[Q].tpe.typeSymbol.companion}")
  }

  def quantityReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
    (c: blackbox.Context): c.Expr[Reader[Q]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[Reader[Q]](
      q"""
      new upickle.default.Reader[$qExpr] {
        override def read0: PartialFunction[upickle.Js.Value, $qExpr] = {
          case v: upickle.Js.Value =>
            val dimension = $dimension
            val (value, uom) = upickle.default.readJs[(Double, String)](v)
            val unit = dimension.units.find(_.getClass.getSimpleName == uom).getOrElse(throw new Exception(uom))
            unit(value)
        }
      }
      """
    )
  }

  implicit def quantityReader[Q <: Quantity[Q]]: Reader[Q] = macro quantityReader_impl[Q]

  implicit def genericDerivativeWriter[Q <: Quantity[Q]]: Writer[GenericDerivative[Q]] = new Writer[GenericDerivative[Q]] {
    override def write0: (GenericDerivative[Q]) => Value = {
      (q: GenericDerivative[Q]) => upickle.default.writeJs((q.value, q.uom.getClass.getSimpleName + " / s"))
    }
  }

  def genericDerivativeReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
    (c: blackbox.Context): c.Expr[Reader[GenericDerivative[Q]]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[Reader[GenericDerivative[Q]]](
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

  implicit def genericDerivativeReader[Q <: Quantity[Q]]: Reader[GenericDerivative[Q]] =
    macro genericDerivativeReader_impl[Q]

  implicit def genericIntegralWriter[Q <: Quantity[Q]]: Writer[GenericIntegral[Q]] = new Writer[GenericIntegral[Q]] {
    override def write0: (GenericIntegral[Q]) => Value = {
      (q: GenericIntegral[Q]) => upickle.default.writeJs((q.value, q.uom.getClass.getSimpleName + " * s"))
    }
  }

  def genericIntegralReader_impl[Q <: Quantity[Q] : c.WeakTypeTag]
    (c: blackbox.Context): c.Expr[Reader[GenericIntegral[Q]]] = {
    import c.universe._

    val dimension = dimension_impl[Q](c)
    val qExpr = weakTypeTag[Q].tpe.typeSymbol

    c.Expr[Reader[GenericIntegral[Q]]](
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

  implicit def genericIntegralReader[Q <: Quantity[Q]]: Reader[GenericIntegral[Q]] =
    macro genericIntegralReader_impl[Q]
}
