package com.team846.sbt

import sbt.Keys.Classpath
import sbt._

object Keys {
  lazy val teamNumber = settingKey[Int]("Your FRC team #")
  lazy val robotClass = settingKey[String]("The Robot Class to boot")

  lazy val downloadWPILib = taskKey[(Classpath, Classpath)]("Download WPILib")
  lazy val deployJAR = taskKey[Unit]("Deploy assembled JAR to robot")
  lazy val restartCode = taskKey[Unit]("Restart robot code")
  lazy val deploy = taskKey[Unit]("Deploy code to robot and restart")
}
