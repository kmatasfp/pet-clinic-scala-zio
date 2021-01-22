package com.example.domain

import java.time.LocalDate

import com.example.model.PetDao
import zio.Task
import zio.URLayer
import zio.ZLayer
import zio.macros.accessible

case class PetOwner(
    firstName: String,
    lastName: String,
    address: String,
    city: String,
    telephone: String
  )

case class PetType(id: Int, name: String)

case class Pet(
    id: Int = 0,
    name: String,
    birthDate: LocalDate,
    `type`: PetType,
    owner: PetOwner
  )
@accessible
object PetRepository {

  trait Service {
    def findById(petId: Int): Task[Option[Pet]]
    def getPetTypes: Task[List[PetType]]
  }

  val live: URLayer[PetDao, PetRepository] = ZLayer.fromService(dao =>
    new PetRepository.Service {
      def findById(petId: Int): zio.Task[Option[Pet]] =
        dao
          .findById(petId)
          .map(_.headOption.map {
            case (p, pt, po) =>
              Pet(
                p.id,
                p.name,
                p.birthDate,
                PetType(pt.id, pt.name),
                PetOwner(po.firstName, po.lastName, po.address, po.city, po.telephone)
              )
          })

      def getPetTypes: zio.Task[List[PetType]] =
        dao.getPetTypes.map(pts => pts.map(pt => PetType(pt.id, pt.name)))
    }
  )
}
