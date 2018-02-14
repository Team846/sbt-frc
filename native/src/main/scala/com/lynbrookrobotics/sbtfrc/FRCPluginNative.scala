package com.lynbrookrobotics.sbtfrc

import sbt.Keys.streams
import sbt.{AutoPlugin, Tests, plugins}
import sbt.Package.ManifestAttributes
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object FRCPluginNative extends AutoPlugin{
  override def requires = plugins.JvmPlugin && scalanative.sbtplugin.ScalaNativePlugin

  val autoImport = Keys

  override lazy val projectSettings = Seq(
    Keys.trackedFiles := Set(RoboRioNative.codePath),
    Keys.restartCode := RoboRioNative.Runtime.restartRobotCodeTsk.value,
    Keys.deploy := RoboRioNative.deployCode.value,
    Keys.robotConsole := RoboRioNative.Runtime.viewRobotConsoleTsk.value,
    Keys.restoreWorking := RoboRioNative.Deployment.restoreRobotCodeVersionTsk.value,
    Keys.markWorking := RoboRioNative.Deployment.markRobotCodeVersionTsk.value,
    Keys.cleanRobot := RoboRioNative.Deployment.deleteRobotCodeTsk.value
  )
}
