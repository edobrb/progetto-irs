name := "analyzer"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.1"

libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"

libraryDependencies += "org.knowm.xchart" % "xchart" % "3.6.4"

libraryDependencies ++= Seq(
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.6.2",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.6.2" % "provided" // or "provided", but it is required only in compile-time
)