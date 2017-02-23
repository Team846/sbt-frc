sbtPlugin := true

organization := "com.lynbrookrobotics"

name := "sbt-frc"

version := "0.4.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies += "com.veact" %% "scala-ssh" % "0.8.0"

publishMavenStyle := true
publishTo := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))

lazy val root = Project("sbt-frc", file(".")).settings(
  addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
)
