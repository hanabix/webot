package webot

import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory
import org.openqa.selenium.Proxy
import selenium._

class DslSpec extends AnyWordSpec with MockFactory {
  "open a url" when {
    "single page" should {
      "get text from a subject" in {

        implicit val proxy = new Proxy().setSocksProxy("127.0.0.1:1080")

        open("https://github.com/trending") apply {
          for {
            projects <- a("article.Box-row") get {
              for {
                name  <- a("h1.h3.lh-condensed") get text
                url   <- a("h1.h3.lh-condensed > a") get attr("href")
                stars <- a("div.f6.color-text-secondary.mt-2 > a[href$=\"stargazers\"]") get text
              } yield (name, url, stars)
            }
          } yield println(projects)
        }

      }
    }
  }
}
