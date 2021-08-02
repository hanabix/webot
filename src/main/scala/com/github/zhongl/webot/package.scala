package com.github.zhongl

import cats._
import cats.data.NonEmptyList
import cats.free._
import cats.syntax.either._
import cats.syntax.foldable._
import cats.syntax.functor._

import java.net._

package object webot {
  type ControlOr[A] = Either[Control, A]
  type Compiled = Option[String] => ControlOr[Unit]
  type Definition = PartialFunction[String, Free[Expression, Unit]]
  type Runtime = Definition => (Compiled => Unit) => Unit

  implicit final class SelectSyntax(val select: String) extends AnyVal {
    def >?>[A](op: Operator[A]): Free[Expression, Id[A]] = Free.liftF(Expression.single(select, op))
    def >*>[A](op: Operator[A]): Free[Expression, NonEmptyList[A]] = Free.liftF(Expression.multiple(select, op))
  }

  implicit final class LogicalSyntax[A](val f: Free[Expression, A]) extends AnyVal {
    def &&[B](g: A => B): Free[Expression, A] = f.mapK(new (Expression ~> Expression) {
      def apply[C](sc: Expression[C]): Expression[C] = Expression.and(sc, c => g(c.asInstanceOf[A]))
    })

    def ||[B](g: => B): Free[Expression, A] = f.mapK(new (Expression ~> Expression) {
      def apply[C](sc: Expression[C]): Expression[C] = Expression.or(sc, () => g)
    })
  }

  val attr = Operator.attr(_)
  val text = Operator.text

  def output[F[_]: Functor: Foldable, A: Monoid](fa: F[A]): Unit = fa.map(println).fold

  def output(value: String): Unit = println(value)

  def explore(urls: NonEmptyList[String]): ControlOr[Unit] = Control.explore(urls).asLeft

  def explore(url: String): ControlOr[Unit] = explore(NonEmptyList.one(url))

  def default[A](a: A): ControlOr[A] = a.asRight

  def one[A](a: A): ControlOr[NonEmptyList[A]] = NonEmptyList.one(a).asRight

  def repeat[A]: ControlOr[A] = Control.repeat.asLeft

  def open(url: String)(df: Definition)(implicit runtime: Runtime): Unit = {
    runtime(df) { Control.run(List(Option(url)), _) }
  }

}
