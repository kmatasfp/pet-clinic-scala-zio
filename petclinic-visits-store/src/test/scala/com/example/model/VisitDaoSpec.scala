package com.example.model

import java.time.LocalDate

import com.dimafeng.testcontainers.MySQLContainer
import com.example.model.{ DbTransactor, Visit, VisitDao }
import com.example.config.Configuration.DbConfig
import zio.test._
import zio.test.DefaultRunnableSpec
import zio.{ Task, ZLayer, ZManaged }

import scala.jdk.CollectionConverters._

object VisitDaoSpec extends DefaultRunnableSpec {
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

    ZManaged.make(acquire)(release)
  }

  def spec = suite("VisitDao")(
    testM("should return visits from mysql db") {
      mysql
        .use {
          mysql =>
            assertM(VisitDao.findByPetId(7))(
              Assertion.hasSameElements(
                List(
                  Visit(
                    id = Some(1),
                    petId = 7,
                    date = LocalDate.of(2010, 3, 4),
                    descripion = "rabies shot"
                  ),
                  Visit(
                    id = Some(4),
                    petId = 7,
                    date = LocalDate.of(2008, 9, 4),
                    descripion = "spayed"
                  )
                )
              )
            ).provideCustomLayer(
              ZLayer.succeed(
                DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
              ) >>> DbTransactor.live >>> VisitDao.mySql
            )
        }
    }
  )

}
