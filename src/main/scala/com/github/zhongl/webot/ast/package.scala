package com.github.zhongl.webot

package object ast {

  sealed trait Node[+A]
  private case class Leaf[A](a: A) extends Node[A]
  private case class Branch[A](dark: List[Node[A]], light: List[Node[A]] = Nil) extends Node[A]

  sealed trait React[+A]
  case class Retry[A](recovery: Node[A]) extends React[A]
  case object Continue extends React[Nothing]
  case object Skip extends React[Nothing]
  case object Abort extends React[Nothing]

  type Handle[A] = Function[A, React[A]]

  def leaf[A](a: A): Node[A] = Leaf(a)

  def branch[A](nodes: Node[A]*): Node[A] = Branch(nodes.toList)

  private object Restored {
    def unapply[A](node: Node[A]): Option[Node[A]] = node match {
      case Branch(dark, light) => Some(Branch(light.reverse ::: dark, Nil))
      case _                   => throw new IllegalStateException("Never reach here")
    }
  }

  implicit class Traversal[A](val node: Node[A]) extends AnyVal {
    def traverse(implicit handle: Handle[A]): Unit = {

      @scala.annotation.tailrec
      def run(cur: Node[A], stack: List[Node[A]]): Unit = cur match {
        case Leaf(a) =>
          (handle(a), stack) match {
            case (Retry(recovery), Restored(head) :: tail) => run(recovery, head :: tail)
            case (Continue, head :: tail)                  => run(head, tail)
            case (Skip, _ :: head :: tail)                 => run(head, tail)
            case (_, _)                                    =>
          }

        case Branch(Nil, _) =>
          stack match {
            case head :: tail => run(head, tail)
            case Nil          =>
          }

        case Branch(head :: tail, light) =>
          run(head, Branch(tail, head :: light) :: stack)
      }

      run(node, Nil)
    }
  }

}
