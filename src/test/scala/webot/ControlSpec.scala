package webot

import java.net.URL
import cats.syntax.either._
import cats.data.NonEmptyList
import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory
import webot.{retry => _, repeat => _, explore => _}

class ControlSpec extends AnyWordSpec with MockFactory {
  import Control._

  private val compiled              = mock[Compiled]
  private val done: ControlOr[Unit] = ().asRight
  private val example_com           = new URL("https://example.com")
  private val example_com_a         = new URL("https://example.com/a")
  private val example_com_b         = new URL("https://example.com/b")

  "Control runner" when {
    "single page" should {
      "be end with one round" in {
        (compiled.apply _).expects(*).returning(done).once()
        Control.runner(compiled)(example_com)
      }

      "retry" in {
        (compiled.apply _).expects(Option(example_com)).returning(Retry(1).asLeft).once()
        (compiled.apply _).expects(Option(example_com)).returning(done).once()
        Control.runner(compiled)(example_com)
      }

      "complain exceed max retring" in {
        (compiled.apply _).expects(Option(example_com)).returning(Retry(1).asLeft).once()
        (compiled.apply _).expects(Option(example_com)).returning(Retry(1).asLeft).once()
        Control.runner(compiled)(example_com)
      }

      "repeat" in {
        (compiled.apply _).expects(Option(example_com)).returning(Repeat.asLeft).once()
        (compiled.apply _).expects(Option.empty[URL]).returning(done).once()
        Control.runner(compiled)(example_com)
      }

      "complain" in {
        (compiled.apply _).expects(*).returning(Complain("mock error").asLeft).once()
        runner(compiled)(example_com)
      }
    }

    "multiple pages" should {
      "explore one" in {
        (compiled.apply _).expects(Option(example_com)).returning(Explore(example_com_a, Nil).asLeft).once()
        (compiled.apply _).expects(Option(example_com_a)).returning(done).once()
        Control.runner(compiled)(example_com)
      }

      "explore more" in {
        val urls = NonEmptyList.of(example_com_a, example_com_b)
        (compiled.apply _).expects(Option(example_com)).returning(Control.explore(urls.head, urls.tail).asLeft).once()
        (compiled.apply _).expects(Option(example_com_a)).returning(done).once()
        (compiled.apply _).expects(Option(example_com_b)).returning(done).once()
        Control.runner(compiled)(example_com)
      }
    }
  }
}
