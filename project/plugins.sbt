addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

val zioGrpcVersion = "0.4.0-RC3"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
