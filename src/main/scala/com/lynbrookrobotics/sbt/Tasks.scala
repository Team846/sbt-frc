package com.lynbrookrobotics.sbt

import java.io.ByteArrayInputStream

import com.decodified.scalassh._
import net.schmizz.sshj.xfer.{FileSystemFile, InMemorySourceFile}
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object Tasks {
  val remoteUser = "lvuser"
  val remoteHome = "/home/lvuser"
  val remoteJARMain = "/home/lvuser/FRCUserProgram.jar"
  val remoteJARDeps = "/home/lvuser/FRCUserProgram-deps.jar"

  lazy val rioHosts: Def.Initialize[Task[List[String]]] = Def.task {
    val teamNumber = Keys.teamNumber.value

    List(
      s"roboRIO-${Keys.teamNumber.value}-FRC.local", // mDNS
      s"roboRIO-${Keys.teamNumber.value}-FRC.lan", // mDNS
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
    val depsFileAlreadyUploaded = client.sftp { s =>
      val stat = s.statExistence(s"$remoteHome/last-deps-${assembledDeps.getName}")
      stat != null
    }.right.getOrElse(false)

    if (depsFileAlreadyUploaded) {
      logger.info(s"Not deploying $assembledDeps because already uploaded")
    } else {
      logger.info(s"Deploying $assembledDeps to $remoteUser@${client.config.hostName}:$remoteJARDeps")
      client.exec(s"rm $remoteHome/last-deps-*")
      client.upload(assembledDeps.absolutePath, s"$remoteHome/temp-deps.jar").right.get
      client.exec(s"cp $remoteHome/temp-deps.jar $remoteJARDeps")
      client.exec(s"cp $remoteHome/temp-deps.jar $remoteHome/last-deps-${assembledDeps.getName}")
      client.exec(s"rm $remoteHome/temp-deps.jar")
    }

    val mainFileAlreadyUploaded = client.sftp { s =>
      val stat = s.statExistence(s"$remoteHome/last-main-${assembledMain.getName}")
      stat != null
    }.right.getOrElse(false)

    if (mainFileAlreadyUploaded) {
      logger.info(s"Not deploying $assembledMain because already uploaded")
    } else {
      logger.info(s"Deploying $assembledMain to $remoteUser@${client.config.hostName}:$remoteJARMain")
      client.exec(s"rm $remoteHome/last-main-*")
      client.upload(assembledMain.absolutePath, s"$remoteHome/temp-main.jar").right.get
      client.exec(s"cp $remoteHome/temp-main.jar $remoteJARMain")
      client.exec(s"cp $remoteHome/temp-main.jar $remoteHome/last-main-${assembledMain.getName}")
      client.exec(s"rm $remoteHome/temp-main.jar")
    }

    logger.success("Copied JARs to roboRIO")
  }

  def restartCodeWithClient(logger: Logger, client: SshClient): Unit = {
    client.exec("killall netconsole-host").right.get
    client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
    logger.success("Restarted robot code")
  }

  lazy val restore: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")

        client.exec(s"mv $remoteHome/last-main-* $remoteJARDeps")

        restartCodeWithClient(logger, client)
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
