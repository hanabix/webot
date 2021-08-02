package com.github.zhongl.webot

import cats.Id
import cats.data.NonEmptyList

sealed trait Expression[-A]

object Expression {
  final case class Single[A](select: String, op: Operator[A]) extends Expression[Id[A]]
  final case class Multiple[A](select: String, op: Operator[A]) extends Expression[NonEmptyList[A]]
  final case class And[A, B](ea: Expression[A], f: A => B) extends Expression[A]
  final case class Or[A, B](ea: Expression[A], f: () => B) extends Expression[A]

  def single[A](select: String, op: Operator[A]): Expression[A] = Single(select, op)
  def multiple[A](select: String, op: Operator[A]): Expression[NonEmptyList[A]] = Multiple(select, op)
  def and[A, B](ea: Expression[A], f: A => B): Expression[A] = And(ea, f)
  def or[A, B](ea: Expression[A], f: () => B): Expression[A] = Or(ea, f)
}
