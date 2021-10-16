import cats._
import cats.data._
import cats.free._
import cats.syntax.all._

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

  sealed private[webot] trait Descriptor
  sealed private[webot] trait Scoped                         extends Descriptor
  final private[webot] case object Self                      extends Descriptor
  final private[webot] case class Local(descriptor: String)  extends Scoped
  final private[webot] case class Global(descriptor: String) extends Scoped

  trait GlobalOps {
    def g(args: Any*): Global
  }

  object dsl extends Operator.Dsl with Expression.Dsl with Control.Dsl with As.Dsl with Open.Dsl {

    def output[F[_]: Functor, A](fa: F[A]): Unit = { fa.map(println); () }
    def output(values: Any*): Unit               = println(values.mkString(", "))

    def a(descriptor: String): Strict[Id] with Loose[Option] = a(Local(descriptor))

    def all(descriptor: String): Strict[NonEmptyList] with Loose[List] = all(Local(descriptor))

    implicit def globalSyntax(sc: StringContext): GlobalOps = new GlobalOps {
      def g(args: Any*) = {
        val strings     = sc.parts.iterator
        val expressions = args.iterator
        val buf         = new StringBuilder(strings.next())
        while (strings.hasNext) {
          buf.append(expressions.next())
          buf.append(strings.next())
        }
        Global(buf.toString())
      }
    }
  }

}
