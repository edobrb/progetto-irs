name := "analyzer"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.1"

libraryDependencies += "org.knowm.xchart" % "xchart" % "3.6.4"

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.1.1"

libraryDependencies += "de.erichseifert.vectorgraphics2d" % "VectorGraphics2D" % "0.13"

libraryDependencies += "de.rototor.pdfbox" % "graphics2d" % "0.30"

libraryDependencies ++= Seq(
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % "2.6.2" ,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.6.2" % "provided" // or "provided", but it is required only in compile-time
)

libraryDependencies ++= Seq(
  "com.github.julien-truffaut" %% "monocle-core"  % "2.0.3",
  "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3",
)

scalacOptions in Global += "-Ymacro-annotations"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

