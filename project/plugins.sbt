addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC4")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.4.2"

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.26")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
