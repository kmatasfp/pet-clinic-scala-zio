package com.example.domain

import java.time.LocalDate

import com.example.model.PetDao
import com.example.model.{ Pet => MPet }
import zio.Task
import zio.URLayer
import zio.ZLayer
import zio.macros.accessible

@accessible
object PetRepository {

  case class PetType(id: Int, name: Option[String] = None)
  case class Pet(
      id: Int = 0,
      name: String,
      birthDate: LocalDate,
      `type`: PetType,
      owner: Owner
    )
  case class Owner(
      id: Int = 0,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      address: Option[String] = None,
      city: Option[String] = None,
      telephone: Option[String] = None
    )

  trait Service {
    def findById(petId: Int): Task[Option[Pet]]
    def getPetTypes: Task[List[PetType]]
    def save(pet: Pet): Task[Pet]
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
                PetType(pt.id, Option(pt.name)),
                Owner(
                  po.id,
                  Option(po.firstName),
                  Option(po.lastName),
                  Option(po.address),
                  Option(po.city),
                  Option(po.telephone)
                )
              )
          })

      def getPetTypes: zio.Task[List[PetType]] =
        dao.getPetTypes.map(pts => pts.map(pt => PetType(pt.id, Option(pt.name))))

      def save(pet: Pet): zio.Task[Pet] =
        dao
          .save(
            MPet(
              name = pet.name,
              birthDate = pet.birthDate,
              typeId = pet.`type`.id,
              ownerId = pet.owner.id
            )
          )
          .map(saved => pet.copy(id = saved.id))
    }
  )
}
