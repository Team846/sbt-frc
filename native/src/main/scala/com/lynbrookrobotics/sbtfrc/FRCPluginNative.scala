package com.lynbrookrobotics.sbtfrc

import sbt.Keys.streams
import sbt.{AutoPlugin, Tests, plugins, settingKey, task, taskKey}
import sbt.Package.ManifestAttributes
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object FRCPluginNative extends AutoPlugin{
  override def requires = plugins.JvmPlugin && scalanative.sbtplugin.ScalaNativePlugin

  val autoImport = Keys

  override lazy val projectSettings = Seq(
    Keys.trackedFiles := Set(RoboRioNative.codePath),
    Keys.deploy := RoboRioNative.deployCode.value,

    Keys.markRobotCodeVersion := RoboRioNative.Deployment.markRobotCodeVersionTsk.value,
    Keys.restoreRobotCodeVersion := RoboRioNative.Deployment.restoreRobotCodeVersionTsk.value,
    Keys.deleteRobotCode := RoboRioNative.Deployment.deleteRobotCodeTsk.value,

    Keys.restartRobotCode := RoboRioNative.Runtime.restartRobotCodeTsk.value,
    Keys.rebootRoboRio := RoboRioNative.Runtime.rebootRoboRioTsk.value,
    Keys.viewRobotConsole := RoboRioNative.Runtime.viewRobotConsoleTsk.value,
  )
}
