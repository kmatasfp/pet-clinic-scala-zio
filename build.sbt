lazy val commonSettings = Seq(
  version := "1.0.0",
  organization := "com.example.petclinic",
  scalaVersion := "2.13.5",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  scalacOptions ++= Seq(
    "-Ymacro-annotations",
    "-explaintypes",
    "-deprecation",
    "-feature",
    "-Ywarn-dead-code",
    "-Wunused",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:stars-align",
    "-Xlint:nonlocal-return",
    "-Xlint:constant",
    "-Xlint:adapted-args",
    "-language:existentials",
    "-language:experimental.macros"
  ),
  ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value),
  ThisBuild / scalafixDependencies ++= List(
    "com.github.liancheng" %% "organize-imports" % "0.5.0",
    "com.github.vovapolu" %% "scaluzzi" % "0.1.16"
  )
)

val grpcVersion = "1.34.0"

lazy val petclinicGrpcApi = project
  .in(file("proto"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.4-2",
      "io.grpc" % "grpc-netty" % grpcVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb
        .compiler
        .Version
        .scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb
        .compiler
        .Version
        .scalapbVersion % "protobuf"
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile := Seq(file("proto/src/main/protobuf"))
  )

lazy val petclinicVetsService = project
  .in(file("vets-service"))
  .settings(commonSettings)
  .settings(
    name := "vets-service",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % "2.2.0.1",
      "dev.zio" %% "zio-macros" % "1.0.4-2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.getquill" %% "quill-jdbc" % "3.5.3",
      "org.tpolecat" %% "doobie-core" % "0.9.4",
      "org.tpolecat" %% "doobie-h2" % "0.9.4",
      "org.tpolecat" %% "doobie-quill" % "0.9.4",
      "mysql" % "mysql-connector-java" % "8.0.21",
      "dev.zio" %% "zio-test" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-sbt" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-magnolia" % "1.0.4-2" % Test,
      "com.dimafeng" %% "testcontainers-scala-core" % "0.38.7" % Test,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.38.7" % Test,
      "org.testcontainers" % "testcontainers" % "1.15.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(petclinicGrpcApi)

lazy val petclinicVisitsService = project
  .in(file("visits-service"))
  .settings(commonSettings)
  .settings(
    name := "visits-service",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
      "dev.zio" %% "zio-macros" % "1.0.4-2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.getquill" %% "quill-jdbc" % "3.5.2",
      "org.tpolecat" %% "doobie-core" % "0.9.4",
      "org.tpolecat" %% "doobie-h2" % "0.9.4",
      "org.tpolecat" %% "doobie-quill" % "0.9.4",
      "mysql" % "mysql-connector-java" % "8.0.21",
      "dev.zio" %% "zio-test" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-sbt" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-magnolia" % "1.0.4-2" % Test,
      "com.dimafeng" %% "testcontainers-scala-core" % "0.38.1" % Test,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.38.1" % Test,
      "org.testcontainers" % "testcontainers" % "1.15.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(petclinicGrpcApi)

lazy val petclinicCustomersService = project
  .in(file("customers-service"))
  .settings(commonSettings)
  .settings(
    name := "customers-service",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
      "dev.zio" %% "zio-macros" % "1.0.4-2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.getquill" %% "quill-jdbc" % "3.5.2",
      "org.tpolecat" %% "doobie-core" % "0.9.4",
      "org.tpolecat" %% "doobie-h2" % "0.9.4",
      "org.tpolecat" %% "doobie-quill" % "0.9.4",
      "mysql" % "mysql-connector-java" % "8.0.21",
      "dev.zio" %% "zio-test" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-sbt" % "1.0.4-2" % Test,
      "dev.zio" %% "zio-test-magnolia" % "1.0.4-2" % Test,
      "com.dimafeng" %% "testcontainers-scala-core" % "0.38.1" % Test,
      "com.dimafeng" %% "testcontainers-scala-mysql" % "0.38.1" % Test,
      "org.testcontainers" % "testcontainers" % "1.15.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(petclinicGrpcApi)
