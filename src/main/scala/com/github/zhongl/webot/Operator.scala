package com.github.zhongl.webot

sealed trait Operator[+A]
object Operator {

  final private[webot] case class Input private (value: String)    extends Operator[Unit]
  final private[webot] case class Attribute private (name: String) extends Operator[String]
  final private[webot] case object Text                            extends Operator[String]

  def input(value: String): Operator[Unit] = Input(value)
  def attr(name: String): Operator[String] = Attribute(name)
  def text: Operator[String]               = Text

}
