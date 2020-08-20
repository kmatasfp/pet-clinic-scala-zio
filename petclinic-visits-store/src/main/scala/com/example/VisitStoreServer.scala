package com.example

import cats.syntax.apply._
import cats.instances.option._
import com.example.model.VisitDao
import io.grpc.Status
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.ServerLayer
import zio.{ system, ExitCode, Has, IO, URLayer, ZEnv, ZIO, ZLayer }
import zio.console.putStrLn
import com.examples.proto.api.visit_store.ZioVisitStore
import com.examples.proto.api.visit_store.GetVisitsForPetRequest
import com.examples.proto.api.visit_store.GetVisitsForPetsRequest
import com.examples.proto.api.visit_store.GetVisitsResponse
import com.examples.proto.api.visit_store.Visit
import com.google.protobuf.timestamp.Timestamp
import java.time.LocalTime
import java.time.ZoneOffset
import com.example.config.Configuration.DbConfig
import com.example.model.DbTransactor

object VisitStoreService {

  val live: URLayer[VisitDao, Has[ZioVisitStore.VisitsStore]] =
    ZLayer.fromService(dao =>
      new ZioVisitStore.VisitsStore {
        def getVisitsForPet(request: GetVisitsForPetRequest): IO[Status, GetVisitsResponse] =
          dao
            .findByPetId(request.petId)
            .map(createResponse)
            .mapError(_ => Status.INTERNAL)

        def getVisitsForPets(request: GetVisitsForPetsRequest): IO[Status, GetVisitsResponse] =
          dao
            .findByPetIdIn(request.petIds)
            .map(createResponse)
            .mapError(_ => Status.INTERNAL)

        private def createResponse(vs: List[com.example.model.Visit]) =
          GetVisitsResponse(visits =
            vs.map(v =>
              Visit(
                id = v.id,
                petId = v.petId,
                visitDate = Some(
                  Timestamp.of(
                    seconds = v
                      .visitDate
                      .atTime(LocalTime.MIDNIGHT)
                      .atZone(ZoneOffset.UTC)
                      .toEpochSecond(),
                    nanos = 0
                  )
                ),
                description = v.description
              )
            )
          )
      }
    )
}

object VisitStoreServer extends zio.App {

  def welcome: ZIO[ZEnv, Throwable, Unit] =
    putStrLn("Server is running. Press Ctrl-C to stop.")

  val app = system.envOrElse("server.port", "9000").map(_.toInt).map { port =>
    val builder = ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())

    val server =
      ServerLayer.fromServiceLayer(builder)(VisitStoreService.live)

    val dbConf = ZLayer.fromEffect(
      ZIO
        .mapN(
          system.env("jdbc.driver.name"),
          system.env("jdbc.url"),
          system.env("db.user"),
          system.env("db.pass")
        )((_, _, _, _).mapN(DbConfig))
        .someOrFail(new NoSuchElementException)
    )

    ((dbConf >>> DbTransactor.live >>> VisitDao.mySql) ++ ZEnv.any) >>> server
  }
  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] =
    (welcome *> app.flatMap(_.build.useForever)).exitCode
}
