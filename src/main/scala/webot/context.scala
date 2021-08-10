package webot

import cats._
import cats.data._

private[webot] trait Context[H] { self =>
  type F[A]
  type C <: Context[H]

  implicit def toId: F ~> Id
  implicit def toOption: F ~> Option
  implicit def toNonEmptyList: F ~> NonEmptyList
  implicit def toList: F ~> List

  def get[G[_]](dp: Descriptor)(implicit nt: F ~> G): ControlOr[G[C]]
  def handle: H
  def currentUrl: String
}
