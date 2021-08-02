package com.github.zhongl.webot

import cats.data.NonEmptyList

sealed trait Control

object Control {

  final private[webot] case class Complain private (error: String) extends Control
  final private[webot] case class Explore private (urls: NonEmptyList[String]) extends Control
  final private[webot] case object Repeat extends Control
  final private[webot] case object Retry extends Control

  def complain(error: String): Control = Complain(error)
  def explore(urls: NonEmptyList[String]): Control = Explore(urls)
  def repeat: Control = Repeat
  def retry: Control = Retry

  def runner(c: Compiled): String => Unit = { url =>
    @scala.annotation.tailrec
    def rec(urls: List[Option[String]]): Unit = urls match {
      case url :: rest =>
        c(url) match {
          case Left(Repeat)          => rec(Option.empty :: rest)
          case Left(Retry)           => rec(url :: rest)
          case Left(Explore(urls))   => rec(urls.map(Option(_)).toList ::: rest)
          case Left(Complain(error)) => Console.err.println(error); rec(rest)
          case Right(_)              => rec(rest)
        }

      case Nil =>
    }

    rec(List(Option(url)))
  }
}
