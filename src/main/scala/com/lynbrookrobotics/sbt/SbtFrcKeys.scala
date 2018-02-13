package com.lynbrookrobotics.sbt

import sbt._

abstract class SbtFrcKeys {
  lazy val teamNumber = settingKey[Int]("Your FRC team #")

  lazy val restartCode = taskKey[Unit]("Restart robot code")
  lazy val deploy = taskKey[Unit]("Deploy code to robot and restart")
  lazy val robotConsole = taskKey[Unit]("Listen to robot logs")

  lazy val restoreWorking = taskKey[Unit]("Restore working state of robot")
  lazy val markWorking = taskKey[Unit]("Save current state of robot")
  lazy val cleanRobot = taskKey[Unit]("Deletes current deploy")
}
