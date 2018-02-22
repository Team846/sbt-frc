package com.lynbrookrobotics.sbt

import sbt._

abstract class SbtFrcKeys {
  lazy val teamNumber = settingKey[Int]("FRC team number")
  lazy val trackedFiles = settingKey[Set[String]]("Files to keep working versions of")

  lazy val deploy = taskKey[Unit]("Deploy new robot code")

  lazy val markRobotCodeVersion = taskKey[Unit]("Save the current trackedFiles for future restore")
  lazy val restoreRobotCodeVersion = taskKey[Unit]("Restore saved trackedFiles")
  lazy val deleteRobotCode = taskKey[Unit]("Clear current and saved trackedFiles")

  lazy val restartRobotCode = taskKey[Unit] ("Restart currently running code")
  lazy val rebootRoboRio = taskKey[Unit]("Reboot the RoboRIO")
  lazy val viewRobotConsole = taskKey[Unit]("View robot print statements")
}
