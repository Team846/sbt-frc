package com.team846.sbt

import sbt.Package.ManifestAttributes
import sbt._
import sbt.inc.Analysis
import sbtassembly.{PathList, MergeStrategy}
import xsbt.api.Discovery

object FRCPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin && sbtassembly.AssemblyPlugin

  val autoImport = Keys

  import sbtassembly.AssemblyKeys._

  def findRobotClasses(analysis: Analysis): Seq[String] = {
    Discovery(Set("edu.wpi.first.wpilibj.RobotBase"), Set.empty)(Tests.allDefs(analysis)) collect {
      case (definition, discovery) if discovery.baseClasses.contains("edu.wpi.first.wpilibj.RobotBase") =>
        definition.name()
    }
  }

  override lazy val projectSettings = Seq(
    Keys.robotClasses in Compile <<= sbt.Keys.compile in Compile map findRobotClasses,
    Keys.robotClass := {
      val robotClasses = (Keys.robotClasses in Compile).value
      if (robotClasses.length > 1) {
        sbt.Keys.streams.value.log.warn(s"Multiple robot classes detected: ${robotClasses.mkString(" ")}")
      }

      robotClasses.head
    },
    Keys.deployJAR := Tasks.deployJAR.value,
    Keys.restartCode := Tasks.restartCode.value,
    Keys.deploy <<= Keys.restartCode dependsOn Keys.deployJAR,
    Keys.staticIP := false,
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.rename
      case x =>
        MergeStrategy.first
    },
    sbt.Keys.mainClass in assembly := Some("edu.wpi.first.wpilibj.RobotBase"),
    sbt.Keys.packageOptions in assembly := (sbt.Keys.packageOptions in assembly).value ++ Seq(ManifestAttributes(("Robot-Class", Keys.robotClass.value)))
  )
}
