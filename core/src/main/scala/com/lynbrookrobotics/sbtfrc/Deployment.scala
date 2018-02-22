package com.lynbrookrobotics.sbtfrc

import java.io.File

import com.decodified.scalassh.SshClient
import com.lynbrookrobotics.sbtfrc.SbtUtils.b64
import sbt.Keys.streams
import sbt.{Def, Logger}

object Deployment {
  val home = "/home/lvuser"
  val dir = s"$home/marked-versions"

  def sha1(file: String)
          (implicit c: SshClient): String =
    c.exec(s"sha1sum $file | cut -c 1-40").right.get.stdOutAsString().trim

  lazy val markRobotCodeVersionTsk = Def.task(
    markRobotCodeVersion(CoreKeys.trackedFiles.value)(
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
    restoreRobotCodeVersion(CoreKeys.trackedFiles.value)(
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
    deleteRobotCode(CoreKeys.trackedFiles.value)(
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
