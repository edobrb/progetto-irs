package utils

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.{Deflater, GZIPInputStream, GZIPOutputStream}

import scala.io.BufferedSource
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object File {

  trait Done

  object Done extends Done

  def read(fileName: String): Try[String] = {
    Try(scala.io.Source.fromFile(fileName)).map(source => {
      val result = Try(source.mkString)
      source.close()
      result
    }).flatten
  }

  def readLines(fileName: String): Try[(Seq[String], BufferedSource)] = {
    Try(scala.io.Source.fromFile(fileName)).flatMap(source => {
      Try((source.getLines().to(LazyList), source))
    })
  }

  def writeGzippedLines(filename: String, data: Iterator[String], level: Int = Deflater.DEFAULT_COMPRESSION): Try[Long] =
    Try {
      val fos = new FileOutputStream(filename)
      class MyGZIPOutputStream(out: OutputStream) extends GZIPOutputStream(out) {
        `def`.setLevel(level)
      }
      var count: Long = 0
      val gzos = new MyGZIPOutputStream(fos)
      val w = new PrintWriter(gzos)
      data.foreach(line => {
        count = count + 1
        w.write(line + "\n")
      })
      w.flush()
      w.close()
      gzos.close()
      fos.close()
      count
    }

  def readGzippedLines(fileName: String): Try[(Iterator[String], BufferedSource)] = {
    Try(scala.io.Source.fromFile(fileName)).flatMap(source => Try {
      val fileStream = new FileInputStream(fileName)
      val gzipStream = new GZIPInputStream(fileStream)
      val decoder = new InputStreamReader(gzipStream)
      val buffered = new BufferedReader(decoder)
      val res = buffered.lines().iterator().asScala
      (res, source)
    })
  }

  def readGzippedLinesAndMap[T](fileName: String)(mapper: Iterator[String] => T): Try[T] = {
    readGzippedLines(fileName).map {
      case (value, source) =>
        val res = mapper(value)
        source.close()
        res
    }
  }

  def exists(fileName: String): Boolean = java.nio.file.Files.exists(Paths.get(fileName))

  def write(fileName: String, data: String): Try[Done] =
    Try(java.nio.file.Files.write(Paths.get(fileName), data.getBytes(StandardCharsets.UTF_8))).map(_ => Done)

  def writeLines(fileName: String, data: Seq[String]): Try[Done] =
    Try(java.nio.file.Files.write(Paths.get(fileName), data.mkString("\n").getBytes(StandardCharsets.UTF_8))).map(_ => Done)

  def append(fileName: String, data: String): Try[Done] =
    Try(new FileWriter(fileName, true)).flatMap(source => Try {
      source.write(data)
      source.close()
      Done
    })

  def move(source: String, destination: String): Try[Done] =
    java.nio.file.Files.move(
      Paths.get(source),
      Paths.get(destination),
      java.nio.file.StandardCopyOption.ATOMIC_MOVE
    ) match {
      case null => Failure(new Exception(s"Can't move $source to $destination"))
      case _ => Success(Done)
    }
}