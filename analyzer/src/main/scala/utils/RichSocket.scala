package utils

import java.io.{InputStream, OutputStream}
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import java.util.concurrent.{BlockingDeque, Executors, LinkedBlockingDeque, TimeUnit}
import scala.util.{Failure, Success, Try}

case class RichSocket(socket: Socket, keepAliveMs: Int = 1000) {
  private val mailbox: BlockingDeque[Try[Array[Byte]]] = new LinkedBlockingDeque[Try[Array[Byte]]]()
  private val executor = Executors.newScheduledThreadPool(2)
  private val outputLock = new Object()

  socket.setSoTimeout(keepAliveMs * 2)
  executor.scheduleWithFixedDelay(() => {
    writePacket(0, Array.emptyByteArray)
  } , keepAliveMs, keepAliveMs, TimeUnit.MILLISECONDS)
  receive()

  private def receive(): Unit = executor.execute(() =>
    readPacket() match {
      case Failure(_) =>
      case Success((kind, data)) =>
        if (kind == 0) { //keep alive

        }
        if (kind == 1) { //message
          mailbox.addLast(Success(data))
        }
        receive()
    }
  )


  private def in: InputStream = socket.getInputStream

  private def out: OutputStream = socket.getOutputStream

  def writeStr(s: String, charset: Charset = StandardCharsets.UTF_8): Try[Unit] = {
    val bytes = s.getBytes(charset)
    write(bytes)
  }

  def readStr(charset: Charset = StandardCharsets.UTF_8): Try[String] =
    read().map(b => new String(b, charset))

  def write(data: Array[Byte]): Try[Unit] = writePacket(1, data)

  def read(): Try[Array[Byte]] = mailbox.takeFirst()

  private def writePacket(kind: Int, message: Array[Byte]): Try[Unit] = Try {
    outputLock.synchronized {
      out.write(ByteBuffer.allocate(4).putInt(kind).array())
      out.write(ByteBuffer.allocate(4).putInt(message.length).array())
      out.write(message)
      out.flush()
    }
  } match {
    case Failure(exception) =>
      mailbox.addLast(Failure(exception))
      richClose()
      Failure(exception)
    case Success(value) => Success(value)
  }

  private def readPacket(): Try[(Int, Array[Byte])] = Try {
    val kind = ByteBuffer.wrap(in.readNBytes(4)).getInt
    val length = ByteBuffer.wrap(in.readNBytes(4)).getInt
    (kind, in.readNBytes(length))
  }  match {
    case Failure(exception) =>
      mailbox.addLast(Failure(exception))
      richClose()
      Failure(exception)
    case Success(value) => Success(value)
  }

  def richClose(): Unit = {
    executor.shutdownNow()
    socket.close()
  }
}

