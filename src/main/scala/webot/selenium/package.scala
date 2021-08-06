package webot

import cats._
import data._
import free._
import instances.all._
import syntax.all._
import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.remote._
import org.openqa.selenium.support.ui._

import java.time.Duration
import java.net.URL
import java.util.function.{Function => JF}
import scala.util.Try
import scala.reflect.runtime.universe._
import org.openqa.selenium.interactions.Actions
import Control._
import cats.evidence.Is

package object selenium {

  implicit final def webDriverRuntime(implicit timeout: Duration = Duration.ofSeconds(3), proxy: Option[Proxy] = None): Runtime[Handler] = df =>
    run => {

      def driver(options: ChromeOptions): WebDriver = {
        try {
          new RemoteWebDriver(options)
        } catch {
          case _: Throwable => new ChromeDriver(options)
        }
      }

      val options = new ChromeOptions()

      proxy.foreach { p => options.setCapability("proxy", p) }

      val rwd = driver(options)

      try {
        val i = interpreter(new Handler(rwd, None, timeout))
        run({ mayBeUrl =>
          mayBeUrl.map(_.toString).foreach(rwd.get)
          df(new URL(rwd.getCurrentUrl)).foldMap(i)
        })
      } finally {
        rwd.quit()
      }

    }

  class Handler private[selenium] (wd: WebDriver, context: Option[WebElement], timeout: Duration) {
    import scala.jdk.FunctionConverters._
    import scala.jdk.CollectionConverters._

    private val searchContext            = context.map(_.asInstanceOf[SearchContext]).getOrElse(wd)
    private def aWait(sc: SearchContext) = new FluentWait(sc).withTimeout(timeout)

    def a(locator: Locator with HasDescription): Option[Handler] = {
      def cond(description: String): SearchContext => Handler =
        _.findElement(By.cssSelector(description)) match {
          case null => null
          case we   => new Handler(wd, Some(we), timeout)
        }

      locator match {
        case Global(d) => Try(aWait(wd).until(cond(d).asJava)).toOption
        case Local(d)  => Try(aWait(searchContext).until(cond(d).asJava)).toOption
      }
    }

    def all(locator: Locator with HasDescription): List[Handler] = {
      def cond(description: String): SearchContext => List[Handler] =
        _.findElements(By.cssSelector(description)) match {
          case wes if wes.isEmpty() => null
          case wes                  => wes.asScala.toList.map(we => new Handler(wd, Some(we), timeout))
        }

      locator match {
        case Global(d) => Try(aWait(wd).until(cond(d).asJava)).toOption.getOrElse(Nil)
        case Local(d)  => Try(aWait(searchContext).until(cond(d).asJava)).toOption.getOrElse(Nil)
      }
    }

    def exec[A](op: Operator[A]): A = {
      if (context.isEmpty) throw new IllegalStateException("Should not exec a handler without context")
      val we = context.get
      op match {
        case Attribute(name) => we.getAttribute(name)
        case Text            => we.getText()
        case Input(value)    => we.sendKeys(value)
        case Hover           => new Actions(wd).moveToElement(we).perform()
      }
    }

    def exec[A](expr: Expression[A]): ControlOr[A] = expr.foldMap(interpreter(this))
  }

  private[selenium] def interpreter(handler: Handler): (ExpressionA ~> ControlOr) = new (ExpressionA ~> ControlOr) {

    private def exec[A](tp: Type, locator: Locator, op: Operator[_]): ControlOr[A] = tp match {
      case Focus.id(f)   => f(locator)(handler).map(_.exec(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.opt(f)  => f(locator)(handler).map(_.exec(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.nel(f)  => f(locator)(handler).map(_.exec(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.list(f) => f(locator)(handler).map(_.exec(op)).value.asInstanceOf[ControlOr[A]]
    }

    private def exec[A](tp: Type, locator: Locator, ex: Expression[_]): ControlOr[A] = tp match {
      case Focus.id(f)   => f(locator)(handler).map(_.exec(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.opt(f)  => f(locator)(handler).map(_.exec(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.nel(f)  => f(locator)(handler).map(_.exec(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.list(f) => f(locator)(handler).map(_.exec(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
    }

    final def apply[A](fa: ExpressionA[A]): ControlOr[A] = fa match {
      case SubjectGet(locator, op: Operator[_], tp) =>
        exec(tp, locator, op)

      case SubjectGet(locator, ex: Expression[_] @unchecked, tp) =>
        exec(tp, locator, ex)

      case SubjectApply(locator, op: Operator[_], tp) =>
        exec(tp, locator, op).map((_: Any) => ())

      case SubjectApply(locator, ex: Expression[_] @unchecked, tp) =>
        exec(tp, locator, ex).map((_: Any) => ())

      case unknown =>
        throw new UnsupportedOperationException(unknown.toString)
    }

  }

  trait Extractor[A, B] {
    def unapply(a: A): Option[B]
  }

  type Focus[F[_]] = Locator => Handler => Nested[ControlOr, F, Handler]
  object Focus {
    private def focus[F[_]: Functor, A >: F[_]: TypeTag](f: Focus[F]): Extractor[Type, Focus[F]] =
      new Extractor[Type, Focus[F]] {
        def unapply(a: Type) = {
          if (typeOf[A] <:< a) Some(f) else None
        }
      }

    val id = focus[Id, Id[_]] { d => h =>
      val oh = d match {
        case Self                             => Option(h)
        case loc: Locator with HasDescription => h.a(loc)
      }
      Nested(oh.map(_.asInstanceOf[Id[Handler]]).toRight(complain(s"Missing locator: $d")))
    }

    val opt = focus[Option, Option[_]] { d => h =>
      Nested(h.a(d.asInstanceOf[Locator with HasDescription]).asRight)
    }

    val nel = focus[NonEmptyList, NonEmptyList[_]] { d => h =>
      Nested(h.all(d.asInstanceOf[Locator with HasDescription]).toNel.toRight(complain(s"Missing locator: $d")))
    }

    val list = focus[List, List[_]] { d => h =>
      Nested(h.all(d.asInstanceOf[Locator with HasDescription]).asRight)
    }
  }

}
