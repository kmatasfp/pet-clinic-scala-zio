package com.example

import com.example.domain.PetRepository.{Owner, Pet, PetType}
import com.example.domain.{PetDaoMock, PetRepository}
import com.example.model.{Owner => MOwner, Pet => MPet, PetType => MPetType}
import zio.Has
import zio.random.Random
import zio.test.Assertion._
import zio.test.magnolia._
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, _}

object PetRepositorySpec extends DefaultRunnableSpec {

  def spec
      : Spec[Has[TestConfig.Service] with Has[Random.Service] with Has[Sized.Service], TestFailure[Throwable], TestSuccess] =
    suite("PetRepository.live")(
      testM("should return Pet for given id") {
        checkM(DeriveGen[MPet], DeriveGen[MPetType], DeriveGen[MOwner]) { (mPet, mPetType, mOwner) =>
          val petDao = PetDaoMock.FindById(anything, value(List((mPet, mPetType, mOwner))))

          assertM(PetRepository.findById(mPet.id))(
            equalTo(
              Some(
                Pet(
                  id = mPet.id,
                  name = mPet.name,
                  birthDate = mPet.birthDate,
                  `type` = PetType(mPetType.id, name = Option(mPetType.name)),
                  owner = Owner(
                    id = mOwner.id,
                    firstName = Option(mOwner.firstName),
                    lastName = Option(mOwner.lastName),
                    address = Option(mOwner.address),
                    city = Option(mOwner.city),
                    telephone = Option(mOwner.telephone)
                  )
                )
              )
            )
          ).provideLayer(
            petDao >>> PetRepository.live
          )
        }

      },
      testM("should return PetTypes") {
        checkM(Gen.listOfN(10)(DeriveGen[MPetType])) { someMPetTypes =>
          val petDao = PetDaoMock.GetPetTypes(value(someMPetTypes))
          assertM(PetRepository.getPetTypes)(
            hasSameElements(someMPetTypes.map(pt => PetType(pt.id, Option(pt.name))))
          ).provideLayer(
            petDao >>> PetRepository.live
          )
        }

      },
      testM("should save Pet") {
        checkM(DeriveGen[Pet], Gen.anyInt) { (pet, autoGenPetId) =>
          val petDao = PetDaoMock.Save(
            anything,
            valueF(p => MPet(id = autoGenPetId, p.name, p.birthDate, p.typeId, p.ownerId))
          )

          assertM(PetRepository.save(pet))(
            equalTo(pet.copy(id = autoGenPetId))
          ).provideLayer(
            petDao >>> PetRepository.live
          )

        }

      }
    )

}
