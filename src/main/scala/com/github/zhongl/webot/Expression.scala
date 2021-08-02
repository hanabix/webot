package com.github.zhongl.webot

import cats.Id
import cats.data.NonEmptyList

sealed trait Expression[-A]

object Expression {
  final private[webot] case class Single[A] private (select: String, op: Operator[A]) extends Expression[Id[A]]
  final private[webot] case class Multiple[A] private (select: String, op: Operator[A]) extends Expression[NonEmptyList[A]]
  final private[webot] case class And[A, B] private (ea: Expression[A], f: A => B) extends Expression[A]
  final private[webot] case class Or[A, B] private (ea: Expression[A], f: () => B) extends Expression[A]

  def single[A](select: String, op: Operator[A]): Expression[A] = Single(select, op)
  def multiple[A](select: String, op: Operator[A]): Expression[NonEmptyList[A]] = Multiple(select, op)
  def and[A, B](ea: Expression[A], f: A => B): Expression[A] = And(ea, f)
  def or[A, B](ea: Expression[A], f: () => B): Expression[A] = Or(ea, f)
}
