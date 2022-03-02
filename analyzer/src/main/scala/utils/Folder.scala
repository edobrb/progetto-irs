package utils

import java.nio.file.{Files, Path, Paths}
import scala.util.Try

object Folder {
  def exists(path: String): Boolean = Files.exists(Paths.get(path)) && Files.isDirectory(Paths.get(path))

  def create(paths: String*): Seq[Try[Path]] = paths.map(path => Try(Files.createDirectories(Paths.get(path))))
}
