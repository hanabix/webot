package com.github.zhongl.webot

import cats.syntax.either._
import cats.data.NonEmptyList
import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory

class ControlSpec extends AnyWordSpec with MockFactory {
  val compiled = mock[Compiled]
  val done: ControlOr[Unit] = ().asRight

  "Control runner" when {
    "single page" should {
      "be end with one round" in {
        (compiled.apply _).expects(*).returning(done).once()
        Control.runner(compiled)("https://example.com")
      }

      "retry" in {
        (compiled.apply _).expects(Option("https://example.com")).returning(retry).once()
        (compiled.apply _).expects(Option("https://example.com")).returning(done).once()
        Control.runner(compiled)("https://example.com")
      }

      "repeat" in {
        (compiled.apply _).expects(Option("https://example.com")).returning(repeat).once()
        (compiled.apply _).expects(Option.empty[String]).returning(done).once()
        Control.runner(compiled)("https://example.com")
      }

      "complain" in {
        (compiled.apply _).expects(*).returning(Control.complain("mock error").asLeft).once()
        Control.runner(compiled)("https://example.com")
      }
    }

    "multiple pages" should {
      "explore one" in {
        (compiled.apply _).expects(Option("https://example.com")).returning(explore("https://example.com/a")).once()
        (compiled.apply _).expects(Option("https://example.com/a")).returning(done).once()
        Control.runner(compiled)("https://example.com")
      }

      "explore more" in {
        val urls = NonEmptyList.of("https://example.com/a", "https://example.com/b")
        (compiled.apply _).expects(Option("https://example.com")).returning(explore(urls)).once()
        (compiled.apply _).expects(Option("https://example.com/a")).returning(done).once()
        (compiled.apply _).expects(Option("https://example.com/b")).returning(done).once()
        Control.runner(compiled)("https://example.com")
      }
    }
  }
}
