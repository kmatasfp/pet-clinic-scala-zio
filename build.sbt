lazy val commonSettings = Seq(
  version := "1.0.0",
  organization := "com.example",
  scalaVersion := "2.13.3",
  scalacOptions ++= Seq(
    "-explaintypes",
    "-deprecation",
    "-feature",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Wunused",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:stars-align",
    "-Xlint:nonlocal-return",
    "-Xlint:constant",
    "-Xlint:adapted-args"
  )
)

val grpcVersion = "1.30.2"

lazy val petclinicGrpcApi = project
  .in(file("proto"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile := Seq(file("proto/src/main/protobuf"))
  )

lazy val petclinicVetsStore = project
  .in(file("petclinic-vets-store"))
  .settings(commonSettings)
  .settings(
    name := "petclinic-vets-store",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
      "io.grpc" % "grpc-netty" % grpcVersion,
      "io.getquill" %% "quill-jdbc" % "3.5.2",
      "org.tpolecat" %% "doobie-core" % "0.9.0",
      "org.tpolecat" %% "doobie-h2" % "0.9.0",
      "org.tpolecat" %% "doobie-quill" % "0.9.0",
      "dev.zio" %% "zio-test" % "1.0.0" % Test,
      "com.h2database" % "h2" % "1.4.200" % Test
    )
  )
  .dependsOn(petclinicGrpcApi)
