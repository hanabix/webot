package com.github.zhongl.webot

import cats.data.NonEmptyList

sealed trait Control
object Control {

  final private[webot] case class Complain(error: String) extends Control
  final private[webot] case class Explore(urls: NonEmptyList[String]) extends Control
  final private[webot] case object Repeat extends Control
  final private[webot] case object Retry extends Control

  def complain(error: String): Control = Complain(error)
  def explore(urls: NonEmptyList[String]): Control = Explore(urls)
  def repeat: Control = Repeat
  def retry: Control = Retry

  @scala.annotation.tailrec
  final def run(urls: List[Option[String]], c: Compiled): Unit = urls match {
    case url :: rest =>
      c(url) match {
        case Left(Repeat)          => run(Option.empty :: rest, c)
        case Left(Retry)           => run(url :: rest, c)
        case Left(Explore(urls))   => run(urls.map(Option(_)).toList ::: rest, c)
        case Left(Complain(error)) => Console.err.println(error); run(rest, c)
        case Right(_)              => run(rest, c)
      }

    case Nil =>
  }

}
