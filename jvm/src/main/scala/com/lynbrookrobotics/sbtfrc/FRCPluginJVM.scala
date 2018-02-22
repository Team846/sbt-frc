package com.lynbrookrobotics.sbtfrc

import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt._
import sbtassembly.{MergeStrategy, PathList}
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object FRCPluginJVM extends AutoPlugin {
  override def requires = plugins.JvmPlugin && sbtassembly.AssemblyPlugin

  val autoImport = JVMKeys

  import sbtassembly.AssemblyKeys._

  def findRobotClasses(analysis: CompileAnalysis): Seq[String] = {
    Discovery(Set("edu.wpi.first.wpilibj.RobotBase"), Set.empty)(Tests.allDefs(analysis)) collect {
      case (definition, discovery) if discovery.baseClasses.contains("edu.wpi.first.wpilibj.RobotBase") =>
        definition.name()
    }
  }

  override lazy val projectSettings = Seq(
    JVMKeys.robotClasses in Compile := (sbt.Keys.compile in Compile).map(findRobotClasses).value,
    JVMKeys.robotClass := {
      val robotClasses = (JVMKeys.robotClasses in Compile).value
      val logger = streams.value.log
      if (robotClasses.size > 1) {
        logger.warn(
          s"Multiple robot classes detected: ${robotClasses.mkString(", ")}"
        )
      }
      robotClasses.head
    },
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.rename
      case PathList("reference.conf") =>
        MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    sbt.Keys.mainClass in assembly := Some("edu.wpi.first.wpilibj.RobotBase"),
    sbt.Keys.packageOptions in assembly := (sbt.Keys.packageOptions in assembly).value ++
      Seq(ManifestAttributes(("Robot-Class", JVMKeys.robotClass.value))),

    JVMKeys.trackedFiles := Set(RoboRioJvm.codePath),
    JVMKeys.deploy := RoboRioJvm.deployCode.value,

    JVMKeys.markRobotCodeVersion := RoboRioJvm.Deployment.markRobotCodeVersionTsk.value,
    JVMKeys.restoreRobotCodeVersion := RoboRioJvm.Deployment.restoreRobotCodeVersionTsk.value,
    JVMKeys.deleteRobotCode := RoboRioJvm.Deployment.deleteRobotCodeTsk.value,

    JVMKeys.restartRobotCode := RoboRioJvm.Runtime.restartRobotCodeTsk.value,
    JVMKeys.rebootRoboRio := RoboRioJvm.Runtime.rebootRoboRioTsk.value,
    JVMKeys.viewRobotConsole := RoboRioJvm.Runtime.viewRobotConsoleTsk.value,
  )
}
