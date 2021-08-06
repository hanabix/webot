package webot

import java.net.URL
import cats._
import free.Free
import syntax.functor._
import scala.collection.StringOps

private[webot] final class AsOps[F[_], G[_]: Functor](val ffa: Free[F, G[String]]) {

  /** Convert a string to type `B`.
    *
    * @param f
    * @return
    */
  def as[A: As]: Free[F, G[A]] = ffa.map(_.map(As[A].from))
}

trait As[A] {
  def from(value: String): A
}

object As {
  def apply[A](implicit ins: As[A]): As[A] = ins
}

private[webot] trait AsInstances {
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
  implicit val asURL = new As[URL] {
    def from(value: String) = new URL(value)
  }
}
