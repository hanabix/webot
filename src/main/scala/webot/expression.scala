package webot

import cats._
import cats.data._
import cats.free._
import cats.syntax.all._

sealed trait Expression[+A]
object Expression {

  final private case class GetAFrom[F[_], A](d: Descriptor, expression: F[A])             extends Expression[A]
  final private case class GetOptionAFrom[F[_], A](d: Descriptor, expression: F[A])       extends Expression[Option[A]]
  final private case class GetNonEmptyListAFrom[F[_], A](d: Descriptor, expression: F[A]) extends Expression[NonEmptyList[A]]
  final private case class GetListAFrom[F[_], A](d: Descriptor, expression: F[A])         extends Expression[List[A]]
  final private case class Go(c: Control)                                                 extends Expression[Unit]

  final def getAFrom[F[_], A](d: Descriptor, expression: F[A]): Expression[A]                           = GetAFrom(d, expression)
  final def getOptionAFrom[F[_], A](d: Descriptor, expression: F[A]): Expression[Option[A]]             = GetOptionAFrom(d, expression)
  final def getNonEmptyListAFrom[F[_], A](d: Descriptor, expression: F[A]): Expression[NonEmptyList[A]] = GetNonEmptyListAFrom(d, expression)
  final def getListAFrom[F[_], A](d: Descriptor, expression: F[A]): Expression[List[A]]                 = GetListAFrom(d, expression)

  trait Dsl extends Element.Dsl {

    implicit def ctxCompiler[E: Element]: ContextCompiler[E] = ctx =>
      new (Expression ~> EControlOr) {
        import ctx._

        private val EControlOr = EitherT

        def apply[A](fa: Expression[A]): EControlOr[A] = fa match {
          case GetAFrom(d, op: Operator[A] @unchecked)                => EControlOr(eval[Id, A](d, op))
          case GetOptionAFrom(d, op: Operator[A] @unchecked)          => EControlOr(eval[Option, A](d, op))
          case GetAFrom(d, fe: FExpression[A] @unchecked)             => EControlOr(eval[Id, A](d, fe))
          case GetOptionAFrom(d, fe: FExpression[A] @unchecked)       => EControlOr(eval[Option, A](d, fe))
          case GetNonEmptyListAFrom(d, op: Operator[A] @unchecked)    => EControlOr(eval[NonEmptyList, A](d, op))
          case GetListAFrom(d, op: Operator[A] @unchecked)            => EControlOr(eval[List, A](d, op))
          case GetNonEmptyListAFrom(d, fe: FExpression[A] @unchecked) => EControlOr(eval[NonEmptyList, A](d, fe))
          case GetListAFrom(d, fe: FExpression[A] @unchecked)         => EControlOr(eval[List, A](d, fe))
          case Go(c)                                                  => EControlOr(Eval.now(c.asLeft))
          case _                                                      => throw new UnsupportedOperationException("Never be here")
        }

        private def eval[F[_]: Traverse, A](d: Descriptor, fe: FExpression[A])(implicit nt: ctx.F ~> F): Eval[ControlOr[F[A]]] = {
          ctx.get[F](d).map(_.map(c => Eval.defer(fe.foldMap(ctxCompiler(Element[E])(c)).value)).sequence.map(_.sequence)).sequence.map(_.flatten)
        }

        private def eval[F[_]: Traverse, A](d: Descriptor, op: Operator[A])(implicit nt: ctx.F ~> F): Eval[ControlOr[F[A]]] = Eval.now {
          ctx.get[F](d).flatMap(_.map(c => c.handle(op)).sequence)
        }

      }

    trait Loose[L[_]] {
      def get_if_present[F[_], A](expression: F[A]): FExpression[L[A]]
      def apply_if_present[F[_]](expression: F[Unit]): FExpression[L[Unit]]
    }

    trait Strict[S[_]] {
      def get[F[_], A](expression: F[A]): FExpression[S[A]]
      def apply[F[_]](expression: F[Unit]): FExpression[S[Unit]]
    }

    /** Get one element by [[Scoped]] descriptor in current [[Context]].
      *
      * {{{ a(g"a.link") }}}
      *
      * Get one element tag named `a` and decorated by class `link` in global search context.
      *
      * @param descriptor
      * @return
      */
    final def a(descriptor: Scoped): Strict[Id] with Loose[Option] = new Strict[Id] with Loose[Option] {
      def get[F[_], A](expression: F[A])              = Free.liftF(getAFrom(descriptor, expression))
      def get_if_present[F[_], A](expression: F[A])   = Free.liftF(getOptionAFrom(descriptor, expression))
      def apply[F[_]](expression: F[Unit])            = Free.liftF(getAFrom(descriptor, expression))
      def apply_if_present[F[_]](expression: F[Unit]) = Free.liftF(getOptionAFrom(descriptor, expression))
    }

    /** Get all elements by [[Scoped]] descriptor in current [[Context]].
      *
      * {{ all(g"a.link") }}
      *
      * Get all elements tag named `a` and decorated by class `link` in global search context.
      *
      * @param descriptor
      * @return
      */
    final def all(descriptor: Scoped): Strict[NonEmptyList] with Loose[List] = new Strict[NonEmptyList] with Loose[List] {
      def get[F[_], A](expression: F[A])              = Free.liftF(getNonEmptyListAFrom(descriptor, expression))
      def get_if_present[F[_], A](expression: F[A])   = Free.liftF(getListAFrom(descriptor, expression))
      def apply[F[_]](expression: F[Unit])            = Free.liftF(getNonEmptyListAFrom(descriptor, expression))
      def apply_if_present[F[_]](expression: F[Unit]) = Free.liftF(getListAFrom(descriptor, expression))
    }

    /** Get the element in current [[Context]].
      *
      * {{{
      * all("a.link") get {
      *   for {
      *     _ <- it apply hover
      *     n <- a("span.popup") get text
      *   } yield
      * }
      * }}}
      *
      * @return
      */
    final def it: Strict[Id] = new Strict[Id] {
      def get[F[_], A](expression: F[A])   = Free.liftF(getAFrom(Self, expression))
      def apply[F[_]](expression: F[Unit]) = Free.liftF(getAFrom(Self, expression))
    }
  }

}
