package com.example

import com.example.domain.Pet
import com.example.domain.PetDaoMock
import com.example.domain.PetOwner
import com.example.domain.PetRepository
import com.example.domain.PetType
import com.example.model.{ Pet => MPet }
import com.example.model.{ Owner => MOwner }
import com.example.model.{ PetType => MPetType }
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.magnolia._
import zio.test.mock.Expectation._

object PetRepositorySpec extends DefaultRunnableSpec {

  def spec = suite("PetRepository")(
    testM("should return Pet for given id") {
      checkM(DeriveGen[MPet], DeriveGen[MPetType], DeriveGen[MOwner]) {
        (mPet, mPetType, mOwner) =>
          val petDao = PetDaoMock.FindById(anything, value(List((mPet, mPetType, mOwner))))

          assertM(PetRepository.findById(mPet.id))(
            equalTo(
              Some(
                Pet(
                  id = mPet.id,
                  name = mPet.name,
                  birthDate = mPet.birthDate,
                  `type` = PetType(mPetType.id, name = Option(mPetType.name)),
                  owner = PetOwner(
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
      checkM(Gen.listOfN(10)(DeriveGen[MPetType])) { (someMPetTypes) =>
        val petDao = PetDaoMock.GetPetTypes(value((someMPetTypes)))
        assertM(PetRepository.getPetTypes)(
          hasSameElements((someMPetTypes).map(pt => PetType(pt.id, Option(pt.name))))
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
