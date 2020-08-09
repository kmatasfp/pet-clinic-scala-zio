package com.example.model.repository

import com.example.model.Vet
import zio.Task
import zio.ZLayer
import zio.URLayer
import zio.Has

object VetsRepository {

  type DbTransactor = Has[DbTransactor.Resource]
  type CitiesRepository = Has[VetsRepository.Service]

  trait Service {
    def all: Task[List[Vet]]
  }

  val live: URLayer[DbTransactor, VetsRepository] =
    ZLayer.fromService(resource => Database(resource.xa))
}
