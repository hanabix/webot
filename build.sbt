lazy val root = (project in file(".")).settings(
  name := "webot",
  version := "0.1",
  organization := "com.github.zhongl",
  scalaVersion := "2.13.6",
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "4.0.0-beta-3",
    "com.lihaoyi" % "ammonite" % "2.4.0" cross CrossVersion.full,
    "org.scalactic" %% "scalactic" % "3.2.9",
    "org.scalatest" %% "scalatest-wordspec" % "3.2.9" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test
  )
)
