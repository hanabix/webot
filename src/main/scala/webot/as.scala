package webot

import java.net.URL
import cats._
import free.Free
import syntax.functor._
import scala.collection.StringOps

trait As[A] {
  def from(value: String): A
}
object As {
  def apply[A](implicit ins: As[A]): As[A] = ins

  implicit val asBoolean = new As[Boolean] {
    def from(value: String) = value.toBoolean
  }
  implicit val asByte = new As[Byte] {
    def from(value: String) = value.toByte
  }
  implicit val asInt = new As[Int] {
    def from(value: String) = value.toInt
  }
  implicit val asDouble = new As[Double] {
    def from(value: String) = value.toDouble
  }

  trait Dsl {
    trait AsOps[F[_], G[_]] {

      /** Convert a string to type `B`.
        *
        * @param f
        * @return
        */
      def as[A: As]: Free[F, G[A]]
    }

    implicit def asSyntax[F[_], G[_]: Functor](fa: Free[F, G[String]]): AsOps[F, G] = new AsOps[F, G] {
      def as[A: As] = fa.map(_.map(As[A].from))
    }
  }

}
