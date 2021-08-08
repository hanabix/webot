import cats._
import data._
import free._
import syntax.all._

import java.net._
import scala.reflect.runtime.universe._

package object webot {

  private[webot] type FunctionAK[A, F[_], G[_]] = A => F ~> G
  private[webot] type ControlOr[A]              = Either[Control, A]
  private[webot] type FExpression[A]            = Free[Expression, A]
  private[webot] type EControlOr[A]             = EitherT[Eval, Control, A]
  private[webot] type ContextCompiler[A]        = FunctionAK[Context[A], Expression, EControlOr]
  private[webot] type Branch                    = () => ControlOr[Unit]
  private[webot] type Fork                      = String => Branch
  private[webot] type Engine                    = Fork => String => Unit
  private[webot] type Runtime[A]                = ((String => Context[A]) => Unit) => Unit

  private[webot] sealed trait Descriptor
  private[webot] sealed trait Scoped                         extends Descriptor
  private[webot] final case object Self                      extends Descriptor
  private[webot] final case class Local(descriptor: String)  extends Scoped
  private[webot] final case class Global(descriptor: String) extends Scoped

  trait GlobalOps {
    def g(args: Any*): Global
  }

  object dsl extends Operator.Dsl with Expression.Dsl with Control.Dsl with As.Dsl with Open.Dsl {

    def output[F[_]: Functor, A: Monoid](fa: F[A]): Unit = { fa.map(println); () }
    def output(values: Any*): Unit = println(values.mkString(", "))

    def a(descriptor: String): Strict[Id] with Loose[Option] = a(Local(descriptor))

    def all(descriptor: String): Strict[NonEmptyList] with Loose[List] = all(Local(descriptor))

    implicit def globalSyntax(sc: StringContext): GlobalOps = new GlobalOps {
      def g(args: Any*) = {
        val strings     = sc.parts.iterator
        val expressions = args.iterator
        var buf         = new StringBuilder(strings.next())
        while (strings.hasNext) {
          buf.append(expressions.next())
          buf.append(strings.next())
        }
        Global(buf.toString())
      }
    }
  }

}
