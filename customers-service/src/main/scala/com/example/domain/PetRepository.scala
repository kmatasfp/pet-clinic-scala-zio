package com.example.domain

import com.example.model.PetDao
import zio.{ RIO, Task, URLayer, ZLayer }
import java.time.LocalDate

case class PetOwner(
    firstName: String,
    lastName: String,
    address: String,
    city: String,
    telephone: String
  )

case class PetType(name: String)

case class Pet(
    id: Int = 0,
    name: String,
    birthDate: LocalDate,
    `type`: PetType,
    owner: PetOwner
  )

object PetRepository {

  trait Service {
    def findById(petId: Int): Task[Option[Pet]]
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
                PetType(pt.name),
                PetOwner(po.firstName, po.lastName, po.address, po.city, po.telephone)
              )
          })
    }
  )

  def findById(petId: Int): RIO[PetRepository, Option[Pet]] =
    RIO.accessM(_.get.findById(petId))

}
