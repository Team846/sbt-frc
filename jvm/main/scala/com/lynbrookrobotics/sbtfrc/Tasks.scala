package com.lynbrookrobotics.sbtfrc

import java.net.{DatagramPacket, DatagramSocket}

import com.decodified.scalassh._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object Tasks {
  val remoteUser = "lvuser"
  val remoteHome = "/home/lvuser"
  val remoteMain = s"$remoteHome/FRCUserProgram.jar"
  val remoteDeps = s"$remoteHome/FRCUserProgram-deps.jar"
  val remoteConf = s"$remoteHome/robot-config.json"

  val remoteMainHash = s"$remoteHome/main-hash"
  val remoteDepsHash = s"$remoteHome/deps-hash"

  val remoteLastMain = s"$remoteHome/last-main"
  val remoteLastDeps = s"$remoteHome/last-deps"
  val remoteLastConf = s"$remoteHome/last-conf"

  val remoteTmpMain = "/tmp/main"
  val remoteTmpDeps = "/tmp/deps"

  lazy val rioHosts: Def.Initialize[Task[List[String]]] = Def.task {
    val teamNumber = Keys.teamNumber.value

    List(
      s"roboRIO-${Keys.teamNumber.value}-FRC.local", // mDNS
      s"roboRIO-${Keys.teamNumber.value}-FRC.lan", // DNS
      s"10.${teamNumber / 100}.${teamNumber % 100}.2", // Static IP,
      "172.22.11.2" // USB
    )
  }

  def attemptConnection(config: HostConfig, logger: Logger): Try[SshClient] = {
    logger.info(s"Attempting to connect to roboRIO @ ${config.hostName}")
    SshClient(config.hostName, config) match {
      case Right(client) =>
        client.authenticatedClient.right.toOption match {
          case Some(_) =>
            logger.success(s"Connected to roboRIO @ ${config.hostName}")
            Success(client)
          case None =>
            logger.error(s"Could not connect to roboRIO @ ${config.hostName}")
            client.close()
            Failure(new Exception("Could not connect to roboRIO"))
        }

      case Left(e) =>
        logger.error(e)
        Failure(new Exception("Could not connect to roboRIO"))
    }
  }

  def attemptAllConnections(hosts: List[String], logger: Logger): List[Future[SshClient]] = {
    hosts.map { h =>
      val config = HostConfig(
        PasswordLogin(
          remoteUser,
          SimplePasswordProducer("")
        ),
        hostName = h,
        hostKeyVerifier = HostKeyVerifiers.DontVerify,
        connectTimeout = Some(10000)
      )

      Future(attemptConnection(config, logger).get)
    }
  }

  lazy val rioConnection: Def.Initialize[Task[Try[SshClient]]] = Def.task {
    Try {
      val allConnectionsWorking = attemptAllConnections(rioHosts.value, streams.value.log)
      val firstWorking = allConnectionsWorking.reduce((a, b) =>
        a.fallbackTo(b)
      )

      val workingConnection = Await.result(
        firstWorking, Duration.Inf
      )

      allConnectionsWorking.foreach(_.foreach { c =>
        if (c.config.hostName != workingConnection.config.hostName) {
          c.close()
        }
      })

      workingConnection
    }
  }

  def deployJAR(logger: Logger, client: SshClient, assembledMain: File, assembledDeps: File): Unit = {
    val depsFileAlreadyUploaded = client.exec(s"cat $remoteDepsHash").right
      .exists(_.stdOutAsString().trim == assembledDeps.name)

    if (depsFileAlreadyUploaded) {
      logger.info(s"Not deploying $assembledDeps because already uploaded")
    } else {
      logger.info(s"Deploying $assembledDeps to $remoteUser@${client.config.hostName}:$remoteDeps")
      client.upload(assembledDeps.absolutePath, remoteTmpDeps).right.get
      client.exec(s"mv $remoteTmpDeps $remoteDeps")
      client.exec(s"echo ${assembledDeps.name} > $remoteDepsHash")
    }

    val mainFileAlreadyUploaded = client.exec(s"cat $remoteMainHash").right
      .exists(_.stdOutAsString().trim == assembledMain.name)

    if (mainFileAlreadyUploaded) {
      logger.info(s"Not deploying $assembledMain because already uploaded")
    } else {
      logger.info(s"Deploying $assembledMain to $remoteUser@${client.config.hostName}:$remoteMain")
      client.upload(assembledMain.absolutePath, remoteTmpMain).right.get
      client.exec(s"mv $remoteTmpMain $remoteMain")
      client.exec(s"echo ${assembledMain.name} > $remoteMainHash")
    }

    logger.success("Copied JARs to roboRIO")
  }

  def restartCodeWithClient(logger: Logger, client: SshClient): Unit = {
    client.exec("killall netconsole-host").right.get
    client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
    logger.success("Restarted robot code")
  }

  lazy val roboClean: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")

        client.exec(s"rm $remoteDeps $remoteDepsHash")
        client.exec(s"rm $remoteMain $remoteMainHash")
        client.exec(s"rm $remoteConf")

        client.close()
        logger.success("Deleted latest jars and config.")

      case Failure(_) =>
        logger.error("Could not connect to roboRIO")
    }
  }

  lazy val itWorks: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")

        client.exec(s"cp $remoteDeps $remoteLastDeps")
        client.exec(s"cp $remoteMain $remoteLastMain")
        client.exec(s"cp $remoteConf $remoteLastConf")

        client.close()
        logger.success("Saved latest jars and config for restoration later.")

      case Failure(_) =>
        logger.error("Could not connect to roboRIO")
    }
  }

  lazy val restore: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")

        val revertible = client.sftp { s =>
          s.statExistence(remoteMainHash) != null && s.statExistence(remoteDepsHash) != null
        }.right.getOrElse(false)

        if (revertible) {
          client.exec(s"cp $remoteLastMain $remoteMain")
          client.exec(s"cp $remoteLastDeps $remoteDeps")
          client.exec(s"cp $remoteLastConf $remoteConf")
          client.exec(s"rm $remoteMainHash")
          client.exec(s"rm $remoteDepsHash")

          logger.success("Restored working jars and config.")

          restartCodeWithClient(logger, client)
        } else {
          logger.error("No working jars/config have been saved.")
        }

        client.close()

      case Failure(_) =>
        logger.error("Could not connect to roboRIO")
    }
  }

  lazy val deploy: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")
        deployJAR(
          logger, client,
          AssemblyKeys.assembly.value,
          AssemblyKeys.assemblyPackageDependency.value
        )
        restartCodeWithClient(logger, client)
        client.close()

      case Failure(_) =>
        logger.error("Could not connect to roboRIO")
    }
  }

  lazy val robotConsole: Def.Initialize[Task[Unit]] = Def.task {
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

  lazy val restartCode: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")
        restartCodeWithClient(logger, client)
        client.close()

      case Failure(_) =>
        logger.error("Could not connect to roboRIO")
    }
  }
}
