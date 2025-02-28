lazy val root = (project in file(".")).settings(
  name         := "webot",
  organization := "com.github.zhongl",
  homepage     := Some(url("https://github.com/hanabix/webot")),
  licenses := List(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  developers := List(
    Developer(
      "zhongl",
      "Lunfu Zhong",
      "zhong.lunfu@gmail.com",
      url("https://github.com/zhongl")
    )
  ),
  scalaVersion      := "2.13.12",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  scalacOptions += "-Wunused",
  resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java"      % "4.15.0",
    "com.lihaoyi"             % "ammonite"           % "2.5.11" cross CrossVersion.full,
    "org.typelevel"          %% "cats-free"          % "2.10.0",
    "org.scalatest"          %% "scalatest-wordspec" % "3.2.17" % Test,
    "org.scalactic"          %% "scalactic"          % "3.2.17",
    "org.scalamock"          %% "scalamock"          % "6.2.0"  % Test
  )
)
