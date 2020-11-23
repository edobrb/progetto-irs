package utils

import java.io.{InputStream, OutputStream}
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

object RichSocket {

  implicit class RichSocket(socket: Socket) {

    private def in: InputStream = socket.getInputStream

    private def out: OutputStream = socket.getOutputStream

    def writeStr(s: String, charset: Charset = StandardCharsets.UTF_8): Unit = {
      val bytes = s.getBytes(charset)
      write(bytes)
    }

    def readStr(charset: Charset = StandardCharsets.UTF_8): String = {
      val bytes = read()
      new String(bytes, charset)
    }

    def write(message: Array[Byte]): Unit = {
      out.write(ByteBuffer.allocate(4).putInt(message.length).array())
      out.write(message)
      out.flush()
    }

    def read(): Array[Byte] = {
      val byteLength = in.readNBytes(4)
      val length = ByteBuffer.wrap(byteLength).getInt
      in.readNBytes(length)
    }
  }

}
