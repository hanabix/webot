[![CI](https://github.com/hanabix/webot/actions/workflows/ci.yml/badge.svg)](https://github.com/hanabix/webot/actions/workflows/ci.yml) [![Publish](https://github.com/hanabix/webot/actions/workflows/sbt-release.yml/badge.svg)](https://github.com/hanabix/webot/actions/workflows/sbt-release.yml)[![Coveralls github](https://img.shields.io/coveralls/github/hanabix/webot.svg)](https://coveralls.io/github/hanabix/webot?branch=main) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/61b6a7eb4e63417fbb16f8f4f0c8efba)](https://www.codacy.com/gh/hanabix/webot/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hanabix/webot&amp;utm_campaign=Badge_Grade) [![Maven Central](https://img.shields.io/maven-central/v/com.github.zhongl/webot_2.13)](https://search.maven.org/artifact/com.github.zhongl/webot_2.13)

**Webot** is a web robot EDSL base on scala.

## Quick Start

### Requirements

- Chrome
- Chrome Driver
- JDK
- Scala
- Ammonite

> Use [ammonite-repl](http://ammonite.io/) to run the scripts blow:

```scala
import $ivy.`com.github.zhongl:webot_2.13:0.1.2`, webot._, selenium._

final case class Location(city: String, country: String)
final case class Contributor(id: String, name: String, location: Option[Location])
final case class Project(name: String, url: URL, stars: Int, contrbs: List[Contributor])

open("https://github.com/trending") apply {
  for {
    projects <- all("article.Box-row") get {
      for {
        name    <- a("h1.h3.lh-condensed") get text as [String]
        url     <- a("h1.h3.lh-condensed > a") get attr("href") as [String]
        stars   <- a("div.f6.color-text-secondary.mt-2 > a") get text as [Int]
        contrbs <- all("div.f6.color-text-secondary.mt-2 > span > a") get {
          for {
            _    <- hover
            name <- a("div.Popover-message div.overflow-hidden > a.Link--primary") get text as [String]
            id   <- a("div.Popover-message div.overflow-hidden > a.Link--secondary") get text as [String] 
            loc  <- a("div.Popover-message div.mt-2") get_if_present text as [Location]
          } yield Contributor(id, name, loc)
        }
      } yield Project(name, url, stars, contrbs)
    }
  } output(json(projects))
}
```


