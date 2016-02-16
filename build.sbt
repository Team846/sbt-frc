sbtPlugin := true

organization := "com.lynbrookrobotics"

name := "sbt-frc"

version := "0.3.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies += "com.decodified" %% "scala-ssh" % "0.7.0"

publishMavenStyle := true
publishTo := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))
