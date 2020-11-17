package utils

import scala.util.Try

object Arguments {
  def argOrDefault[T](argName: String, f: String => Option[T], default: T)(args: Array[String]): T =
    argOrException(argName, f, Some(default))(args)

  def argOrException[T](argName: String, f: String => Option[T], default: Option[T] = None)(args: Array[String]): T =
    args.find(a => Try {
      a.split('=')(0) == argName
    }.toOption.contains(true)).flatMap(a => Try {
      a.split('=')(1)
    }.toOption).flatMap(f) match {
      case Some(value) => value
      case None => default match {
        case Some(value) => value
        case None => throw new Exception(s"Argument $argName not defined")
      }
    }

  def boolOrDefault(argName: String, default: Boolean)(args: Array[String]): Boolean = argOrDefault(argName, v => Try(v.toBoolean).toOption, default)(args)
}
