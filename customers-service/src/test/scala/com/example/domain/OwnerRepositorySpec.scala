package com.example.domain

import com.example.domain.OwnerRepository.Owner
import com.example.domain.OwnerRepository.Pet
import com.example.domain.OwnerRepository.PetType
import com.example.model.{ Owner => MOwner }
import com.example.model.{ Pet => MPet }
import com.example.model.{ PetType => MPetType }
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.magnolia.DeriveGen
import zio.test.mock.Expectation._

object OwnerRepositorySpec extends DefaultRunnableSpec {

  def spec = suite("OwnerRepository.live")(
    testM("should return Owner for given id") {

      checkM(DeriveGen[MOwner], Gen.listOf(DeriveGen[MPet] <&> DeriveGen[MPetType])) {
        (mOwner, mPets) =>
          val ownerDao = {
            if (mPets.isEmpty)
              OwnerDaoMock.FindById(anything, value(List((mOwner, None))))
            else {
              OwnerDaoMock.FindById(anything, value(mPets.map(pet => (mOwner, Some(pet)))))
            }
          }
          assertM(OwnerRepository.findById(mOwner.id))(
            equalTo(
              Some(
                Owner(
                  id = mOwner.id,
                  firstName = Option(mOwner.firstName),
                  lastName = Option(mOwner.lastName),
                  address = Option(mOwner.address),
                  city = Option(mOwner.city),
                  telephone = Option(mOwner.telephone),
                  pets = mPets.map {
                    case (mPet, mPetType) =>
                      Pet(
                        id = mPet.id,
                        name = mPet.name,
                        birthDate = mPet.birthDate,
                        `type` = PetType(id = mPetType.id, name = mPetType.name)
                      )
                  }
                )
              )
            )
          ).provideLayer(
            ownerDao >>> OwnerRepository.live
          )
      }
    },
    testM("should return all Owners") {
      checkM(Gen.listOf(DeriveGen[MOwner] <&> Gen.listOf(DeriveGen[MPet] <&> DeriveGen[MPetType]))) {
        mOwners =>
          val ownerDao = OwnerDaoMock.FindAll(
            value(
              mOwners.flatMap {
                case (mOwner, Nil)   => List((mOwner, None))
                case (mOwner, mPets) => mPets.map(pet => (mOwner, Some(pet)))
              }
            )
          )

          assertM(OwnerRepository.findAll)(
            hasSameElements(
              mOwners.map {
                case (mOwner, mPets) =>
                  Owner(
                    id = mOwner.id,
                    firstName = Option(mOwner.firstName),
                    lastName = Option(mOwner.lastName),
                    address = Option(mOwner.address),
                    city = Option(mOwner.city),
                    telephone = Option(mOwner.telephone),
                    pets = mPets.map {
                      case (mPet, mPetType) =>
                        Pet(
                          id = mPet.id,
                          name = mPet.name,
                          birthDate = mPet.birthDate,
                          `type` = PetType(id = mPetType.id, name = mPetType.name)
                        )
                    }
                  )
              }
            )
          ).provideLayer(
            ownerDao >>> OwnerRepository.live
          )

      }
    },
    testM("should save an Owner") {
      checkM(DeriveGen[Owner], Gen.weighted((Gen.const(0), 5), (Gen.int(1, Int.MaxValue), 5))) {
        (owner, oId) =>
          val ownerDao = OwnerDaoMock.Update(
            hasField("id", _.id, isGreaterThan(0)),
            valueF(o => MOwner(id = oId, o.firstName, o.lastName, o.address, o.city, o.telephone))
          ) || OwnerDaoMock.Insert(
            hasField("id", _.id, equalTo(0)),
            valueF(o => MOwner(id = oId, o.firstName, o.lastName, o.address, o.city, o.telephone))
          )

          assertM(OwnerRepository.save(owner.copy(id = oId)))(
            equalTo(owner.copy(id = oId))
          ).provideLayer(
            ownerDao >>> OwnerRepository.live
          )
      }
    }
  )
}
