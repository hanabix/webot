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
  scalaVersion      := "2.13.16",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalafmtOnCompile := true,
  scalacOptions += "-deprecation",
  scalacOptions += "-Wunused",
  resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases",
  libraryDependencies ++= Seq(
    "org.seleniumhq.selenium" % "selenium-java"      % "4.15.0",
    "com.lihaoyi"             % "ammonite"           % "3.0.2" cross CrossVersion.full,
    "org.typelevel"          %% "cats-free"          % "2.13.0",
    "org.scalatest"          %% "scalatest-wordspec" % "3.2.19" % Test,
    "org.scalactic"          %% "scalactic"          % "3.2.19",
    "org.scalamock"          %% "scalamock"          % "7.3.0"  % Test
  )
)
