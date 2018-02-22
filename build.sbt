organization in ThisBuild := "com.lynbrookrobotics"

name := "sbt-frc"

scalaVersion in ThisBuild  := "2.12.4"

publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := Some(Resolver.file("gh-pages-repo", baseDirectory.value / "repo"))

lazy val root = Project("sbt-frc", file(".")).aggregate(
  sbtFrcCore,
  sbtFrcJvm,
  sbtFrcNative
).settings(
  publish := {},
  publishLocal := {}
)

lazy val sbtFrcCore = project.in(file("core")).settings(
  name := "sbt-frc-core",
  sbtPlugin := true
)

lazy val sbtFrcJvm = project.in(file("jvm")).dependsOn(sbtFrcCore).settings(
  name := "sbt-frc-jvm",
  sbtPlugin := true,
  addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
)

lazy val sbtFrcNative = project.in(file("native")).dependsOn(sbtFrcCore).settings (
  name := "sbt-frc-native",
  sbtPlugin := true,
  resolvers += "Funky-Repo" at "http://team846.github.io/repo",
  addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.7-arm-jni-threads")
)
