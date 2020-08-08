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
  .in(file("petclinic-grpc-api"))
  .settings(commonSettings)
  .settings(
    name := "petclinic-gprc-api",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc" % "grpc-netty" % grpcVersion
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    )
  )

lazy val petclinicVetsStore = project
  .in(file("petclinic-vets-store"))
  .settings(commonSettings)
  .settings(
    name := "petclinic-vets-store",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % "1.0.0" % Test
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    )
  )
  .dependsOn(petclinicGrpcApi)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "pet-clinic",
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )
  .aggregate(petclinicVetsStore)
