package com.example.model

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import zio.Task
import zio.ZManaged
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.Assertion.hasSameElements
import java.time.LocalDate
import zio.ZLayer
import com.example.config.Configuration.DbConfig

object PetDaoSpec extends DefaultRunnableSpec {

  private val mysqlC = {
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

  def spec =
    suite("PetDao.mySql")(testM("should return a pet from mysql db") {
      mysqlC
        .use {
          mysql =>
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
            ).provideCustomLayer(
              ZLayer.succeed(
                DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
              ) >>> DbTransactor.live >>> PetDao.mySql
            )
        }
    })
}
