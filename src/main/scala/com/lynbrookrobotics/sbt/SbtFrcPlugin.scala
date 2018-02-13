package com.lynbrookrobotics.sbt

import java.io.File
import java.nio.charset.StandardCharsets

import com.decodified.scalassh._
import fastparse.utils.Base64
import sbt.Keys._
import sbt._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import SbtUtils._

abstract class SbtFrcPlugin() {

  val keys: SbtFrcKeys

  lazy val connection: Def.Initialize[Task[SshClient]] = Def.task {
    implicit val logger: Logger = streams.value.log
    val connections = RoboRIO.connect(keys.teamNumber.value)
    Await.result(
      connections.reduce((a, b) => a.fallbackTo(b)),
      Duration.Inf
    )
  }

  def sendFile(file: File, targetPath: String)
              (implicit logger: Logger, client: SshClient): Unit = {
    val hash = sha1(file)
    logger.info(s"sending ${file.getPath} to $targetPath")
    client.upload(file.getAbsolutePath, s"/tmp/$hash")
    client.exec(s"mv /tmp/$hash $targetPath").right.get

    if (hash != RoboRIO.sha1(targetPath)) {
      logger.err(s"local hash and target hash do not match")
    } else {
      logger.success(s"sent file")
    }
  }
}
