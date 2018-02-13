package com.lynbrookrobotics.sbt

import com.decodified.scalassh._
import com.lynbrookrobotics.sbt.SbtUtils._
import sbt._
import sbt.util.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RoboRIO {
  val home = "/home/lvuser"
  val markedVersions = s"$home/marked-versions"

  def connect(team: Int)
             (implicit logger: Logger): List[Future[SshClient]] =
    connect(addresses(team))

  def addresses(teamNumber: Int) = List(
    s"roboRIO-$teamNumber-FRC.local", // mDNS
    s"roboRIO-$teamNumber-FRC.lan", // DNS
    s"10.${teamNumber / 100}.${teamNumber % 100}.2", // Static IP,
    "172.22.11.2" // USB
  )

  def connect(hosts: List[String])
             (implicit logger: Logger): List[Future[SshClient]] = {
    val passwd = PasswordLogin(
      "admin", SimplePasswordProducer("")
    )

    hosts.map { it =>
      Future(connect(HostConfig(
        login = passwd,
        hostName = it,
        hostKeyVerifier = HostKeyVerifiers.DontVerify,
        connectTimeout = Some(5000)
      )).right.get)
    }
  }

  def connect(config: HostConfig)
             (implicit logger: Logger): Validated[SshClient] = {
    val client = SshClient(config.hostName, config)
    client.flatMap(_.authenticatedClient) match {
      case Right(_) =>
        logger.success(s"connected to ${config.hostName}")
        client
      case t: Left[String, SshClient] =>
        logger.err(s"connection to ${config.hostName} failed")
        t
    }
  }

  def restartCode(implicit logger: Logger, client: SshClient): Unit = {
    client.exec("killall netconsole-host").right.get
    client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
    logger.success("restarted robot code")
  }

  def reboot(implicit logger: Logger, client: SshClient): Unit = {
    client.exec("reboot").right.get
    client.close()
    logger.success("restarted RoboRIO")
  }

  def markVersion(trackedFiles: Set[String])
                 (implicit logger: Logger, client: SshClient): Unit = {
    client.exec(s"rm -r $markedVersions").right.get
    client.exec(s"mkdir $markedVersions").right.get
    trackedFiles.foreach { it =>
      val encodedName = b64(it)
      client.exec(s"cp $it /tmp/$encodedName").right.get
      client.exec(s"mv /tmp/$encodedName $markedVersions/$encodedName").right.get
    }
    logger.success("marked version")
  }

  def restoreVersion(trackedFiles: Set[String])
                    (implicit logger: Logger, client: SshClient) = trackedFiles.foreach { it =>
    val encodedName = b64(it)
    client.exec(s"cp $markedVersions/$encodedName /tmp/$encodedName").right.get
    client.exec(s"mv /tmp/$encodedName $it").right.get
    logger.success("restored version")
  }

  def clean(trackedFiles: Set[String])
           (implicit logger: Logger, client: SshClient): Unit = {
    client.exec(s"rm -r $markedVersions")
    trackedFiles.foreach(
      it => client.exec(s"rm -r $it").right.get
    )
    logger.success("cleaned robot files")
  }

  def sha1(file: String)
          (implicit c: SshClient): String =
    c.exec(s"sha1sum $file | cut -c 1-40").right.get.stdOutAsString()
}
