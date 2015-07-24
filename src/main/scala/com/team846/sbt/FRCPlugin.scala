package com.team846.sbt

import sbt.Package.ManifestAttributes
import sbt._
import sbtassembly.{PathList, MergeStrategy}

object FRCPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin && sbtassembly.AssemblyPlugin

  val autoImport = Keys

  import sbtassembly.AssemblyKeys._

  override lazy val projectSettings = Seq(
    sbt.Keys.unmanagedJars in Compile ++= Tasks.downloadWPILib.value._1,
    sbt.Keys.unmanagedJars in Test ++= Tasks.downloadWPILib.value._2,
    (sbt.Keys.unmanagedClasspath in Test) := (sbt.Keys.unmanagedClasspath in Test).value.filterNot(Tasks.downloadWPILib.value._1.contains),
    Keys.deployJAR := Tasks.deployJAR.value,
    Keys.restartCode := Tasks.restartCode.value,
    Keys.deploy <<= Keys.restartCode dependsOn Keys.deployJAR,
    assemblyExcludedJars in assembly := {
      val cp = (sbt.Keys.fullClasspath in assembly).value
      cp filter {c => c.data.getName == "WPILib-sources.jar" || c.data.getName == "NetworkTables-sources.jar"}
    },
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.rename
      case x =>
        MergeStrategy.first
    },
    sbt.Keys.mainClass in assembly := Some("edu.wpi.first.wpilibj.RobotBase"),
    sbt.Keys.packageOptions in assembly := (sbt.Keys.packageOptions in assembly).value ++ Seq(ManifestAttributes(("Robot-Class", Keys.robotClass.value)))
  )
}
