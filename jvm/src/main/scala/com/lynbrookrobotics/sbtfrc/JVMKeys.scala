package com.lynbrookrobotics.sbtfrc

import com.lynbrookrobotics.sbt.SbtFrcKeys
import sbt._

object JVMKeys extends SbtFrcKeys {
  lazy val robotClasses = taskKey[Seq[String]]("Classes detected to extend WPIlib's RobotBase class")
  lazy val robotClass = taskKey[String]("Selected class that will be run on launch")
}
