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
  scalaVersion      := "2.13.5",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  scalacOptions += "-Wunused",
  resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java"      % "4.1.1",
    "com.lihaoyi"             % "ammonite"           % "2.4.0" cross CrossVersion.full,
    "org.typelevel"          %% "cats-free"          % "2.6.1",
    "org.scalatest"          %% "scalatest-wordspec" % "3.2.10" % Test,
    "org.scalactic"          %% "scalactic"          % "3.2.10",
    "org.scalamock"          %% "scalamock"          % "5.1.0"  % Test
  )
)
