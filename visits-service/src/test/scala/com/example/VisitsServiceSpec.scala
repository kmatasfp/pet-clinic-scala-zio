package com.example

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.examples.proto.api.visits_service.CreateVisitRequest
import com.examples.proto.api.visits_service.GetVisitsForPetRequest
import com.examples.proto.api.visits_service.GetVisitsForPetsRequest
import com.examples.proto.api.visits_service.Visit
import com.examples.proto.api.visits_service.VisitId
import com.examples.proto.api.visits_service.ZioVisitsService
import com.google.protobuf.timestamp.Timestamp
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel
import zio.Task
import zio.ZManaged
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.Assertion.hasSameElements
import zio.test.TestAspect._
import zio.test._
import zio.test.environment._

object VisitsServiceSpec extends DefaultRunnableSpec {

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

  private val server = (port: Int) =>
    (mc: MySQLContainer) => {
      val acquire = for {
        _ <- TestSystem.putEnv("jdbc.driver.name", mc.driverClassName)
        _ <- TestSystem.putEnv("jdbc.url", mc.jdbcUrl)
        _ <- TestSystem.putEnv("db.user", mc.username)
        _ <- TestSystem.putEnv("db.pass", mc.password)
        _ <- TestSystem.putEnv("server.port", port.toString())
        f <- VisitsServiceServer.run(List.empty).forkDaemon
      } yield {
        f
      }

      ZManaged.make(acquire)(_.interruptFork)
    }

  private val before = (port: Int) => mysql.flatMap(server(port))

  private def port(p: Int) = p

  def spec =
    suite("VisitsService")(
      testM("should return visits for a pet")(
        before(port(9001)).use_ {
          val visits = ZioVisitsService
            .VisitsClient
            .getVisitsForPet(GetVisitsForPetRequest(petId = 7))
            .map(_.visits)

          assertM(visits)(
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
          ).provideCustomLayer(
              ZioVisitsService
                .VisitsClient
                .live(
                  ZManagedChannel(
                    ManagedChannelBuilder.forAddress("localhost", port(9001)).usePlaintext()
                  )
                )
            )
            .eventually
        }
      ) @@ timeout(25.seconds),
      testM("should return visits for a pets")(
        before(port(9002)).use_ {
          val visits = ZioVisitsService
            .VisitsClient
            .getVisitsForPets(GetVisitsForPetsRequest(petIds = List(7, 8)))
            .map(_.visits)

          assertM(visits)(
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
          ).provideCustomLayer(
              ZioVisitsService
                .VisitsClient
                .live(
                  ZManagedChannel(
                    ManagedChannelBuilder.forAddress("localhost", port(9002)).usePlaintext()
                  )
                )
            )
            .eventually
        }
      ) @@ timeout(25.seconds),
      testM("should create visit for a pet")(
        before(port(9003)).use_ {
          val visit = ZioVisitsService
            .VisitsClient
            .createVisit(
              CreateVisitRequest(
                Some(
                  Visit(
                    petId = 6,
                    visitDate = Some(Timestamp.of(seconds = 1609545600, nanos = 0)),
                    description = "Checkup"
                  )
                )
              )
            )
            .map(_.visit)

          assertM(visit)(
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
          ).provideCustomLayer(
              ZioVisitsService
                .VisitsClient
                .live(
                  ZManagedChannel(
                    ManagedChannelBuilder.forAddress("localhost", port(9003)).usePlaintext()
                  )
                )
            )
            .eventually
        }
      ) @@ timeout(25.seconds)
    )
}
