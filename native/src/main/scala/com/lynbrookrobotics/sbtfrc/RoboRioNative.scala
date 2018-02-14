package com.lynbrookrobotics.sbtfrc

import com.lynbrookrobotics.sbt.RoboRio
import sbt._
import sbt.Keys._

import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

object RoboRioNative extends RoboRio(Keys) {
  val codePath = s"$home/robot-code"

  override val deployCode = Def.task {
    implicit val client = Connection.connectTsk.value
    implicit val logger = streams.value.log

    Deployment.sendFile((nativeLink in Compile).value, codePath)
    client.exec(s"rm -f $home/FRCUserProgram; cp $codePath $home/FRCUserProgram; . /etc/profile.d/natinst-path.sh; chown lvuser $home/FRCUserProgram; setcap 'cap_sys_nice=pe' $home/FRCUserProgram; chmod a+x $home/FRCUserProgram").right.get
    Runtime.restartRobotCode
  }
}
