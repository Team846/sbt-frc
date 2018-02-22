package com.lynbrookrobotics.sbtfrc

import sbt._

object FRCPlugin extends AutoPlugin {
  val autoImport = CoreKeys

  override lazy val projectSettings = Seq(
    CoreKeys.deployCode := {},
    CoreKeys.trackedFiles := Set.empty,
    CoreKeys.deploy := CoreKeys.deployCode.value,

    CoreKeys.markRobotCodeVersion := Deployment.markRobotCodeVersionTsk.value,
    CoreKeys.restoreRobotCodeVersion := Deployment.restoreRobotCodeVersionTsk.value,
    CoreKeys.deleteRobotCode := Deployment.deleteRobotCodeTsk.value,

    CoreKeys.restartRobotCode := Runtime.restartRobotCodeTsk.value,
    CoreKeys.rebootRoboRio := Runtime.rebootRoboRioTsk.value,
    CoreKeys.viewRobotConsole := Runtime.viewRobotConsoleTsk.value
  )
}
