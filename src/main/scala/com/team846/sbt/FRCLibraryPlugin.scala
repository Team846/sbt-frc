package com.team846.sbt

import sbt._

object FRCLibraryPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  object autoImport {
    val downloadWPILib = Keys.downloadWPILib
  }

  override lazy val projectSettings = Seq(
    sbt.Keys.unmanagedJars in Compile ++= Tasks.downloadWPILib.value._1,
    sbt.Keys.unmanagedJars in Test ++= Tasks.downloadWPILib.value._2,
    (sbt.Keys.unmanagedClasspath in Test) := (sbt.Keys.unmanagedClasspath in Test).value.filterNot(Tasks.downloadWPILib.value._1.contains)
  )
}
