package com.lynbrookrobotics.sbt

import sbt._

object Keys {
  lazy val teamNumber = settingKey[Int]("Your FRC team #")
  lazy val robotClass = taskKey[String]("The Robot Class to boot")

  lazy val robotClasses = taskKey[Seq[String]]("Available Robot Classes")
  lazy val restartCode = taskKey[Unit]("Restart robot code")
  lazy val deploy = taskKey[Unit]("Deploy code to robot and restart")

  lazy val restore = taskKey[Unit]("Restore previous deploy")
}
