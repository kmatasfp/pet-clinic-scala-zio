package com.example.domain

import com.example.domain.OwnerRepository.{Owner, Pet, PetType}
import com.example.model.{Owner => MOwner, Pet => MPet, PetType => MPetType}
import zio.Has
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.environment.{Live, TestClock, TestConsole, TestRandom, TestSystem}
import zio.test.magnolia.DeriveGen
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, _}

object OwnerRepositorySpec extends DefaultRunnableSpec {

  def spec
      : Spec[Has[Annotations.Service] with Has[Live.Service] with Has[Sized.Service] with Has[TestClock.Service] with Has[TestConfig.Service] with Has[TestConsole.Service] with Has[TestRandom.Service] with Has[TestSystem.Service] with Has[Clock.Service] with Has[zio.console.Console.Service] with Has[zio.system.System.Service] with Has[Random.Service] with Has[Blocking.Service], TestFailure[Any], TestSuccess] =
    suite("OwnerRepository.live")(
      testM("should return Owner for given id") {

        checkM(genOwnerWithMaybePets) {
          case (mOwner, mPets) =>
            val ownerDao =
              if (mPets.isEmpty)
                OwnerDaoMock.FindById(anything, value(List((mOwner, None))))
              else
                OwnerDaoMock.FindById(anything, value(mPets.map(pet => (mOwner, Some(pet)))))

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
        checkM(Gen.listOf(genOwnerWithMaybePets)) { mOwners =>
          val ownerDao = OwnerDaoMock.FindAll(
            value(
              mOwners.flatMap {
                case (mOwner, Nil) =>
                  List((mOwner, None))
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
        val genIdForUpdate = Gen.const(0)
        val genIdForInsert = Gen.int(1, Int.MaxValue)

        checkM(DeriveGen[Owner], Gen.weighted((genIdForUpdate, 5), (genIdForInsert, 5))) { (owner, oId) =>
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

  private val genPetWithPetType = for {
    pet <- DeriveGen[MPet]
    petType <- DeriveGen[MPetType]
  } yield (pet, petType)

  private val genOwnerWithMaybePets =
    for {
      owner <- DeriveGen[MOwner]
      petsWithType <- Gen.listOf(genPetWithPetType)
    } yield (owner, petsWithType)
}
