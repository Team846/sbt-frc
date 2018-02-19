package com.lynbrookrobotics.sbt

import java.io.File
import java.net.{DatagramPacket, DatagramSocket}

import com.decodified.scalassh._
import com.lynbrookrobotics.sbt.SbtUtils._
import sbt.Keys._
import sbt.{Def, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

abstract class RoboRio(keys: SbtFrcKeys) {

  val home = "/home/lvuser"

  val deployCode: Def.Initialize[Task[Unit]]

  def sha1(file: String)
          (implicit c: SshClient): String =
    c.exec(s"sha1sum $file | cut -c 1-40").right.get.stdOutAsString().trim

  object Connection {
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
        case Left(t) =>
          logger.err(s"connection to ${config.hostName} failed")
          Left(t)
      }
    }

    lazy val connectTsk: Def.Initialize[Task[SshClient]] = Def.task {
      implicit val logger = streams.value.log

      val toTryList = addresses(keys.teamNumber.value)
      var remainingThatMightWork = toTryList.size
      val connectFutures = connect(toTryList)
      val retPromise = Promise[SshClient]()

      connectFutures.foreach { f =>
        f.onComplete { r =>
          if (!retPromise.isCompleted) {
            r match {
              case Success(client) =>
                retPromise.success(client)
              case Failure(ex) =>
                remainingThatMightWork -= 1
                if (remainingThatMightWork == 0) {
                  retPromise.failure(new Exception("Could not connect to roboRIO"))
                }
            }
          }
        }
      }

      Await.result(
        retPromise.future,
        Duration.Inf
      )
    }
  }

  object Deployment {
    val dir = s"$home/marked-versions"

    lazy val markRobotCodeVersionTsk = Def.task(
      markRobotCodeVersion(keys.trackedFiles.value)(
        Connection.connectTsk.value, streams.value.log)
    )

    def markRobotCodeVersion(trackedFiles: Set[String])
                            (implicit client: SshClient, logger: Logger): Unit = {
      client.exec(s"rm -r $dir").right.get
      client.exec(s"mkdir $dir").right.get

      trackedFiles.foreach { it =>
        val encodedName = SbtUtils.b64(it)
        client.exec(s"cp $it /tmp/$encodedName").right.get
        client.exec(s"mv /tmp/$encodedName $dir/$encodedName").right.get
      }
      logger.success("marked version")
    }

    lazy val restoreRobotCodeVersionTsk = Def.task(
      restoreRobotCodeVersion(keys.trackedFiles.value)(
        Connection.connectTsk.value, streams.value.log
      )
    )

    def restoreRobotCodeVersion(trackedFiles: Set[String])
                               (implicit client: SshClient, logger: Logger): Unit = {
      trackedFiles.foreach { it =>
        val encodedName = b64(it)
        client.exec(s"cp $dir/$encodedName /tmp/$encodedName").right.get
        client.exec(s"mv /tmp/$encodedName $it").right.get
        logger.success("restored version")
      }
    }

    lazy val deleteRobotCodeTsk = Def.task(
      deleteRobotCode(keys.trackedFiles.value)(
        Connection.connectTsk.value, streams.value.log
      )
    )

    def deleteRobotCode(trackedFiles: Set[String])
                       (implicit client: SshClient, logger: Logger): Unit = {
      client.exec(s"rm -r $dir")
      trackedFiles.foreach(
        it => client.exec(s"rm -r $it").right.get
      )
      logger.success("cleaned robot files")
    }

    def sendFile(file: File, targetPath: String)
                (implicit logger: Logger, client: SshClient): Unit = {
      val localHash = SbtUtils.sha1(file)
      val remoteHash = sha1(targetPath)

      if (localHash != remoteHash) {
        logger.info(s"sending ${file.getPath} to $targetPath")
        client.upload(file.getAbsolutePath, s"/tmp/$localHash").right.get
        client.exec(s"mv /tmp/$localHash $targetPath").right.get

        if (localHash != sha1(targetPath)) { // corruption check
          logger.err(s"local hash and target hash do not match")
        } else {
          logger.success(s"sent file, hash: $localHash")
        }

      } else {
        logger.success(s"${file.getPath} already sent to $targetPath")
      }
    }
  }

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

}