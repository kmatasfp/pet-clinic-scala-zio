package com.example.model

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.example.config.Configuration.DbConfig
import com.example.model.DbTransactor
import com.example.model.Specialty
import com.example.model.Vet
import com.example.model.VetDao
import zio.Task
import zio.ZLayer
import zio.ZManaged
import zio.test.DefaultRunnableSpec
import zio.test._

object VetDaoSpec extends DefaultRunnableSpec {

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

  def spec = suite("VetDao.mySql")(
    testM("should return vets and their specialities from mysql db") {
      mysql
        .use(mysql =>
          assertM(VetDao.findAll)(
            Assertion.hasSameElements(
              List(
                Vet(1, "James", "Carter") -> None,
                Vet(2, "Helen", "Leary") -> Some(Specialty(1, "radiology")),
                Vet(3, "Linda", "Douglas") -> Some(Specialty(2, "surgery")),
                Vet(3, "Linda", "Douglas") -> Some(Specialty(3, "dentistry")),
                Vet(4, "Rafael", "Ortega") -> Some(Specialty(2, "surgery")),
                Vet(5, "Henry", "Stevens") -> Some(Specialty(1, "radiology")),
                Vet(6, "Sharon", "Jenkins") -> None
              )
            )
          ).provideLayer(
            ZLayer.succeed(
              DbConfig(mysql.driverClassName, mysql.jdbcUrl, mysql.username, mysql.password)
            ) >>> DbTransactor.live >>> VetDao.mySql
          )
        )
    }
  )
}
