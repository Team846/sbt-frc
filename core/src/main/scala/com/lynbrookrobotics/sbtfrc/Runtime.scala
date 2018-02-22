package com.lynbrookrobotics.sbtfrc

import java.net.{DatagramPacket, DatagramSocket}

import com.decodified.scalassh.SshClient
import sbt.Keys.streams
import sbt.{Def, Logger, Task}

object Runtime {
  lazy val restartRobotCodeTsk = Def.task(
    restartRobotCode(Connection.connectTsk.value, streams.value.log)
  )

  def restartRobotCode(implicit client: SshClient, logger: Logger): Unit = {
    client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
    logger.success("restarted robot code")
  }

  lazy val rebootRoboRioTsk = Def.task(
    rebootRoboRio(Connection.connectTsk.value, streams.value.log)
  )

  def rebootRoboRio(implicit client: SshClient, logger: Logger): Unit = {
    client.exec("reboot").right.get
    client.close()
    logger.success("restarted RoboRIO")
  }

  lazy val viewRobotConsoleTsk: Def.Initialize[Task[Unit]] = Def.task {
    val SIZE = 1024
    val PORT = 6666
    val socket = new DatagramSocket(PORT)

    val thr = new Thread(() => while (!Thread.interrupted()) {
      try {
        val buffer = new Array[Byte](SIZE)
        val packet = new DatagramPacket(buffer, buffer.length)
        socket.receive(packet)
        val message = new String(packet.getData)
        print(message)
      } catch {
        case (e: Throwable) =>
      }
    })

    thr.start()

    scala.io.StdIn.readLine()
    println("\nTerminating")
    thr.interrupt()
    socket.close()
  }
}
