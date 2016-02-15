package com.lynbrookrobotics.sbt

import com.decodified.scalassh._
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys

object Tasks {
  val remoteUser = "lvuser"
  val remoteJAR = "/home/lvuser/FRCUserProgram.jar"

  lazy val rioHost = Def.task {
    if (Keys.staticIP.value) {
      s"10.${Keys.teamNumber.value.toString.dropRight(2)}.${Keys.teamNumber.value.toString.takeRight(2)}.2"
    } else {
      s"roboRIO-${Keys.teamNumber.value}-FRC.local"
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
      client.authenticatedClient.right.toOption match {
        case Some(_) =>
          logger.success("Connected to roboRIO")
          client.upload(assembledFile.absolutePath, remoteJAR).right.get
          logger.success("Copied JAR to roboRIO")
        case None =>
          logger.error("Could not connect to roboRIO")
      }
    }
  }

  lazy val restartCode = Def.task {
    val logger = streams.value.log
    val host = rioHost.value

    logger.info("Attempting to restart robot code")

    SSH(host, hostConfig.value) { client =>
      client.authenticatedClient.right.toOption match {
        case Some(_) =>
          logger.success("Connected to roboRIO")
          client.exec("killall netconsole-host").right.get
          client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
          logger.info("Restarted robot code")
        case None =>
          logger.error("Could not connect to roboRIO")
      }
    }
  }
}
