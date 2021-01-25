package com.example.model

import java.time.LocalDate

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.example.config.Configuration.DbConfig
import zio.Has
import zio.Task
import zio.ZLayer
import zio.test.Assertion.hasSameElements
import zio.test.DefaultRunnableSpec
import zio.test._

object PetDaoSpec extends DefaultRunnableSpec {

  private val mysql = {
    val acquire = Task {

      val mysql = MySQLContainer().configure { c =>
        c.withUsername("root")
        c.withPassword("")
        c.withInitScript("db/mysql/init.sql")
        c.withTmpFs(Map("/testtmpfs" -> "rw").asJava)
        c.withDatabaseName("petclinic")
      }

      mysql.start()

      mysql
    }

    val release = (m: MySQLContainer) => Task(m.close()).orDie

    ZLayer.fromAcquireRelease(acquire)(release)
  }

  private val mysqlDbConf =
    mysql.map(msc =>
      Has(DbConfig(msc.get.driverClassName, msc.get.jdbcUrl, msc.get.username, msc.get.password))
    )

  def spec =
    suite("PetDao.mySql")(
      testM("should return a pet from mysql db") {
        assertM(PetDao.findById(7))(
          hasSameElements(
            List(
              (
                Pet(
                  id = 7,
                  name = "Samantha",
                  birthDate = LocalDate.of(1995, 9, 4),
                  typeId = 1,
                  ownerId = 6
                ),
                PetType(id = 1, name = "cat"),
                Owner(
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
      },
      testM("should return pet types from mysql db") {
        assertM(PetDao.getPetTypes)(
          hasSameElements(
            List(
              PetType(id = 1, name = "cat"),
              PetType(id = 2, name = "dog"),
              PetType(id = 3, name = "lizard"),
              PetType(id = 4, name = "snake"),
              PetType(id = 5, name = "bird"),
              PetType(id = 6, name = "hamster")
            )
          )
        )
      },
      testM("should insert pet to mysql db") {
        PetDao
          .save(
            Pet(
              name = "Ghost",
              birthDate = LocalDate.of(2020, 1, 5),
              typeId = 2,
              ownerId = 6
            )
          )
          .flatMap(p =>
            assertM(PetDao.findById(p.id))(
              hasSameElements(
                List(
                  (
                    Pet(
                      id = 14,
                      name = "Ghost",
                      birthDate = LocalDate.of(2020, 1, 5),
                      typeId = 2,
                      ownerId = 6
                    ),
                    PetType(id = 2, name = "dog"),
                    Owner(
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
          )
      }
    ).provideCustomLayerShared(
      (mysqlDbConf >>> DbTransactor.live >>> PetDao.mySql).mapError(TestFailure.fail)
    )

}
