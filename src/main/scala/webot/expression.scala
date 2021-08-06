package webot

import java.net.URL
import scala.reflect.runtime.universe._
import cats._
import data._
import free._

sealed trait ExpressionA[A]
object ExpressionA {
  final private[webot] case class SubjectGet[F[_], G[_], A](locator: Locator, expr: F[A], tp: Type)   extends ExpressionA[G[A]]
  final private[webot] case class SubjectApply[F[_], G[_]](locator: Locator, proc: F[Unit], tp: Type) extends ExpressionA[Unit]
  final private[webot] case class Go(control: Control)                                                extends ExpressionA[Unit]
}

private[webot] final class Subject[S[_], L[_], S_ >: S[_]: TypeTag, L_ >: L[_]: TypeTag](locator: Locator) {
  import ExpressionA._
  def apply[H[_]](proc: H[Unit]): Procedure                 = Free.liftF[ExpressionA, Unit](SubjectApply(locator, proc, typeOf[S_]))
  def apply_if_present[H[_]](proc: H[Unit]): Procedure      = Free.liftF[ExpressionA, Unit](SubjectApply(locator, proc, typeOf[L_]))
  def get[H[_], A](expr: H[A]): Expression[S[A]]            = Free.liftF[ExpressionA, S[A]](SubjectGet(locator, expr, typeOf[S_]))
  def get_if_present[H[_], A](expr: H[A]): Expression[L[A]] = Free.liftF[ExpressionA, L[A]](SubjectGet(locator, expr, typeOf[L_]))
}

trait ExpressionDSL {
  import ExpressionA._
  import Control._

  type Expression[A] = Free[ExpressionA, A]
  type Procedure     = Free[ExpressionA, Unit]

  /** Get a [[Subject]] contains one element Subject by descriptor.
    *
    * {{{ a("a.link") }}}
    *
    * Get element tag named `a` and decorated by class `link`.
    *
    * @param descriptor
    * @return
    */
  def a(description: String): Subject[Id, Option, Id[_], Option[_]] = a(Local(description))

  /** Get a [[Subject]] contains one elements by [[Locator]].
    *
    * {{ a(g"a.link") }}
    *
    * Get element tag named `a` and decorated by class `link` in global search context.
    *
    * @param locator
    * @return
    */
  def a(locator: Locator with HasDescription): Subject[Id, Option, Id[_], Option[_]] = new Subject(locator)

  /** Get a [[Subject]] contains all elements by description in current search context.
    *
    * {{{ a("a.link") }}}
    *
    * Get element tag named `a` and decorated by class `link`.
    *
    * @param descriptor
    * @return
    */
  def all(description: String): Subject[NonEmptyList, List, NonEmptyList[_], List[_]] = all(Local(description))

  /** Get a [[Subject]] contains all elements by [[Locator]].
    *
    * {{ all(g"a.link") }}
    *
    * Get all elements tag named `a` and decorated by class `link` in global search context.
    *
    * @param locator
    * @return
    */
  def all(locator: Locator with HasDescription): Subject[NonEmptyList, List, NonEmptyList[_], List[_]] = new Subject(locator)

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

  /** Sequential explore urls.
    *
    * @param urls
    * @return
    */
  def explore(urls: NonEmptyList[URL]): Procedure = Free.liftF(Go(Explore(urls.head, urls.tail)))

  /** Explore url.
    *
    * @param url
    * @return
    */
  def explore(url: URL): Procedure = explore(NonEmptyList.one(url))

  /** Repeat current url without refresh.
    *
    * @return
    */
  def repeat: Procedure = Free.liftF(Go(Repeat))

  /** Retry current url after refresh, and halt if exceed max times.
    *
    * @param max
    * @return
    */
  def retry(max: Int): Procedure = Free.liftF(Go(Retry(max)))

}
