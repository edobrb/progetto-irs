package main

import scala.util.Try

object Args {
  def CONFIGURATION(implicit args: Array[String]): String = utils.Arguments.argOrException("config", Some.apply)(args)

  def WORKING_DIR(implicit args: Array[String]): String = utils.Arguments.argOrException("working_dir", Some.apply)(args)

  def DATA_FOLDER(implicit args: Array[String]): String = utils.Arguments.argOrException("data", Some.apply)(args)

  def PARALLELISM_DEGREE(implicit args: Array[String]): Int = utils.Arguments.argOrDefault("threads", v => Try(v.toInt).toOption, 4)(args)

  def REPETITIONS(implicit args: Array[String]): Range =
    utils.Arguments.argOrDefault("from", v => Try(v.toInt).toOption, 1)(args) to utils.Arguments.argOrDefault("to", v => Try(v.toInt).toOption, 100)(args)

  def MAKE_CHARTS(implicit args: Array[String]): Boolean = utils.Arguments.boolOrDefault("chart", default = true)(args)

  def SHOW_CHARTS(implicit args: Array[String]): Boolean = utils.Arguments.boolOrDefault("show", default = false)(args)

  def RUN_BEST(implicit args: Array[String]): Boolean = utils.Arguments.boolOrDefault("run", default = false)(args)

  def WRITE_OUTPUT(implicit args: Array[String]): Boolean = utils.Arguments.boolOrDefault("write", default = true)(args)

  def LOAD_OUTPUT(implicit args: Array[String]): Boolean = utils.Arguments.boolOrDefault("load", default = false)(args)

}
