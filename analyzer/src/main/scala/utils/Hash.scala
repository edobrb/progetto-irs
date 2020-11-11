package utils

import java.math.BigInteger
import java.security.MessageDigest

object Hash {
  def sha256(str: String): String = String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8"))))

  def sha256(strs: Iterator[String]): String = {
    val sha = MessageDigest.getInstance("SHA-256")
    strs.map(_.getBytes("UTF-8")).foreach(sha.update)
    String.format("%032x", new BigInteger(1, sha.digest()))
  }
}
