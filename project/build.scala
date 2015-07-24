import sbt._

object build extends Build {
  override def projects = Seq(root)
  lazy val root = Project("sbt-frc", file(".")) settings addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
}
