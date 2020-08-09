package com.example

import com.examples.proto.api.vet_store.{
  GetVetsRequest,
  GetVetsResponse,
  Specialty,
  Vet,
  ZioVetStore
}
import io.grpc.Status
import scalapb.zio_grpc.{ ServerMain, ServiceList }
import zio.{ ZEnv, ZIO }

object VetStoreService extends ZioVetStore.RVetsStore[ZEnv] {
  def getVets(request: GetVetsRequest): ZIO[zio.ZEnv, Status, GetVetsResponse] =
    ZIO.succeed(
      GetVetsResponse(vets =
        List(
          Vet(firstName = "Bob", lastName = "Marley", specialties = List(Specialty(name = "xray")))
        )
      )
    )
}

object VetStoreServer extends ServerMain {
  def services: ServiceList[zio.ZEnv] = ServiceList.add(VetStoreService)
}
