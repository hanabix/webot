lazy val root = (project in file(".")).settings(
  name := "webot",
  version := "0.1",
  scalaVersion := "2.13.6",
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java" % "4.0.0-beta-3",
    "com.lihaoyi" % "ammonite" % "2.4.0" cross CrossVersion.full
  )
)
