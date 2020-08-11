package com.example

import cats.syntax.apply._
import cats.instances.option._
import com.examples.proto.api.vet_store.{
  GetVetsRequest,
  GetVetsResponse,
  Specialty,
  Vet,
  ZioVetStore
}
import io.grpc.Status
import scalapb.zio_grpc.{ ServerMain, ServiceList }
import zio.{ system, ZEnv, ZIO }
import zio.console.putStrLn
import com.example.domain.VetRepository
import com.example.model.VetDao
import zio.ZLayer
import zio.Ref
import zio.ExitCode
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import scalapb.zio_grpc.ServerLayer
import com.example.config.Configuration.DbConfig
import com.example.model.DbTransactor

object VetStoreService extends ZioVetStore.RVetsStore[ZEnv with VetRepository] {
  def getVets(request: GetVetsRequest): ZIO[zio.ZEnv with VetRepository, Status, GetVetsResponse] =
    VetRepository
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

object VetStoreServer extends zio.App {

  def welcome: ZIO[ZEnv, Throwable, Unit] =
    putStrLn("Server is running. Press Ctrl-C to stop.")

  def builder = ServerBuilder.forPort(9000).addService(ProtoReflectionService.newInstance())

  val server =
    ServerLayer.fromServiceLayer(builder)(VetStoreService.toLayer)

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

  val app =
    ((dbConf >>> DbTransactor.live >>> VetDao.mySql >>> VetRepository.live) ++ ZEnv.any) >>> server

  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] =
    (welcome *> app.build.useForever).exitCode
}
