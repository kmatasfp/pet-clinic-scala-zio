package com.example

import cats.syntax.apply._
import cats.instances.option._
import com.example.config.Configuration.DbConfig
import com.example.domain.VetRepository
import com.example.model.VetDao
import com.example.model.DbTransactor
import com.examples.proto.api.vets_service.{
  GetVetsRequest,
  GetVetsResponse,
  Specialty,
  Vet,
  ZioVetsService
}
import io.grpc.Status
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.ServerLayer
import zio.{ system, ExitCode, Has, IO, URLayer, ZEnv, ZIO, ZLayer }
import zio.console.putStrLn

object VetsService {

  val live: URLayer[VetRepository, Has[ZioVetsService.Vets]] =
    ZLayer.fromService(repository =>
      new ZioVetsService.Vets {
        def getVets(request: GetVetsRequest): IO[Status, GetVetsResponse] =
          repository
            .all
            .map(vs =>
              GetVetsResponse(vets =
                vs.map(v =>
                  Vet(
                    id = v.id,
                    firstName = v.fistName,
                    lastName = v.lastName,
                    specialties = v.specalities.map(s => Specialty(name = s.name))
                  )
                )
              )
            )
            .mapError(_ => Status.INTERNAL)
      }
    )

}

object VetsServiceServer extends zio.App {

  def welcome: ZIO[ZEnv, Throwable, Unit] =
    putStrLn("Server is running. Press Ctrl-C to stop.")

  val app = system.envOrElse("server.port", "9000").map(_.toInt).map { port =>
    def builder = ServerBuilder.forPort(port).addService(ProtoReflectionService.newInstance())

    val server =
      ServerLayer.fromServiceLayer(builder)(VetsService.live)

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

    ((dbConf >>> DbTransactor.live >>> VetDao.mySql >>> VetRepository.live) ++ ZEnv.any) >>> server
  }

  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] =
    (welcome *> app.flatMap(_.build.useForever)).exitCode
}
