package com.lynbrookrobotics.sbtfrc

import com.lynbrookrobotics.sbt.RoboRio
import sbt.Def
import sbtassembly.AssemblyKeys
import sbt.Keys._

object RoboRioJvm extends RoboRio(JVMKeys) {
  val codePath = s"$home/robot-code.jar"

  override val deployCode = Def.task {
    implicit val client = Connection.connectTsk.value
    implicit val logger = streams.value.log

    val code = AssemblyKeys.assembly.value
    Deployment.sendFile(code, codePath)
    Runtime.restartRobotCode
  }
}
