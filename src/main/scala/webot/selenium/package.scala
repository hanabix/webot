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

      proxy.foreach { p =>
        options.setCapability("proxy", p)
      }

      val rwd = driver(options)
      try {
        val i = interpreter(new Handler(rwd, None, timeout))
        run(mayBeUrl => {
          mayBeUrl.map(_.toString).foreach(rwd.get)
          df(new URL(rwd.getCurrentUrl)).foldMap(i)
        })
      } finally {
        rwd.quit()
      }

    }

  final class Handler private[selenium] (wd: WebDriver, context: Option[WebElement], timeout: Duration) {
    import scala.jdk.FunctionConverters._
    import scala.jdk.CollectionConverters._

    private val wdw = new FluentWait(context.map(_.asInstanceOf[SearchContext]).getOrElse(wd))
      .withTimeout(timeout)

    def a(descriptor: String): Option[Handler] = {
      val by = By.cssSelector(descriptor)
      val cond: SearchContext => Handler = sc => {
        sc.findElement(by) match {
          case null                   => null
          case we if we.isDisplayed() => new Handler(wd, Some(we), timeout)
          case _                      => null
        }
      }

      Try(wdw.until(cond.asJava)).toOption
    }

    def all(descriptor: String): List[Handler] = {
      val by = By.cssSelector(descriptor)
      val cond: SearchContext => List[Handler] = sc => {
        sc.findElements(by) match {
          case wes if wes.isEmpty() => null
          case wes                  => wes.asScala.toList.map(we => new Handler(wd, Some(we), timeout))
        }
      }
      Try(wdw.until(cond.asJava)).toOption.getOrElse(Nil)
    }

    def exec(expr: Any): ControlOr[_] = expr match {
      case op: Operator[_] @unchecked   => execOp(op).asRight
      case ex: Expression[_] @unchecked => execEx(ex)
      case unknown                      => throw new UnsupportedOperationException(unknown.toString())
    }

    def execOp[A](op: Operator[A]): A = {
      if (context.isEmpty) throw new IllegalStateException("Should not exec a handler without context")
      val we = context.get
      op match {
        case Attribute(name) => we.getAttribute(name)
        case Text            => we.getText()
        case Input(value)    => we.sendKeys(value)
        case Hover           => new Actions(wd).moveToElement(we).build.perform()
      }
    }

    def execEx[A](expr: Expression[A]): ControlOr[A] = expr.foldMap(interpreter(this))
  }

  private[selenium] def interpreter(handler: Handler): (ExpressionA ~> ControlOr) = new (ExpressionA ~> ControlOr) {

    private def exec[A](tp: Type, descriptor: Option[String], op: Operator[_]): ControlOr[A] = tp match {
      case Focus.id(f)   => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.opt(f)  => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.nel(f)  => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.list(f) => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
    }

    private def exec[A](tp: Type, descriptor: Option[String], ex: Expression[_]): ControlOr[A] = tp match {
      case Focus.id(f)   => f(descriptor)(handler).map(_.execEx(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.opt(f)  => f(descriptor)(handler).map(_.execEx(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.nel(f)  => f(descriptor)(handler).map(_.execEx(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
      case Focus.list(f) => f(descriptor)(handler).map(_.execEx(ex)).value.map(_.sequence).flatten.asInstanceOf[ControlOr[A]]
    }

    final def apply[A](fa: ExpressionA[A]): ControlOr[A] = fa match {
      case SubjectGet(descriptor, op: Operator[_], tp) =>
        exec(tp, descriptor, op)

      case SubjectGet(descriptor, ex: Expression[_] @unchecked, tp) =>
        exec(tp, descriptor, ex)

      case SubjectApply(descriptor, op: Operator[_], tp) =>
        exec(tp, descriptor, op).map((_: Any) => ())

      case SubjectApply(descriptor, ex: Expression[_] @unchecked, tp) =>
        exec(tp, descriptor, ex).map((_: Any) => ())

      case unknown =>
        throw new UnsupportedOperationException(unknown.toString)
    }

  }

  trait Extractor[A, B] {
    def unapply(a: A): Option[B]
  }

  type Focus[F[_]] = Option[String] => Handler => Nested[ControlOr, F, Handler]
  object Focus {
    private def focus[F[_]: Functor, A >: F[_]: TypeTag](f: Focus[F]): Extractor[Type, Focus[F]] =
      new Extractor[Type, Focus[F]] {
        def unapply(a: Type) = {
          if (typeOf[A] <:< a) Some(f) else None
        }
      }

    val id = focus[Id, Id[_]] { d => h =>
      val oh: Option[Handler] = d.flatMap(h.a).orElse(Option(h))
      Nested(oh.map(_.asInstanceOf[Id[Handler]]).toRight(complain(s"Missing descriptor: $d")))
    }

    val opt = focus[Option, Option[_]] { d => h =>
      if (d.isEmpty) throw new IllegalStateException("Never be here")
      Nested(h.a(d.get).asRight)
    }

    val nel = focus[NonEmptyList, NonEmptyList[_]] { d => h =>
      if (d.isEmpty) throw new IllegalStateException("Never be here")
      Nested(h.all(d.get).toNel.toRight(complain(s"Missing descriptor: $d")))
    }

    val list = focus[List, List[_]] { d => h =>
      if (d.isEmpty) throw new IllegalStateException("Never be here")
      Nested(h.all(d.get).asRight)
    }
  }

}
