import cats._
import data._
import free._
import syntax.all._

import java.net._
import scala.reflect.runtime.universe._

package object webot {
  type ControlOr[A]         = Either[Control, A]
  type Compiled             = Option[URL] => ControlOr[Unit]
  type Expression[A]        = Free[ExpressionA, A]
  type Procedure            = Free[ExpressionA, Unit]
  type ParticalProcedure[A] = PartialFunction[URL, Procedure]
  type Runtime[A]           = ParticalProcedure[A] => (Compiled => Unit) => Unit

  final class Open[A](url: URL) {
    def apply(proc: Procedure)(implicit rt: Runtime[A]): Unit             = apply { case _ => proc }
    def apply(pproc: ParticalProcedure[A])(implicit rt: Runtime[A]): Unit = rt(pproc) { c => Control.runner(c)(url) }
  }

  sealed trait Operator[+A]
  final private[webot] case class Attribute(name: String) extends Operator[String]
  final private[webot] case object Text                   extends Operator[String]
  final private[webot] case class Input(value: String)    extends Operator[Unit]
  final private[webot] case object Hover                  extends Operator[Unit]

  sealed trait Locator
  trait HasDescription {
    def description: String
  }
  final private[webot] case object Self                       extends Locator
  final private[webot] case class Local(description: String)  extends Locator with HasDescription
  final private[webot] case class Global(description: String) extends Locator with HasDescription

  sealed trait ExpressionA[A]
  final private[webot] case class SubjectGet[F[_], G[_], A](locator: Locator, expr: F[A], tp: Type)   extends ExpressionA[G[A]]
  final private[webot] case class SubjectApply[F[_], G[_]](locator: Locator, proc: F[Unit], tp: Type) extends ExpressionA[Unit]

  final class Subject[S[_], L[_], S_ >: S[_]: TypeTag, L_ >: L[_]: TypeTag](locator: Locator) {
    def apply[H[_]](proc: H[Unit]): Procedure                 = Free.liftF[ExpressionA, Unit](SubjectApply(locator, proc, typeOf[S_]))
    def apply_if_present[H[_]](proc: H[Unit]): Procedure      = Free.liftF[ExpressionA, Unit](SubjectApply(locator, proc, typeOf[L_]))
    def get[H[_], A](expr: H[A]): Expression[S[A]]            = Free.liftF[ExpressionA, S[A]](SubjectGet(locator, expr, typeOf[S_]))
    def get_if_present[H[_], A](expr: H[A]): Expression[L[A]] = Free.liftF[ExpressionA, L[A]](SubjectGet(locator, expr, typeOf[L_]))
  }

  def open[A](url: String): Open[A] = new Open(new URL(url))

  /** Get a [[Subject]] contains one element Subject by descriptor.
    *
    * {{{ a("a.link") }}}
    *
    * Get element tag name was `a` and decorated by class `link`.
    *
    * @param descriptor
    * @return
    */
  def a(description: String): Subject[Id, Option, Id[_], Option[_]] = new Subject(Local(description))

  // def a(locator: Locator with HasDescription): Subject[Id, Option, Id[_], Option[_]] = new Subject(locator)

  /** Get a [[Subject]] contains all elements by descriptor.
    *
    * {{{ a("a.link") }}}
    *
    * Get element tag name was `a` and decorated by class `link`.
    *
    * @param descriptor
    * @return
    */
  def all(description: String): Subject[NonEmptyList, List, NonEmptyList[_], List[_]] = new Subject(Local(description))

  // def all(locator: Locator with HasDescription): Subject[NonEmptyList, List, NonEmptyList[_], List[_]] = new Subject(locator)

  /** Get the [[Subject]] in current context.
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
  def it: Subject[Id, Id, Id[_], Id[_]] = new Subject(Self)

  implicit final class AsOps[F[_], G[_]: Functor, A](val ffa: Free[F, G[String]]) {

    /** Convert a string to type `B`.
      *
      * @param f
      * @return
      */
    def as[B](implicit f: String => B): Free[F, G[B]] = ffa.map(_.map(f))
  }

  // implicit final class LocatorOps(val sc: StringContext) extends AnyVal {
  //   def g(args: Any*): String = {
  //     val strings     = sc.parts.iterator
  //     val expressions = args.iterator
  //     var buf         = new StringBuilder(strings.next())
  //     while (strings.hasNext) {
  //       buf.append(expressions.next())
  //       buf.append(strings.next())
  //     }
  //     buf.toString()
  //   }
  // }

  /** An [[Operator]] to get attribute by name from a [[Subject]].
    *
    * {{{ a("a.title") get attr("href") }}}
    *
    * @param name
    * @return
    */
  def attr(name: String): Operator[String] = Attribute(name)

  /** An [[Operator]] to get text from a [[Subject]].
    *
    * {{{ a("span.content") get text  }}}
    *
    * @return
    */
  def text: Operator[String] = Text

  /** An [[Operator]] to input a value to a [[Subject]].
    *
    * {{{ a("input.username") apply input("rock") }}}
    *
    * @param value
    * @return
    */
  def input(value: String): Operator[Unit] = Input(value)

  /** An [[Operator]] to hover mouse on a [[Subject]].
    *
    * {{{ a("a.link") apply hover }}}
    *
    * @return
    */
  def hover: Operator[Unit] = Hover

  def output[F[_]: Functor: Foldable, A: Monoid](fa: F[A]): Unit = fa.map(println).fold

  def output(values: Any*): Unit = println(values.mkString(", "))

  def explore(urls: NonEmptyList[URL]): ControlOr[Unit] = Control.explore(urls.head, urls.tail).asLeft

  def explore(url: URL): ControlOr[Unit] = explore(NonEmptyList.one(url))

  def default[A](a: A): ControlOr[A] = a.asRight

  def one[A](a: A): ControlOr[NonEmptyList[A]] = NonEmptyList.one(a).asRight

  def repeat: ControlOr[Unit] = Control.repeat.asLeft

  def retry(max: Int): ControlOr[Unit] = Control.retry(max).asLeft

}
