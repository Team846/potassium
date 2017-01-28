package com.lynbrookrobotics.potassium.config

import squants.{Dimension, Quantity}

import upickle.Js.Value
import upickle.default.{Reader, Writer}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object SquantsPickling {
  implicit def quantityWriter[Q <: Quantity[Q]]: Writer[Q] = new Writer[Q] {
    override def write0: (Q) => Value = {
      (q: Q) => upickle.default.writeJs((q.value, q.unit.symbol))
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
            val unit = dimension.units.find(_.symbol == uom).get
            unit(value)
        }
      }
      """
    )
  }

  implicit def quantityReader[Q <: Quantity[Q]]: Reader[Q] = macro quantityReader_impl[Q]
}
