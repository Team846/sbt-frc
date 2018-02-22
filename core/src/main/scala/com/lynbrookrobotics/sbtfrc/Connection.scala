package com.lynbrookrobotics.sbtfrc

import com.decodified.scalassh._
import sbt.Keys._
import sbt.{Def, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

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

    val toTryList = addresses(CoreKeys.teamNumber.value)
    var remainingThatMightWork = toTryList.size
    val connectFutures = connect(toTryList)
    val retPromise = Promise[SshClient]()

    connectFutures.foreach { f =>
      f.onComplete { r =>
        retPromise.synchronized {
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
    }

    Await.result(
      retPromise.future,
      Duration.Inf
    )
  }
}
