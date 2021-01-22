package com.example.model

import java.time.LocalDate

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.example.config.Configuration.DbConfig
import zio.Task
import zio.ZLayer
import zio.ZManaged
import zio.test.Assertion.hasSameElements
import zio.test.DefaultRunnableSpec
import zio.test._

object PetDaoSpec extends DefaultRunnableSpec {

  private val mysqlManaged = {
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

    ZManaged.make(acquire)(release)
  }

  private val mysqlDbConf = ZLayer.fromManaged(
    mysqlManaged.map(mysql =>
      DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
    )
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
                PetOwner(
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
      }
    ).provideCustomLayerShared(
      (mysqlDbConf >>> DbTransactor.live >>> PetDao.mySql).mapError(TestFailure.fail)
    )

}
