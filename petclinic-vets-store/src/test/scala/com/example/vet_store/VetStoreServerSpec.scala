package com.example.vet_store

import zio.test._
import Assertion.hasSameElements
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel

object VetStoreServerSpec extends DefaultRunnableSpec {
  def spec = suite("VetStoreServer")(
    testM("should return list of Vets") {

      val vets = PetStoreServer
        .run(List.empty)
        .forkManaged
        .use_(ZioVetStore.VetsStoreClient.getVets(GetVetsRequest()).map(_.vets))

      assertM(vets)(
        hasSameElements(
          List(
            Vet(
              firstName = "Bob",
              lastName = "Marley",
              specialties = List(Specialty(name = "xray"))
            )
          )
        )
      ).provideCustomLayer(
        ZioVetStore
          .VetsStoreClient
          .live(
            ZManagedChannel(
              ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext()
            )
          )
      )

    }
  )
}
