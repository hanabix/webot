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

    def execEx(expr: Expression[_]): ControlOr[_] = expr.foldMap(interpreter(this))
  }

  private[selenium] def interpreter(handler: Handler): (ExpressionA ~> ControlOr) = new (ExpressionA ~> ControlOr) {

    private def get[A](tp: Type, descriptor: String, op: Operator[_]): ControlOr[A] = tp match {
      case Focus.id(f)   => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.opt(f)  => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.nel(f)  => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
      case Focus.list(f) => f(descriptor)(handler).map(_.execOp(op)).value.asInstanceOf[ControlOr[A]]
    }

    final def apply[A](fa: ExpressionA[A]): ControlOr[A] = fa match {
      case SubjectGet(descriptor, op: Operator[_], tp) =>
        get(tp, descriptor, op)

      case SubjectApply(descriptor, procedure, IsId()) =>
        handler
          .a(descriptor)
          .map(_.exec(procedure))
          .toRight(complain(s"Missing $descriptor"))
          .asInstanceOf[ControlOr[A]]

      case SubjectApply(descriptor, procedure, IsOption()) =>
        handler
          .a(descriptor)
          .map(_.exec(procedure))
          .asRight
          .asInstanceOf[ControlOr[A]]

      case SubjectApply(descriptor, procedure, IsNonEmptyList()) =>
        handler
          .all(descriptor)
          .map(_.exec(procedure))
          .toNel
          .toRight(complain(s"Missing $descriptor"))
          .asInstanceOf[ControlOr[A]]

      case SubjectApply(descriptor, procedure, IsList()) =>
        handler
          .all(descriptor)
          .map(_.exec(procedure))
          .asRight
          .asInstanceOf[ControlOr[A]]

      case unknown =>
        throw new UnsupportedOperationException(unknown.toString)
    }

  }

  trait Extractor[A, B] {
    def unapply(a: A): Option[B]
  }

  type Focus[F[_]] = String => Handler => Nested[ControlOr, F, Handler]
  object Focus {
    private def focus[F[_]: Functor, A >: F[_]: TypeTag](f: Focus[F]): Extractor[Type, Focus[F]] =
      new Extractor[Type, Focus[F]] {
        def unapply(a: Type) = {
          if (typeOf[A] <:< a) Some(f) else None
        }
      }

    val id = focus[Id, Id[_]] { d => h =>
      Nested(h.a(d).map(_.asInstanceOf[Id[Handler]]).toRight(complain(s"Missing descriptor: $d")))
    }

    val opt = focus[Option, Option[_]] { d => h =>
      Nested(h.a(d).asRight)
    }

    val nel = focus[NonEmptyList, NonEmptyList[_]] { d => h =>
      Nested(h.all(d).toNel.toRight(complain(s"Missing descriptor: $d")))
    }

    val list = focus[List, List[_]] { d => h =>
      Nested(h.all(d).asRight)
    }
  }

  private trait Predictor {
    def unapply(tt: Type): Boolean
  }

  private def is[T: TypeTag] = new Predictor {
    def unapply(t: Type): Boolean = t <:< typeOf[T]
  }

  private val IsId           = is[Id[_]]
  private val IsOption       = is[Option[_]]
  private val IsNonEmptyList = is[NonEmptyList[_]]
  private val IsList         = is[List[_]]

}
