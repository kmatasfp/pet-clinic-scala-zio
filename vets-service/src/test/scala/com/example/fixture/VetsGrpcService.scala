package com.example.fixture

import com.examples.proto.api.vets_service.ZioVetsService
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel
import zio.UIO
import zio.ZLayer
import zio.ZManaged
import zio.macros.accessible

@accessible
object VetsGrpcService {
  trait Service {
    def port: UIO[Int]
    def client: UIO[ZioVetsService.VetsClient.Service]
  }

  def live: ZLayer[OpenPortFinder, Throwable, VetsGrpcService] =
    ZLayer.fromManaged(for {
      p <- ZManaged.fromEffect(OpenPortFinder.find)
      c <- ZioVetsService
        .VetsClient
        .managed(
          ZManagedChannel(
            ManagedChannelBuilder.forAddress("localhost", p).usePlaintext()
          )
        )
    } yield {
      new VetsGrpcService.Service {
        def client: UIO[ZioVetsService.VetsClient.Service] = UIO.effectTotal(c)
        def port: UIO[Int] = UIO.effectTotal(p)
      }
    })

}
