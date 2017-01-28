package com.lynbrookrobotics.sbt

import com.decodified.scalassh._
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyKeys

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

object Tasks {
  val remoteUser = "lvuser"
  val remoteJAR = "/home/lvuser/FRCUserProgram.jar"

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

  def firstWorkingConnection(hosts: List[String], logger: Logger): Future[SshClient] = {
    hosts match {
      case head :: tail =>
        val config = HostConfig(
          PasswordLogin(
            remoteUser,
            SimplePasswordProducer("")
          ),
          hostName = head,
          hostKeyVerifier = HostKeyVerifiers.DontVerify,
          connectTimeout = Some(10000)
        )

        Future(attemptConnection(config, logger).get).
          fallbackTo(firstWorkingConnection(tail, logger))

      case _ =>
        Future.failed(new Exception("Could not connect to roboRIO"))
    }
  }

  lazy val rioConnection: Def.Initialize[Task[Try[SshClient]]] = Def.task {
    Try(Await.result(
      firstWorkingConnection(rioHosts.value, streams.value.log), Duration.Inf
    ))
  }

  def deployJAR(logger: Logger, client: SshClient, assembledFile: File): Unit = {
    logger.info(s"Deploying $assembledFile to $remoteUser@${client.config.hostName}:$remoteJAR")

    client.upload(assembledFile.absolutePath, remoteJAR).right.get
    logger.success("Copied JAR to roboRIO")
  }

  def restartCodeWithClient(logger: Logger, client: SshClient): Unit = {
    client.exec("killall netconsole-host").right.get
    client.exec(". /etc/profile.d/natinst-path.sh; /usr/local/frc/bin/frcKillRobot.sh -t -r").right.get
    logger.success("Restarted robot code")
  }

  lazy val deploy: Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log

    rioConnection.value match {
      case Success(client) =>
        logger.success("Connected to roboRIO")
        deployJAR(logger, client, AssemblyKeys.assembly.value)
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
