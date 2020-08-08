package com.example.vet_store

import io.grpc.Status
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio.{ ZEnv, ZIO }

object PetStoreService extends ZioVetStore.RVetsStore[ZEnv] {
  def getVets(request: GetVetsRequest): ZIO[zio.ZEnv, Status, GetVetsResponse] =
    ZIO.succeed(
      GetVetsResponse(vets =
        List(
          Vet(firstName = "Bob", lastName = "Marley", specialties = List(Specialty(name = "xray")))
        )
      )
    )
}

object PetStoreServer extends ServerMain {
  def services: ServiceList[zio.ZEnv] = ServiceList.add(PetStoreService)
}
