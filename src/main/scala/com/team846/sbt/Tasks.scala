package com.team846.sbt

import com.decodified.scalassh._
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys

object Tasks {
  val eclipseURLBase = "http://first.wpi.edu/FRC/roborio/release/eclipse"
  val wpiVersion = "java_0.1.0.201501221609"
  val remoteUser = "lvuser"
  val remoteJAR = "/home/lvuser/FRCUserProgram.jar"

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
    if (Keys.staticIP.value) {
      s"10.${Keys.teamNumber.value.toString.dropRight(2)}.${Keys.teamNumber.value.toString.takeRight(2)}.2"
    } else {
      s"roboRIO-${Keys.teamNumber.value}.local"
    }
  }

  lazy val hostConfig = Def.task {
    HostConfig(
      PasswordLogin(
        remoteUser,
        SimplePasswordProducer("")
      ),
      hostName = rioHost.value,
      hostKeyVerifier = HostKeyVerifiers.DontVerify
    )
  }

  lazy val deployJAR = Def.task {
    val logger = streams.value.log

    val assembledFile = AssemblyKeys.assembly.value
    val host = rioHost.value

    logger.info(s"Deploying $assembledFile to $remoteUser@$host:$remoteJAR")

    SSH(host, hostConfig.value) { client =>
      logger.success("Connected to roboRIO")
      client.upload(assembledFile.absolutePath, remoteJAR).right.get
      logger.success("Copied JAR to roboRIO")
    }
  }

  lazy val restartCode = Def.task {
    val logger = streams.value.log
    val host = rioHost.value

    logger.info("Attempting to restart robot code")

    SSH(host, hostConfig.value) { client =>
      logger.success("Connected to roboRIO")
      client.exec("killall netconsole-host").right.get
      client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
      logger.info("Restarted robot code")
    }
  }
}
