package com.example

import com.example.fixture.{OpenPortFinder, RunningMysql, VetsGrpcService}
import com.examples.proto.api.vets_service.{GetVetsRequest, Specialty, Vet}
import zio.duration._
import zio.test.Assertion.hasSameElements
import zio.test.TestAspect._
import zio.test._
import zio.test.environment._
import zio.{ExitCode, Ref, Task}

object VetsServiceSpec extends DefaultRunnableSpec {

  def spec: Spec[TestEnvironment, TestFailure[Any], TestSuccess] =
    suiteM("VetsService")(
      for {
        initialServicePortToTry <- Ref.make(49252)
      } yield List(
        testM("should return list of Vets") {

          for {
            _ <- runServerInTheBackground
            vets = VetsGrpcService.client.flatMap(_.getVets(GetVetsRequest()).map(_.vets))
            testResult <- assertM(vets)(
              hasSameElements(
                List(
                  Vet(id = 1, firstName = "James", lastName = "Carter", specialties = List.empty),
                  Vet(
                    id = 2,
                    firstName = "Helen",
                    lastName = "Leary",
                    specialties = List(Specialty("radiology"))
                  ),
                  Vet(
                    id = 3,
                    firstName = "Linda",
                    lastName = "Douglas",
                    specialties = List(Specialty("surgery"), Specialty("dentistry"))
                  ),
                  Vet(
                    id = 4,
                    firstName = "Rafael",
                    lastName = "Ortega",
                    specialties = List(Specialty("surgery"))
                  ),
                  Vet(
                    id = 5,
                    firstName = "Henry",
                    lastName = "Stevens",
                    specialties = List(Specialty("radiology"))
                  ),
                  Vet(
                    id = 6,
                    firstName = "Sharon",
                    lastName = "Jenkins",
                    specialties = List.empty
                  )
                )
              )
            ).eventually
          } yield testResult
        }.provideCustomLayer(
          (RunningMysql.live ++ (OpenPortFinder
            .live(initialServicePortToTry) >>> VetsGrpcService.live))
            .mapError(TestFailure.fail)
        ) @@ timeout(25.seconds)
      )
    )

  private val runServerInTheBackground =
    for {
      dbUser <- RunningMysql.username
      dbPassword <- RunningMysql.password
      dbUrl <- RunningMysql.jdbcUrl
      jdbcClassName <- RunningMysql.driverClassName
      _ <- TestSystem.putEnv("db.user", dbUser)
      _ <- TestSystem.putEnv("db.pass", dbPassword)
      _ <- TestSystem.putEnv("jdbc.url", dbUrl)
      _ <- TestSystem.putEnv("jdbc.driver.name", jdbcClassName)
      port <- VetsGrpcService.port
      _ <- TestSystem.putEnv("server.port", port.toString)
      f <- VetsServiceServer
        .run(List.empty)
        .flatMap {
          case ExitCode.failure => Task.fail(new IllegalStateException("App crashed"))
          case _                => Task.unit
        }
        .fork
    } yield f

}
