package com.lynbrookrobotics.sbtfrc

import sbt._
import sbt.Keys._
import sbt.Package.ManifestAttributes
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
    Keys.trackedFiles := Set(RoboRioJvm.codePath, RoboRioJvm.depsPath),
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
    Keys.restartCode := RoboRioJvm.Runtime.restartRobotCodeTsk.value,
    Keys.deploy := RoboRioJvm.deployCode.value,
    Keys.robotConsole := RoboRioJvm.Runtime.viewRobotConsoleTsk.value,
    Keys.restoreWorking := RoboRioJvm.Deployment.restoreRobotCodeVersionTsk.value,
    Keys.markWorking := RoboRioJvm.Deployment.markRobotCodeVersionTsk.value,
    Keys.cleanRobot := RoboRioJvm.Deployment.deleteRobotCodeTsk.value,
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.rename
      case PathList("reference.conf") =>
        MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    sbt.Keys.mainClass in assembly := Some("edu.wpi.first.wpilibj.RobotBase"),
    sbt.Keys.packageOptions in assembly := (sbt.Keys.packageOptions in assembly).value ++
      Seq(ManifestAttributes(("Robot-Class", Keys.robotClass.value))),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(
      includeScala = false, includeDependency = false,
      appendContentHash = true,
      cacheOutput = false
    )
  )
}
