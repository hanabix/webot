package webot

trait Element[A] extends FunctionAK[A, Operator, ControlOr]
object Element {
  def apply[A](implicit e: Element[A]) = e

  trait Dsl {

    trait Ops[E] {
      def self: E
      def instance: Element[E]
      def apply[A](op: Operator[A]): ControlOr[A] = instance.apply(self).apply(op)
    }

    implicit def asElementOps[A](a: A)(implicit e: Element[A]) = new Ops[A] {
      val self     = a
      val instance = e
    }
  }
}
