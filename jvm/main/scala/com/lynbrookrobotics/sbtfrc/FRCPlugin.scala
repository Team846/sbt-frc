package com.lynbrookrobotics.sbtfrc

import sbt._
import sbtassembly.{MergeStrategy, PathList}

object FRCPlugin extends AutoPlugin {
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
    Keys.restartCode := Tasks.restartCode.value,
    Keys.deploy := Tasks.deploy.value,
    Keys.robotConsole := Tasks.robotConsole.value,
    Keys.restore := Tasks.restore.value,
    Keys.itWorks := Tasks.itWorks.value,
    Keys.roboClean := Tasks.roboClean.value,
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
