sbtPlugin := true

organization in ThisBuild := "com.lynbrookrobotics"

name := "sbt-frc"

version in ThisBuild := "0.5.0"

scalaVersion in ThisBuild  := "2.12.4"

libraryDependencies += "com.github.seratch.com.veact" %% "scala-ssh" % "0.8.0-1"

publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))

lazy val root = Project("sbt-frc", file("."))

lazy val sbtFrcJvm = project.in(file("jvm")).dependsOn(root).settings(
  name := "sbt-frc-jvm",
  sbtPlugin := true,
  addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
)

lazy val sbtFrcNative = project.in(file("native")).dependsOn(root).settings (
  name := "sbt-frc-native",
  sbtPlugin := true,
  addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7-arm-jni-threads")
)
