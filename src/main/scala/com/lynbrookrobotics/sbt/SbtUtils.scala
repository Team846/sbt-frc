package com.lynbrookrobotics.sbt

import java.io.File
import java.nio.charset.StandardCharsets

import fastparse.utils.Base64
import sbt.Hash

object SbtUtils {
  def b64(s: String): String = Base64.Encoder(s.getBytes(StandardCharsets.UTF_8)).toBase64
  def sha1(file: File): String = Hash.toHex(Hash(file))
}
