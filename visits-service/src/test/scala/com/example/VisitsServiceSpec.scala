package com.example

import com.example.fixture.OpenPortFinder
import com.example.fixture.RunningMysql
import com.example.fixture.VisitsGrpcService
import com.examples.proto.api.visits_service.CreateVisitRequest
import com.examples.proto.api.visits_service.GetVisitsForPetRequest
import com.examples.proto.api.visits_service.GetVisitsForPetsRequest
import com.examples.proto.api.visits_service.Visit
import com.examples.proto.api.visits_service.VisitId
import com.google.protobuf.timestamp.Timestamp
import zio.ExitCode
import zio.Ref
import zio.Task
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.Assertion.hasSameElements
import zio.test.TestAspect._
import zio.test._
import zio.test.environment._

object VisitsServiceSpec extends DefaultRunnableSpec {

  def spec =
    suiteM("VisitsService")(
      for {
        initialServicePortToTry <- Ref.make(49352)
      } yield {
        List(
          testM("should return visits for a pet") {
            for {
              _ <- runServerInTheBackground
              visits = VisitsGrpcService
                .client
                .flatMap(
                  _.getVisitsForPet(GetVisitsForPetRequest(petId = 7))
                    .map(_.visits)
                )
              testResult <- assertM(visits)(
                hasSameElements(
                  List(
                    Visit(
                      id = Some(VisitId(1)),
                      petId = 7,
                      visitDate = Some(Timestamp.of(seconds = 1267660800, nanos = 0)),
                      description = "rabies shot"
                    ),
                    Visit(
                      id = Some(VisitId(4)),
                      petId = 7,
                      visitDate = Some(Timestamp.of(seconds = 1220486400, nanos = 0)),
                      description = "spayed"
                    )
                  )
                )
              ).eventually
            } yield {
              testResult
            }

          }.provideCustomLayer(runningMySqlAndGrpcClient(initialServicePortToTry)) @@ timeout(
            25.seconds
          ),
          testM("should return visits for a pets") {

            for {
              _ <- runServerInTheBackground
              visits = VisitsGrpcService
                .client
                .flatMap(
                  _.getVisitsForPets(GetVisitsForPetsRequest(petIds = List(7, 8)))
                    .map(_.visits)
                )
              testResult <- assertM(visits)(
                hasSameElements(
                  List(
                    Visit(
                      id = Some(VisitId(1)),
                      petId = 7,
                      visitDate = Some(Timestamp.of(seconds = 1267660800, nanos = 0)),
                      description = "rabies shot"
                    ),
                    Visit(
                      id = Some(VisitId(4)),
                      petId = 7,
                      visitDate = Some(Timestamp.of(seconds = 1220486400, nanos = 0)),
                      description = "spayed"
                    ),
                    Visit(
                      id = Some(VisitId(2)),
                      petId = 8,
                      visitDate = Some(Timestamp.of(seconds = 1299196800, nanos = 0)),
                      description = "rabies shot"
                    ),
                    Visit(
                      id = Some(VisitId(3)),
                      petId = 8,
                      visitDate = Some(Timestamp.of(seconds = 1244073600, nanos = 0)),
                      description = "neutered"
                    )
                  )
                )
              ).eventually
            } yield {
              testResult
            }

          }.provideCustomLayer(runningMySqlAndGrpcClient(initialServicePortToTry)) @@ timeout(
            25.seconds
          ),
          testM("should create visit for a pet") {

            for {
              _ <- runServerInTheBackground
              visit = VisitsGrpcService
                .client
                .flatMap(
                  _.createVisit(
                    CreateVisitRequest(
                      Some(
                        Visit(
                          petId = 6,
                          visitDate = Some(Timestamp.of(seconds = 1609545600, nanos = 0)),
                          description = "Checkup"
                        )
                      )
                    )
                  ).map(_.visit)
                )
              testResult <- assertM(visit)(
                equalTo(
                  Some(
                    Visit(
                      id = Some(VisitId(5)),
                      petId = 6,
                      visitDate = Some(Timestamp.of(seconds = 1609545600, nanos = 0)),
                      description = "Checkup"
                    )
                  )
                )
              ).eventually
            } yield {
              testResult
            }
          }.provideCustomLayer(runningMySqlAndGrpcClient(initialServicePortToTry)) @@ timeout(
            25.seconds
          )
        )
      }
    )

  private def runningMySqlAndGrpcClient(initialServicePortToTry: Ref[Int]) =
    (RunningMysql.live ++ (OpenPortFinder.live(initialServicePortToTry) >>> VisitsGrpcService.live))
      .mapError(TestFailure.fail)

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
      port <- VisitsGrpcService.port
      _ <- TestSystem.putEnv("server.port", port.toString)
      f <- VisitsServiceServer
        .run(List.empty)
        .flatMap {
          case ExitCode.failure => Task.fail(new IllegalStateException("App crashed"))
          case _                => Task.unit
        }
        .fork
    } yield {
      f
    }

}
