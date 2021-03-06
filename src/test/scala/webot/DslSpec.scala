package webot

import java.net.URI
import java.util.Arrays
import java.util.function

import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Wait

import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec

import selenium.{runtime => _, _}
import dsl._

class DslSpec extends AnyWordSpec with MockFactory {
  private val sc: SearchContext   = mock[SearchContext]
  private val element: WebElement = mock[WebElement]
  private val compiler            = implicitly[ContextCompiler[Handle]]
  private val url                 = "https://example.com/a"
  private val ctx                 = Context(sc, Handle(element, null, URI.create(url)), url, new FakeWait(_))

  "Expression" when {
    "nest" should {
      "get text from global context" in {
        val div  = mock[WebElement]
        val span = mock[WebElement]
        val p    = mock[WebElement]

        (sc.findElements _).expects(By.cssSelector("span")).returning(Arrays.asList(span))
        (element.findElements _).expects(By.cssSelector("div")).returning(Arrays.asList(div))
        (div.findElements _).expects(By.cssSelector("p")).returning(Arrays.asList(p))
        (p.getText: () => String).expects().returning("hello")
        (span.getText: () => String).expects().returning("world")

        val expr = for {
          x <- a("div") get {
            for {
              y <- a("p") get text
              z <- a(g"span") get text
            } yield (y, z)
          }
        } yield x
        assert(expr.foldMap(compiler(ctx)).value.value.getOrElse(("", "")) == ("hello", "world"))
      }

    }

    "just execute operators" should {
      "get text" in {
        (element.findElements _).expects(By.cssSelector("span")).returning(Arrays.asList(element))
        (element.getText: () => String).expects().returning("hello")

        val expr = for {
          x <- a("span") get text
        } yield x
        assert(expr.foldMap(compiler(ctx)).value.value.getOrElse("") == "hello")
      }

      "get text as Int" in {
        (element.findElements _).expects(By.cssSelector("span")).returning(Arrays.asList(element))
        (element.getText: () => String).expects().returning("10")

        val expr = for {
          x <- a("span") get text
        } yield x.as[Int]
        assert(expr.foldMap(compiler(ctx)).value.value.getOrElse(0) == 10)
      }

      "get attribute name" in {
        (element.findElements _).expects(By.cssSelector("a")).returning(Arrays.asList(element))
        (element.getAttribute _).expects("href").returning("/hello")

        val expr = for {
          x <- a("a") get attr("href")
        } yield x
        assert(expr.foldMap(compiler(ctx)).value.value.getOrElse("") == "https://example.com/hello")

      }
    }
  }

  final class FakeWait(sc: SearchContext) extends Wait[SearchContext] {
    def until[A](isTrue: function.Function[_ >: SearchContext, A]): A = isTrue.apply(sc)
  }
}
