package com.example.domain

import com.example.model.{Owner => MOwner, OwnerDao, Pet => MPet, PetType => MPetType}
import zio.macros.accessible
import zio.{Task, URLayer, ZLayer}

import java.time.LocalDate

@accessible
object OwnerRepository {

  case class PetType(id: Int, name: String)

  case class Pet(
      id: Int = 0,
      name: String,
      birthDate: LocalDate,
      `type`: PetType
    )

  case class Owner(
      id: Int = 0,
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      address: Option[String] = None,
      city: Option[String] = None,
      telephone: Option[String] = None,
      pets: List[Pet] = List.empty
    )

  trait Service {
    def findById(ownerId: Int): Task[Option[Owner]]
    def findAll: Task[List[Owner]]
    def save(owner: Owner): Task[Owner]
  }

  val live: URLayer[OwnerDao, OwnerRepository] = ZLayer.fromService(dao =>
    new OwnerRepository.Service {
      def findById(ownerId: Int): Task[Option[Owner]] =
        dao
          .findById(ownerId)
          .map(os => merge(os).headOption)

      def findAll: zio.Task[List[Owner]] =
        dao.findAll.map(os => merge(os).toList)

      def save(owner: Owner): Task[Owner] = {
        val mOwner = MOwner(
          id = owner.id,
          firstName = owner.firstName.orNull,
          lastName = owner.lastName.orNull,
          address = owner.address.orNull,
          city = owner.city.orNull,
          telephone = owner.telephone.orNull
        )

        if (mOwner.id == 0) {
          dao.insert(mOwner).map(io => owner.copy(id = io.id))
        }
        else {
          dao.update(mOwner).as(owner)
        }
      }

      private def merge(owners: List[(MOwner, Option[(MPet, MPetType)])]) =
        owners.groupMapReduce { case (mOwner, _) => mOwner.id } {
          case (mOwner, maybePet) =>
            Owner(
              id = mOwner.id,
              firstName = Option(mOwner.firstName),
              lastName = Option(mOwner.lastName),
              address = Option(mOwner.address),
              city = Option(mOwner.city),
              telephone = Option(mOwner.telephone),
              pets = maybePet.fold(List.empty[Pet]) {
                case (mPet, mPetType) =>
                  List(
                    Pet(
                      id = mPet.id,
                      name = mPet.name,
                      birthDate = mPet.birthDate,
                      `type` = PetType(id = mPetType.id, name = mPetType.name)
                    )
                  )
              }
            )
        } { case (first, second) => first.copy(pets = first.pets ++ second.pets) }.values
    }
  )

}
