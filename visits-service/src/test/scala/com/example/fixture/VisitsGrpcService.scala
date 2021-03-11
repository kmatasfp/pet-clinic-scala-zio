package com.example.fixture

import com.examples.proto.api.visits_service.ZioVisitsService
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel
import zio.macros.accessible
import zio.{UIO, ZLayer, ZManaged}

@accessible
object VisitsGrpcService {
  trait Service {
    def port: UIO[Int]
    def client: UIO[ZioVisitsService.VisitsClient.Service]
  }

  val live: ZLayer[OpenPortFinder, Throwable, VisitsGrpcService] =
    ZLayer.fromManaged(for {
      p <- ZManaged.fromEffect(OpenPortFinder.find)
      c <- ZioVisitsService
        .VisitsClient
        .managed(
          ZManagedChannel(
            ManagedChannelBuilder.forAddress("localhost", p).usePlaintext()
          )
        )
    } yield new VisitsGrpcService.Service {
      def client: UIO[ZioVisitsService.VisitsClient.Service] = UIO.effectTotal(c)
      def port: UIO[Int] = UIO.effectTotal(p)
    })
}
