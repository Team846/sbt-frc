package com.lynbrookrobotics.sbtfrc

import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt._
import sbtassembly.{MergeStrategy, PathList}
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object FRCPluginJVM extends AutoPlugin() {
  override def requires = plugins.JvmPlugin && sbtassembly.AssemblyPlugin

  val autoImport = Keys

  import sbtassembly.AssemblyKeys._

  def findRobotClasses(analysis: CompileAnalysis): Seq[String] = {
    Discovery(Set("edu.wpi.first.wpilibj.RobotBase"), Set.empty)(Tests.allDefs(analysis)) collect {
      case (definition, discovery) if discovery.baseClasses.contains("edu.wpi.first.wpilibj.RobotBase") =>
        definition.name()
    }
  }

  override lazy val projectSettings = Seq(
    Keys.robotClasses in Compile := (sbt.Keys.compile in Compile).map(findRobotClasses).value,
    Keys.robotClass := {
      val robotClasses = (Keys.robotClasses in Compile).value
      val logger = streams.value.log
      if (robotClasses.length > 1) {
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
      Seq(ManifestAttributes(("Robot-Class", Keys.robotClass.value))),

    Keys.trackedFiles := Set(RoboRioJvm.codePath),
    Keys.deploy := RoboRioJvm.deployCode.value,

    Keys.markRobotCodeVersion := RoboRioJvm.Deployment.markRobotCodeVersionTsk.value,
    Keys.restoreRobotCodeVersion := RoboRioJvm.Deployment.restoreRobotCodeVersionTsk.value,
    Keys.deleteRobotCode := RoboRioJvm.Deployment.deleteRobotCodeTsk.value,

    Keys.restartRobotCode := RoboRioJvm.Runtime.restartRobotCodeTsk.value,
    Keys.rebootRoboRio := RoboRioJvm.Runtime.rebootRoboRioTsk.value,
    Keys.viewRobotConsole := RoboRioJvm.Runtime.viewRobotConsoleTsk.value,
  )
}
