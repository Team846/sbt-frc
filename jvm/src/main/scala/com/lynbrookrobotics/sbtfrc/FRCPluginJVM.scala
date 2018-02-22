package com.lynbrookrobotics.sbtfrc

import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt._
import sbtassembly.{AssemblyKeys, AssemblyPlugin, MergeStrategy, PathList}
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object FRCPluginJVM extends AutoPlugin {
  override def requires = FRCPlugin && plugins.JvmPlugin && AssemblyPlugin

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

    CoreKeys.trackedFiles += s"${Deployment.home}/robot-code.jar",
    CoreKeys.deployCode := Def.task {
      implicit val client = Connection.connectTsk.value
      implicit val logger = streams.value.log

      val code = AssemblyKeys.assembly.value
      Deployment.sendFile(code, s"${Deployment.home}/robot-code.jar")
      Runtime.restartRobotCode
    }.value
  )
}
