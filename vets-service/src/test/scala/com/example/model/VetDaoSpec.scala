package com.example.model

import com.example.config.Configuration.DbConfig
import com.example.fixture.RunningMysql
import com.example.model.DbTransactor
import com.example.model.Specialty
import com.example.model.Vet
import com.example.model.VetDao
import zio.ZLayer
import zio.test.DefaultRunnableSpec
import zio.test._

object VetDaoSpec extends DefaultRunnableSpec {

  private val mysqlDbConf = ZLayer.fromEffect(for {
    dbUser <- RunningMysql.username
    dbPassword <- RunningMysql.password
    dbUrl <- RunningMysql.jdbcUrl
    jdbcClassName <- RunningMysql.driverClassName
  } yield {
    DbConfig(jdbcClassName, dbUrl, dbUser, dbPassword)
  })

  def spec =
    suite("VetDao.mySql")(
      testM("should return vets and their specialities from mysql db") {

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
        )
      }
    ).provideLayerShared(
      (RunningMysql.live >>> mysqlDbConf >>> DbTransactor.live >>> VetDao.mySql)
        .mapError(TestFailure.fail)
    )
}
