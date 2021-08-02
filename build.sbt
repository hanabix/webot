lazy val root = (project in file(".")).settings(
  name := "webot",
  organization := "com.github.zhongl",
  scalaVersion := "2.13.6",
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "4.0.0-beta-3",
    "com.lihaoyi" % "ammonite" % "2.4.0" cross CrossVersion.full,
    "org.typelevel" %% "cats-core" % "2.6.1",
    "org.typelevel" %% "cats-free" % "2.6.1",
    "org.scalactic" %% "scalactic" % "3.2.9",
    "org.scalatest" %% "scalatest-wordspec" % "3.2.9" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test
  )
)
