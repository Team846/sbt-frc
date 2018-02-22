package com.lynbrookrobotics.sbtfrc

import com.lynbrookrobotics.sbt.RoboRio
import sbt.Keys._
import sbt._

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

object RoboRioNative extends RoboRio(NativeKeys) {
  val codePath = s"$home/robot-code"

  override val deployCode = Def.task {
    implicit val client = Connection.connectTsk.value
    implicit val logger = streams.value.log

    Deployment.sendFile((nativeLink in Compile).value, codePath)
    client.exec(s"rm -f $home/FRCUserProgram;" +
      s"cp $codePath $home/FRCUserProgram;" +
      s". /etc/profile.d/natinst-path.sh;" +
      s"chown lvuser $home/FRCUserProgram;" +
      s"setcap 'cap_sys_nice=pe' $home/FRCUserProgram;" +
      s"chmod a+x $home/FRCUserProgram"
    ).right.get
    Runtime.restartRobotCode
  }
}
