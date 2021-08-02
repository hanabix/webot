package com.github.zhongl.webot

sealed trait Control

object Control {

  final private[webot] case class Complain private (error: String)                  extends Control
  final private[webot] case class Explore private (url: String, more: List[String]) extends Control
  final private[webot] case class Retry(max: Int)                                   extends Control
  final private[webot] case object Repeat                                           extends Control

  final private[webot] case class Context(url: Option[String], Control: Control)

  def complain(error: String): Control                        = Complain(error)
  def explore(url: String, more: List[String] = Nil): Control = Explore(url, more)
  def repeat: Control                                         = Repeat
  def retry(max: Int): Control                                = Retry(max)

  def runner(c: Compiled): String => Unit = { url =>
    @scala.annotation.tailrec
    def rec(urls: List[Context]): Unit = urls match {
      case Context(_, Explore(url, Nil)) :: rest =>
        c(Option(url)) match {
          case Left(Complain(error)) => Console.err.println(error); rec(rest)
          case Left(c)               => rec(Context(Option(url), c) :: rest)
          case Right(_)              => rec(rest)
        }

      case Context(Some(_), Explore(url, head :: tail)) :: rest =>
        c(Option(url)) match {
          case Left(Complain(error)) => Console.err.println(error); rec(rest)
          case Left(c)               => rec(Context(Option(url), c) :: rest)
          case Right(_)              => rec(Context(Option(url), Explore(head, tail)) :: rest)
        }

      case Context(Some(url), Repeat) :: rest =>
        c(None) match {
          case Left(Complain(error)) => Console.err.println(error); rec(rest)
          case Left(c)               => rec(Context(Option(url), c) :: rest)
          case Right(_)              => rec(rest)
        }

      case Context(Some(url), Retry(0)) :: rest =>
        Console.err.println(s"Exceed retring times: $url"); rec(rest)

      case Context(o @ Some(url), Retry(max)) :: rest =>
        c(o) match {
          case Left(Complain(error)) => Console.err.println(error); rec(rest)
          case Left(Retry(_))        => rec(Context(Option(url), Retry(max - 1)) :: rest)
          case Left(c)               => rec(Context(Option(url), c) :: rest)
          case Right(_)              => rec(rest)
        }

      case Context(_, _) :: rest =>
        throw new IllegalStateException("Never be here")

      case Nil =>
    }

    rec(List(Context(None, explore(url))))
  }
}
