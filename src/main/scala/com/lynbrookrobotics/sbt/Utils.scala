package com.lynbrookrobotics.sbt

import java.io.File

import com.decodified.scalassh.SshClient
import sbt.Logger
import sbt.io.Hash

object Utils {
  def sendFile(file: File, targetPath: String)
              (implicit logger: Logger, client: SshClient): Unit = {
    val hash = localHash(file)
    logger.info(s"sending `${file.getPath}` to `$targetPath` on ${client.endpoint}")
    client.upload(file.getAbsolutePath, s"/tmp/$hash")
    client.exec(s"mv /tmp/$hash $targetPath")
    if (hash != remoteHash(targetPath)) {
      logger.err(s"local hash and target hash do not match!")
    } else {
      logger.success(s"sent!")
    }
  }

  def localHash(file: File): String = Hash.toHex(Hash(file))

  def remoteHash(file: String)(implicit c: SshClient): String = c.exec(s"md5sum $file").right.get.stdOutAsString()
}
