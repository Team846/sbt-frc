package com.lynbrookrobotics.sbtfrc

import sbt._

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

object FRCPluginNative extends AutoPlugin {
  override def requires = FRCPlugin && scalanative.sbtplugin.ScalaNativePlugin

  object autoImport

  import Deployment.home
  val codePath = s"$home/robot-code"

  override lazy val projectSettings = Seq(
    CoreKeys.trackedFiles += codePath,
    CoreKeys.deployCode := Def.task {
      implicit val client = Connection.connectTsk.value
      implicit val logger = Keys.streams.value.log

      Deployment.sendFile((nativeLink in Compile).value, codePath)
      client.exec(s"rm -f $home/FRCUserProgram;" +
        s"cp $codePath $home/FRCUserProgram;" +
        s". /etc/profile.d/natinst-path.sh;" +
        s"chown lvuser $home/FRCUserProgram;" +
        s"setcap 'cap_sys_nice=pe' $home/FRCUserProgram;" +
        s"chmod a+x $home/FRCUserProgram"
      ).right.get
      Runtime.restartRobotCode
    }.value
  )
}
