package com.example.model

import java.time.LocalDate

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.example.config.Configuration.DbConfig
import com.example.model.DbTransactor
import com.example.model.Visit
import com.example.model.VisitDao
import zio.Task
import zio.ZLayer
import zio.ZManaged
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._

object VisitDaoSpec extends DefaultRunnableSpec {

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

  def spec = suite("VisitDao.mySql")(
    testM("should return visits for a pet from mysql db") {
      mysqlC
        .use {
          mysql =>
            assertM(VisitDao.findByPetId(7))(
              hasSameElements(
                List(
                  Visit(
                    id = 1,
                    petId = 7,
                    visitDate = LocalDate.of(2010, 3, 4),
                    description = "rabies shot"
                  ),
                  Visit(
                    id = 4,
                    petId = 7,
                    visitDate = LocalDate.of(2008, 9, 4),
                    description = "spayed"
                  )
                )
              )
            ).provideCustomLayer(
              ZLayer.succeed(
                DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
              ) >>> DbTransactor.live >>> VisitDao.mySql
            )
        }
    },
    testM("should return visits for pets from mysql db") {
      mysqlC
        .use {
          mysql =>
            assertM(VisitDao.findByPetIdIn(List(7, 8)))(
              hasSameElements(
                List(
                  Visit(
                    id = 1,
                    petId = 7,
                    visitDate = LocalDate.of(2010, 3, 4),
                    description = "rabies shot"
                  ),
                  Visit(
                    id = 4,
                    petId = 7,
                    visitDate = LocalDate.of(2008, 9, 4),
                    description = "spayed"
                  ),
                  Visit(
                    id = 2,
                    petId = 8,
                    visitDate = LocalDate.of(2011, 3, 4),
                    description = "rabies shot"
                  ),
                  Visit(
                    id = 3,
                    petId = 8,
                    visitDate = LocalDate.of(2009, 6, 4),
                    description = "neutered"
                  )
                )
              )
            ).provideCustomLayer(
              ZLayer.succeed(
                DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
              ) >>> DbTransactor.live >>> VisitDao.mySql
            )
        }
    },
    testM("should insert visit to mysql db") {
      mysqlC
        .use {
          mysql =>
            VisitDao
              .save(
                Visit(petId = 7, visitDate = LocalDate.of(2020, 8, 19), description = "checkup")
              )
              .flatMap(v =>
                assertM(VisitDao.findByPetId(v.petId))(
                  hasSameElements(
                    List(
                      Visit(
                        id = 1,
                        petId = 7,
                        visitDate = LocalDate.of(2010, 3, 4),
                        description = "rabies shot"
                      ),
                      Visit(
                        id = 4,
                        petId = 7,
                        visitDate = LocalDate.of(2008, 9, 4),
                        description = "spayed"
                      ),
                      Visit(
                        id = v.id,
                        petId = 7,
                        visitDate = LocalDate.of(2020, 8, 19),
                        description = "checkup"
                      )
                    )
                  )
                )
              )
              .provideCustomLayer(
                ZLayer.succeed(
                  DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
                ) >>> DbTransactor.live >>> VisitDao.mySql
              )
        }
    }
  )

}
