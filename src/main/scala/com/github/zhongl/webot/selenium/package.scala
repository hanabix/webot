package com.github.zhongl.webot

import cats._
import cats.free._
import cats.instances.all._
import cats.syntax.all._
import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.remote._
import org.openqa.selenium.support.ui._

import java.time._
import java.util.function.{Function => JF}
import scala.jdk.CollectionConverters._
import scala.util.Try

package object selenium {

  import Expression._
  import Operator._
  import Control._

  private implicit final class WaitOps(val wd: WebDriver) extends AnyVal {
    def find[F[_], A](cond: JF[WebDriver, F[A]])(implicit d: Duration): ControlOr[F[A]] = {
      Try(new WebDriverWait(wd, d).until(cond)).toEither.left.map[Control](t => Control.complain(t.getMessage))
    }
  }

  implicit def webDriverRuntime(implicit d: Duration = Duration.ofSeconds(3)): Runtime = df =>
    run => {

      def interpreter(wd: WebDriver)(implicit d: Duration): (Expression ~> ControlOr) = new (Expression ~> ControlOr) {

        final def apply[A](fa: Expression[A]): ControlOr[A] = fa match {
          case And(a: Expression[A], g: (A => ControlOr[A]) @ unchecked) =>
            apply(a).flatMap(g)

          case Or(a: Expression[A], g: (() => ControlOr[A]) @ unchecked) =>
            apply(a).orElse(g())

          case Single(select, op) =>
            val cond: JF[WebDriver, Id[WebElement]] =
              ExpectedConditions.visibilityOfElementLocated(By.cssSelector(select))
            apply0(wd.find(cond)).apply(op).asInstanceOf[ControlOr[A]]

          case Multiple(select, op) =>
            val cond = ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(select))
            val ee = wd.find(cond).flatMap(_.asScala.toList.toNel.toRight[Control](complain("Never be here")))
            apply0(ee).apply(op).asInstanceOf[ControlOr[A]]
        }

        private def apply0[F[_]: Functor: Traverse](
            found: ControlOr[F[WebElement]]
        ): PartialFunction[Operator[_], ControlOr[F[_]]] = {
          case Attribute(name) =>
            found.flatMap {
              _.map { e =>
                Option(e.getAttribute(name)).toRight[Control](Complain(s"Missing attribute: $name"))
              }.sequence
            }

          case Text =>
            found.flatMap {
              _.map { e =>
                Option(e.getText()).toRight[Control](Complain(s"Never be here"))
              }.sequence
            }

        }
      }

      val driver = new RemoteWebDriver(new ChromeOptions())
      try {
        val i = interpreter(driver)
        run(mayBeUrl => {
          mayBeUrl.foreach(driver.get)
          df(driver.getCurrentUrl).foldMap(i)
        })
      } finally {
        driver.quit()
      }

    }
}
