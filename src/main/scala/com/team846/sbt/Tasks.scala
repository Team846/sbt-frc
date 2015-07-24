package com.team846.sbt

import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys

object Tasks {
  val eclipseURLBase = "http://first.wpi.edu/FRC/roborio/release/eclipse"
  val wpiVersion = "java_0.1.0.201501221609"
  val remoteUser = "lvuser"
  val remoteJAR = "FRCUserProgram.jar"

  lazy val downloadWPILib = Def.task[(sbt.Keys.Classpath, sbt.Keys.Classpath)] {
    if (!(target.value / "frc-downloads" / "wpiJava" / "lib" / "WPILib.jar").exists()) {
      val logger = streams.value.log

      val downloadFolder = target.value / "frc-downloads"
      downloadFolder.mkdir()

      val contentJARPath = s"$eclipseURLBase/plugins/edu.wpi.first.wpilib.plugins.$wpiVersion.jar"

      logger.info(s"Downloading Eclipse plugin jar from $contentJARPath and unzipping")

      val contentJARTarget = downloadFolder / "content"
      contentJARTarget.mkdir()
      IO.unzipURL(new URL(contentJARPath), contentJARTarget)

      logger.success(s"Downloaded Eclipse plugin jar and unzipped")

      logger.info("Extracting WPILib libraries from plugin jar")

      val javaJARTarget = downloadFolder / "wpiJava"
      IO.unzip(contentJARTarget / "resources" / "java.zip", javaJARTarget)

      logger.info("Extracted WPILib libraries from plugin jar")

      ((javaJARTarget / "lib" ** "*.jar").classpath, (javaJARTarget / "sim/lib" ** "*.jar").classpath)
    } else {
      ((target.value / "frc-downloads" / "wpiJava" / "lib" ** "*.jar").classpath, (target.value / "frc-downloads" / "wpiJava" / "sim/lib" ** "*.jar").classpath)
    }
  }



  lazy val rioHost = Def.task {
    s"roboRIO-${Keys.teamNumber.value}.local"
  }

  lazy val deployJAR = Def.task {
    val logger = streams.value.log

    val assembledFile = AssemblyKeys.assembly.value
    val host = rioHost.value

    logger.info(s"Deploying $assembledFile to $remoteUser@$host:$remoteJAR")
    Seq("scp", "-o StrictHostKeyChecking=no", assembledFile.absolutePath, s"$remoteUser@$host:$remoteJAR").!
    logger.info("Copied JAR to roboRIO")
  }

  lazy val restartCode = Def.task {
    val logger = streams.value.log
    val host = rioHost.value

    logger.info("Attempting to restart robot code")
    Seq(
      "ssh",
      "-o StrictHostKeyChecking=no",
      s"$remoteUser@$host",
      "killall netconsole-host"
    ).!

    Seq(
      "ssh",
      "-o StrictHostKeyChecking=no",
      s"$remoteUser@$host",
      ". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r"
    ).!
    logger.info("Restarted robot code")
  }
}
