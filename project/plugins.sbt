resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.11")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.12")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.2")
