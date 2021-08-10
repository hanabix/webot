package webot

import cats._
import cats.arrow.FunctionK
import cats.data.NonEmptyList

sealed trait Control
object Control {
  final private case class Complain(error: String)     extends Control
  final private case class Explore(urls: List[String]) extends Control
  final private case class Retry(max: Int)             extends Control
  final private case object Repeat                     extends Control

  final private case class Run(url: String, b: Branch, limit: Option[Int] = None)

  trait Dsl {
    implicit final val fromId: Id ~> List = new (Id ~> List) {
      def apply[A](fa: Id[A]): List[A] = List(fa)
    }
    implicit final val fromOption: Option ~> List = new (Option ~> List) {
      def apply[A](fa: Option[A]): List[A] = fa.toList
    }
    implicit final val fromNonEmptyList: NonEmptyList ~> List = new (NonEmptyList ~> List) {
      def apply[A](fa: NonEmptyList[A]): List[A] = fa.toList
    }
    implicit final val fromList: List ~> List = FunctionK.id

    def complain(error: String): Control                                = Complain(error)
    def explore[F[_]](urls: F[String])(implicit nt: F ~> List): Control = Explore(nt(urls))
    def repeat: Control                                                 = Repeat
    def retry(max: Int): Control                                        = Retry(max)

    implicit def engine: Engine = { fork => url =>
      @scala.annotation.tailrec
      def rec(branches: List[Run]): Unit = branches match {

        case (head @ Run(url, branch, limit)) :: tail =>
          branch() match {
            case Right(_)              => rec(tail)
            case Left(Complain(error)) => Console.err.println(error); rec(tail)
            case Left(Explore(urls))   => rec(urls.map(u => Run(u, fork(u))) ::: tail)
            case Left(Repeat)          => rec(head :: tail)
            case Left(Retry(max)) =>
              limit match {
                case Some(0) => Console.err.println(s"Exceed max retries: $max"); rec(tail)
                case Some(r) => rec(Run(url, fork(url), Option(r - 1)) :: tail)
                case None    => rec(Run(url, fork(url), Option(max - 1)) :: tail)
              }
          }

        case Nil =>
      }

      rec(List(Run(url, fork(url))))

    }

  }

}
