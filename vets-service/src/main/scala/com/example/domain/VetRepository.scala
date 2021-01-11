package com.example.domain

import com.example.model.VetDao
import zio.RIO
import zio.Task
import zio.URLayer
import zio.ZLayer

case class Specialty(name: String)

case class Vet(
    id: Int,
    fistName: String,
    lastName: String,
    specalities: List[Specialty]
  )

object VetRepository {
  trait Service {
    def all: Task[List[Vet]]
  }

  val live: URLayer[VetDao, VetRepository] = ZLayer.fromService(dao =>
    new VetRepository.Service {
      def all: zio.Task[List[Vet]] =
        dao
          .findAll
          .map(_.groupMapReduce { case (vet, _) => vet.id } {
            case (vet, maybeSpecialty) =>
              Vet(
                vet.id,
                vet.firstName,
                vet.lastName,
                maybeSpecialty.fold(List.empty[Specialty])(s => List(Specialty(s.name)))
              )
          } {
            case (first, second) =>
              first.copy(specalities = first.specalities ++ second.specalities)
          }.values.toList)
    }
  )

  def all: RIO[VetRepository, List[Vet]] =
    RIO.accessM(_.get.all)
}
