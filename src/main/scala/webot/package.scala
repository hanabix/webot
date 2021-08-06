import cats._
import data._
import free._
import syntax.all._

import java.net._
import scala.reflect.runtime.universe._

package object webot extends OperatorDSL with ExpressionDSL with AsInstances {

  type ControlOr[A]         = Either[Control, A]
  type Compiled             = Option[URL] => ControlOr[Unit]
  type ParticalProcedure[A] = PartialFunction[URL, Procedure]
  type Runtime[A]           = ParticalProcedure[A] => (Compiled => Unit) => Unit

  final class Open[A](url: URL) {
    def apply(proc: Procedure)(implicit rt: Runtime[A]): Unit             = apply { case _ => proc }
    def apply(pproc: ParticalProcedure[A])(implicit rt: Runtime[A]): Unit = rt(pproc) { c => Control.runner(c)(url) }
  }

  def open[A](url: String): Open[A] = new Open(new URL(url))

  def output[F[_]: Functor: Foldable, A: Monoid](fa: F[A]): Unit = fa.map(println).fold
  def output(values: Any*): Unit                                 = println(values.mkString(", "))

  implicit def asSyntax[F[_], G[_]: Functor](ffa: Free[F, G[String]]) = new AsOps[F, G](ffa)
  implicit def globalLocatorSyntax(sc: StringContext): LocatorOps     = new LocatorOps(sc)
}
