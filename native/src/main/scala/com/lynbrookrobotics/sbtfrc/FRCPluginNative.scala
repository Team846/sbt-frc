package com.lynbrookrobotics.sbtfrc

import sbt.AutoPlugin

object FRCPluginNative extends AutoPlugin {
  override def requires = scalanative.sbtplugin.ScalaNativePlugin

  val autoImport = NativeKeys

  override lazy val projectSettings = Seq(
    NativeKeys.trackedFiles := Set(RoboRioNative.codePath),
    NativeKeys.deploy := RoboRioNative.deployCode.value,

    NativeKeys.markRobotCodeVersion := RoboRioNative.Deployment.markRobotCodeVersionTsk.value,
    NativeKeys.restoreRobotCodeVersion := RoboRioNative.Deployment.restoreRobotCodeVersionTsk.value,
    NativeKeys.deleteRobotCode := RoboRioNative.Deployment.deleteRobotCodeTsk.value,

    NativeKeys.restartRobotCode := RoboRioNative.Runtime.restartRobotCodeTsk.value,
    NativeKeys.rebootRoboRio := RoboRioNative.Runtime.rebootRoboRioTsk.value,
    NativeKeys.viewRobotConsole := RoboRioNative.Runtime.viewRobotConsoleTsk.value,
  )
}
