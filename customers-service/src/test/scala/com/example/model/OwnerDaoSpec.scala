package com.example.model

import com.example.fixture.{RunningMysql, mysqlDbConf}
import zio.test.Assertion.hasSameElements
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, _}

import java.time.LocalDate

object OwnerDaoSpec extends DefaultRunnableSpec {

  def spec: Spec[TestEnvironment, TestFailure[Throwable], TestSuccess] =
    suite("OwnerDao.mySql")(
      testM("should return all owners from mysql db") {
        assertM(OwnerDao.findAll)(
          hasSameElements(
            List(
              (
                Owner(1, "George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"),
                Some((Pet(1, "Leo", LocalDate.of(2000, 9, 7), 1, 1), PetType(1, "cat")))
              ),
              (
                Owner(2, "Betty", "Davis", "638 Cardinal Ave.", "Sun Prairie", "6085551749"),
                Some((Pet(2, "Basil", LocalDate.of(2002, 8, 6), 6, 2), PetType(6, "hamster")))
              ),
              (
                Owner(3, "Eduardo", "Rodriquez", "2693 Commerce St.", "McFarland", "6085558763"),
                Some((Pet(3, "Rosy", LocalDate.of(2001, 4, 17), 2, 3), PetType(2, "dog")))
              ),
              (
                Owner(3, "Eduardo", "Rodriquez", "2693 Commerce St.", "McFarland", "6085558763"),
                Some((Pet(4, "Jewel", LocalDate.of(2000, 3, 7), 2, 3), PetType(2, "dog")))
              ),
              (
                Owner(4, "Harold", "Davis", "563 Friendly St.", "Windsor", "6085553198"),
                Some((Pet(5, "Iggy", LocalDate.of(2000, 11, 30), 3, 4), PetType(3, "lizard")))
              ),
              (
                Owner(5, "Peter", "McTavish", "2387 S. Fair Way", "Madison", "6085552765"),
                Some((Pet(6, "George", LocalDate.of(2000, 1, 20), 4, 5), PetType(4, "snake")))
              ),
              (
                Owner(6, "Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"),
                Some((Pet(7, "Samantha", LocalDate.of(1995, 9, 4), 1, 6), PetType(1, "cat")))
              ),
              (
                Owner(6, "Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"),
                Some((Pet(8, "Max", LocalDate.of(1995, 9, 4), 1, 6), PetType(1, "cat")))
              ),
              (
                Owner(7, "Jeff", "Black", "1450 Oak Blvd.", "Monona", "6085555387"),
                Some((Pet(9, "Lucky", LocalDate.of(1999, 8, 6), 5, 7), PetType(5, "bird")))
              ),
              (
                Owner(8, "Maria", "Escobito", "345 Maple St.", "Madison", "6085557683"),
                Some((Pet(10, "Mulligan", LocalDate.of(1997, 2, 24), 2, 8), PetType(2, "dog")))
              ),
              (
                Owner(9, "David", "Schroeder", "2749 Blackhawk Trail", "Madison", "6085559435"),
                Some((Pet(11, "Freddy", LocalDate.of(2000, 3, 9), 5, 9), PetType(5, "bird")))
              ),
              (
                Owner(10, "Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487"),
                Some((Pet(12, "Lucky", LocalDate.of(2000, 6, 24), 2, 10), PetType(2, "dog")))
              ),
              (
                Owner(10, "Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487"),
                Some((Pet(13, "Sly", LocalDate.of(2002, 6, 8), 1, 10), PetType(1, "cat")))
              )
            )
          )
        )
      },
      testM("should return a owner from mysql db") {
        assertM(OwnerDao.findById(10))(
          hasSameElements(
            List(
              (
                Owner(10, "Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487"),
                Some((Pet(12, "Lucky", LocalDate.of(2000, 6, 24), 2, 10), PetType(2, "dog")))
              ),
              (
                Owner(10, "Carlos", "Estaban", "2335 Independence La.", "Waunakee", "6085555487"),
                Some((Pet(13, "Sly", LocalDate.of(2002, 6, 8), 1, 10), PetType(1, "cat")))
              )
            )
          )
        )
      },
      testM("should insert a owner to mysql db") {
        OwnerDao
          .insert(
            Owner(
              firstName = "Jack",
              lastName = "Smith",
              address = "14th Ave",
              city = "New York",
              telephone = "4163340476"
            )
          )
          .flatMap(o =>
            assertM(OwnerDao.findById(o.id))(
              hasSameElements(
                List(
                  (
                    Owner(
                      id = o.id,
                      firstName = "Jack",
                      lastName = "Smith",
                      address = "14th Ave",
                      city = "New York",
                      telephone = "4163340476"
                    ),
                    None
                  )
                )
              )
            )
          )
      },
      testM("should update a owner in mysql db") {
        OwnerDao
          .update(Owner(10, "Carl", "Logan", "1st Street", "Phoenix", "7777777777"))
          .flatMap(uo =>
            assertM(OwnerDao.findById(uo.id))(
              hasSameElements(
                List(
                  (
                    Owner(10, "Carl", "Logan", "1st Street", "Phoenix", "7777777777"),
                    Some((Pet(12, "Lucky", LocalDate.of(2000, 6, 24), 2, 10), PetType(2, "dog")))
                  ),
                  (
                    Owner(10, "Carl", "Logan", "1st Street", "Phoenix", "7777777777"),
                    Some((Pet(13, "Sly", LocalDate.of(2002, 6, 8), 1, 10), PetType(1, "cat")))
                  )
                )
              )
            )
          )
      }
    ).provideCustomLayerShared(
      (RunningMysql.live >>> mysqlDbConf >>> DbTransactor.live >>> OwnerDao.mySql)
        .mapError(TestFailure.fail)
    )
}
