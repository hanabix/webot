resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.2")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.12")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.15")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.0")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
