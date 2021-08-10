package webot

import java.net.URI
import java.time.Duration

import cats._
import cats.syntax.all._

import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote._
import org.openqa.selenium.support.ui._

package object selenium {

  final case class Handle private (element: WebElement, actions: Actions, uri: URI)

  implicit val element: Element[Handle] = new Element[Handle] with Control.Dsl {
    import Operator._

    def apply(ea: Handle) = new (Operator ~> ControlOr) {
      def apply[A](op: Operator[A]): ControlOr[A] = op match {
        case Input(value)    => ea.element.sendKeys(value).asRight
        case Text            => ea.element.getText().asRight
        case Hover           => ea.actions.moveToElement(ea.element).perform().asRight
        case Attribute(name) => attr(name)
      }

      private def attr(name: String): ControlOr[String] = {
        def absolute(href: String): String = {
          if (URI.create(href).isAbsolute()) href
          else {
            if (href.startsWith("/")) s"${ea.uri.getScheme()}://${ea.uri.getHost()}$href"
            else s"${ea.uri}$href"
          }
        }

        val r = Option(ea.element.getAttribute(name)).toRight(complain(s"Missing Attribute: $name"))
        name match {
          case "href" => r.map(absolute)
          case _      => r
        }
      }
    }
  }

  implicit final def runtime(implicit
      timeout: Duration = Duration.ofSeconds(3),
      proxy: Option[Proxy] = None
  ): Runtime[Handle] = { run =>
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

    val context: String => webot.Context[Handle] = { url =>
      rwd.get(url)
      val currentUrl = rwd.getCurrentUrl()
      val h          = Handle(rwd.findElement(By.tagName("html")), new Actions(rwd), URI.create(currentUrl))
      Context(rwd, h, currentUrl, sc => new FluentWait(sc).withTimeout(timeout))
    }
    try { run(context) }
    finally { rwd.quit() }
  }

}
