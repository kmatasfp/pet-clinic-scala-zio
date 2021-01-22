package com.example

import java.time.LocalDate

import com.example.domain.Pet
import com.example.domain.PetOwner
import com.example.domain.PetRepository
import com.example.domain.PetType
import com.example.model.{ Pet => MPet }
import com.example.model.{ PetOwner => MPetOwner }
import com.example.model.{ PetType => MPetType }
import com.example.model.PetDao
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.mock.Expectation._
import zio.test.mock.mockable

@mockable[PetDao.Service]
object PetDaoMock

object PetRepositorySpec extends DefaultRunnableSpec {

  def spec = suite("PetRepository")(
    testM("should return Pet for given id") {

      val petDao = PetDaoMock.FindById(
        anything,
        value(
          List(
            (
              MPet(
                id = 7,
                name = "Samantha",
                birthDate = LocalDate.of(1995, 9, 4),
                typeId = 1,
                ownerId = 6
              ),
              MPetType(id = 1, name = "cat"),
              MPetOwner(
                id = 6,
                firstName = "Jean",
                lastName = "Coleman",
                address = "105 N. Lake St.",
                city = "Monona",
                telephone = "6085552654"
              )
            )
          )
        )
      )

      assertM(PetRepository.findById(7))(
        equalTo(
          Some(
            Pet(
              id = 7,
              name = "Samantha",
              birthDate = LocalDate.of(1995, 9, 4),
              `type` = PetType(name = "cat"),
              owner = PetOwner(
                firstName = "Jean",
                lastName = "Coleman",
                address = "105 N. Lake St.",
                city = "Monona",
                telephone = "6085552654"
              )
            )
          )
        )
      ).provideLayer(
        petDao >>> PetRepository.live
      )
    },
    testM("should return PetTypes") {
      val petDao = PetDaoMock.GetPetTypes(
        value(
          List(
            MPetType(id = 1, name = "cat"),
            MPetType(id = 2, name = "dog"),
            MPetType(id = 3, name = "lizard")
          )
        )
      )

      assertM(PetRepository.getPetTypes)(
        hasSameElements(
          List(
            PetType("cat"),
            PetType("dog"),
            PetType("lizard")
          )
        )
      ).provideLayer(
        petDao >>> PetRepository.live
      )
    }
  )

}
