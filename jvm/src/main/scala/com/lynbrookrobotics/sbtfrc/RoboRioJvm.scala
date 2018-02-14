package com.lynbrookrobotics.sbtfrc

import java.io.File

import com.decodified.scalassh.SshClient
import com.lynbrookrobotics.sbt.RoboRio
import sbt.{Def, Logger}
import sbtassembly.AssemblyKeys
import sbt.Keys._

object RoboRioJvm extends RoboRio(Keys) {
  val codePath = s"$home/robot-code.jar"

  override val deployCode = Def.task {
    implicit val client = Connection.connectTsk.value
    implicit val logger = streams.value.log

    val code = AssemblyKeys.assembly.value
    Deployment.sendFile(code, codePath)
    Runtime.restartRobotCode
  }
}
