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

  implicit final def webDriverRuntime(implicit timeout: Duration = Duration.ofSeconds(3), proxy: Option[String] = None): Runtime[Handler] = df =>
    run => {

      def driver(options: ChromeOptions): WebDriver = {
        try {
          new RemoteWebDriver(options)
        } catch {
          case _: Throwable => new ChromeDriver(options)
        }
      }

      val options = new ChromeOptions()

      proxy.foreach { s =>
        options.setCapability("proxy", new Proxy().setSocksProxy(s))
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

    def exec[A](expr: Any): ControlOr[A] = expr match {
      case op: Operator[A] @unchecked   => execOp(op)
      case ex: Expression[A] @unchecked => execEx(ex)
      case unknown                      => throw new UnsupportedOperationException(unknown.toString())
    }

    private def execOp[A](op: Operator[A]): ControlOr[A] = {
      def cast(a: Any): ControlOr[A] = a.asInstanceOf[A].asRight

      if (context.isEmpty) throw new IllegalStateException("Should not exec a handler without context")
      val we = context.get
      op match {
        case Attribute(name) => Option(we.getAttribute(name).asInstanceOf[A]).toRight(complain(s"Missing attribute $name"))
        case Text            => cast(we.getText())
        case Input(value)    => cast(we.sendKeys(value))
        case Hover           => cast(new Actions(wd).moveToElement(we).build.perform())
      }
    }

    private def execEx[A](expr: Expression[A]): ControlOr[A] = expr.foldMap(interpreter(this))
  }

  private def interpreter(handler: Handler): (ExpressionA ~> ControlOr) = new (ExpressionA ~> ControlOr) {

    final def apply[A](fa: ExpressionA[A]): ControlOr[A] = fa match {
      case SubjectGet(descriptor, expression, IsId()) =>
        handler
          .a(descriptor)
          .map(_.exec(expression))
          .toRight(complain(s"Missing $descriptor"))
          .asInstanceOf[ControlOr[A]]

      case SubjectGet(descriptor, expression, IsOption()) =>
        handler
          .a(descriptor)
          .map(_.exec(expression))
          .asRight
          .asInstanceOf[ControlOr[A]]

      case SubjectGet(descriptor, expression, IsNonEmptyList()) =>
        handler
          .all(descriptor)
          .map(_.exec(expression))
          .toNel
          .toRight(complain(s"Missing $descriptor"))
          .asInstanceOf[ControlOr[A]]

      case SubjectGet(descriptor, expression, IsList()) =>
        handler
          .all(descriptor)
          .map(_.exec(expression))
          .asRight
          .asInstanceOf[ControlOr[A]]

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

  private trait Predictor {
    def unapply(tt: Type): Boolean
  }

  private def is[T: TypeTag] = new Predictor {
    def unapply(t: Type): Boolean = t =:= typeOf[T]
  }

  private val IsId           = is[Id[_]]
  private val IsOption       = is[Option[_]]
  private val IsNonEmptyList = is[NonEmptyList[_]]
  private val IsList         = is[List[_]]

}
