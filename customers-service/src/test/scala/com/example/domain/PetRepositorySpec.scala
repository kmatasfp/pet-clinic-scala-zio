package com.example

import java.time.LocalDate

import com.example.domain.Pet
import com.example.domain.PetOwner
import com.example.domain.PetRepository
import com.example.domain.PetType
import com.example.model.PetDao
import com.example.model.{ Pet => MPet }
import com.example.model.{ PetOwner => MPetOwner }
import com.example.model.{ PetType => MPetType }
import zio.Ref
import zio.ZLayer
import zio.test.DefaultRunnableSpec
import zio.test._

object PetRepositorySpec extends DefaultRunnableSpec {

  def spec = suite("PetRepository")(
    testM("should return Pet for given id") {

      assertM(PetRepository.findById(7))(
        Assertion.equalTo(
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
        ZLayer.fromEffect(
          Ref.make(
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
        ) >>> PetDao.inMemory >>> PetRepository.live
      )
    }
  )

}
