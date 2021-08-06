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
import $ivy.`com.github.zhongl:webot_2.13:latest.release`, webot._, selenium._

open("https://baidu.com") apply {
  for {
    _           <- a("#kw") apply input("github")
    suggestions <- all("li.bdsug-overflow") get text
  } yield output(suggestions)
}
```


