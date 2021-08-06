package webot.selenium

import org.openqa.selenium.WebDriver
import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory
import cats._
import syntax.all._
import webot._
import java.time.Duration

class InterpreterSpec extends AnyWordSpec with MockFactory {
  private class FakeHandler extends Handler(mock[WebDriver], None, Duration.ZERO)
  private val handler: Handler = mock[FakeHandler]

  "A subject " when {

    "get operator" should {
      "be blabla" in {
        (handler.a _).expects(Local("a.link")).returning(Option(handler))
        (handler.exec(_: Operator[String])).expects(Text).returning("blabla")

        val expression = for { t <- a("a.link") get text } yield t
        val result     = expression.foldMap(interpreter(handler))
        assert(result.exists(_ == "blabla"))
      }
    }

    "get expression" should {
      "be that" in {
        (handler.a _).expects(Local("div")).returning(Option(handler))
        (handler.exec(_: Expression[(String, String)])).expects(*).returning(("blabla", "/blabla").asRight)

        val expression = for {
          t <- a("div") get {
            for {
              x <- a("span") get text
              y <- a("a") get attr("href")
            } yield (x, y)
          }
        } yield t
        val result = expression.foldMap(interpreter(handler))
        assert(result.exists(_ === ("blabla", "/blabla")))
      }
    }

    "apply input `hey`" should {
      "be that" in {
        (handler.a _).expects(Local("input")).returning(Option(handler))
        (handler.exec(_: Operator[Unit])).expects(Input("hey")).returning(())

        val procedure = for { _ <- a("input") apply input("hey") } yield ()
        procedure.foldMap(interpreter(handler))
      }
    }

    "apply procedure" should {
      "be that" in {
        (handler.a _).expects(Local("input")).returning(Option(handler))
        (handler.exec(_: Procedure)).expects(*).returning(().asRight)

        val procedure = for {
          _ <- a("input") apply {
            for { _ <- a("any") apply input("what ever") } yield ()
          }
        } yield ()
        procedure.foldMap(interpreter(handler))
      }
    }
  }
}
