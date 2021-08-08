package webot

import java.net.URL
import cats._
import syntax.either._
import data.NonEmptyList
import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory

class ControlSpec extends AnyWordSpec with MockFactory with Control.Dsl {
  import Control._

  private val fork                  = mock[Fork]
  private val done: ControlOr[Unit] = ().asRight
  private val example_com           = "https://example.com"
  private val example_com_a         = "https://example.com/a"
  private val example_com_b         = "https://example.com/b"

  "Control runner" when {
    "single page" should {
      "be end with one round" in {
        (fork.apply _).expects(*).returning(() => done).once()
        engine(fork)(example_com)
      }

      "retry" in {
        (fork.apply _).expects(example_com).returning(() => retry(1).asLeft).once()
        (fork.apply _).expects(example_com).returning(() => done).once()
        engine(fork)(example_com)
      }

      "complain exceed max retring" in {
        (fork.apply _).expects(example_com).returning(() => retry(1).asLeft).once()
        (fork.apply _).expects(example_com).returning(() => retry(1).asLeft).once()
        engine(fork)(example_com)
      }

      "repeat" in {
        val branch = mockFunction[ControlOr[Unit]]
        (fork.apply _).expects(example_com).returning(branch).once()
        branch.expects().returning(repeat.asLeft).once()
        branch.expects().returning(done).once()
        engine(fork)(example_com)
      }

      "complain" in {
        (fork.apply _).expects(*).returning(() => complain("mock error").asLeft).once()
        engine(fork)(example_com)
      }
    }

    "multiple pages" should {
      "explore one" in {
        (fork.apply _).expects(example_com).returning(() => explore[Id](example_com_a).asLeft).once()
        (fork.apply _).expects(example_com_a).returning(() => done).once()
        engine(fork)(example_com)
      }

      "explore more" in {
        val urls = List(example_com_a, example_com_b)
        (fork.apply _).expects(example_com).returning(() => explore(urls).asLeft).once()
        (fork.apply _).expects(example_com_a).returning(() => done).once()
        (fork.apply _).expects(example_com_b).returning(() => done).once()
        engine(fork)(example_com)
      }
    }
  }
}
