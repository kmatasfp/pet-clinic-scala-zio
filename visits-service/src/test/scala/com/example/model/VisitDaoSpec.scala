package com.example.model

import java.time.LocalDate

import com.example.config.Configuration.DbConfig
import com.example.fixture.RunningMysql
import com.example.model.DbTransactor
import com.example.model.Visit
import com.example.model.VisitDao
import zio.ZLayer
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._

object VisitDaoSpec extends DefaultRunnableSpec {

  def spec =
    suite("VisitDao.mySql")(
      testM("should return visits for a pet from mysql db") {

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
        )

      },
      testM("should return visits for pets from mysql db") {

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
        )

      },
      testM("should insert visit to mysql db") {

        VisitDao
          .save(
            Visit(petId = 6, visitDate = LocalDate.of(2020, 8, 19), description = "checkup")
          )
          .flatMap(v =>
            assertM(VisitDao.findByPetId(v.petId))(
              hasSameElements(
                List(
                  Visit(
                    id = v.id,
                    petId = 6,
                    visitDate = LocalDate.of(2020, 8, 19),
                    description = "checkup"
                  )
                )
              )
            )
          )
      }
    ).provideCustomLayerShared(
      (RunningMysql.live >>> mysqlDbConf >>> DbTransactor.live >>> VisitDao.mySql)
        .mapError(TestFailure.fail)
    )

  private val mysqlDbConf = ZLayer.fromEffect(for {
    dbUser <- RunningMysql.username
    dbPassword <- RunningMysql.password
    dbUrl <- RunningMysql.jdbcUrl
    jdbcClassName <- RunningMysql.driverClassName
  } yield {
    DbConfig(jdbcClassName, dbUrl, dbUser, dbPassword)
  })

}
