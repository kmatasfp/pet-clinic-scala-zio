package com.example

import scala.jdk.CollectionConverters._

import com.dimafeng.testcontainers.MySQLContainer
import com.examples.proto.api.vets_service.GetVetsRequest
import com.examples.proto.api.vets_service.Specialty
import com.examples.proto.api.vets_service.Vet
import com.examples.proto.api.vets_service.ZioVetsService
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel
import zio.Task
import zio.ZManaged
import zio.duration._
import zio.test.Assertion.hasSameElements
import zio.test.TestAspect._
import zio.test._
import zio.test.environment._

object VetsServiceSpec extends DefaultRunnableSpec {

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
        f <- VetsServiceServer.run(List.empty).forkDaemon
      } yield {
        f
      }

      ZManaged.make(acquire)(_.interruptFork)
    }

  private val before = (port: Int) => mysql.flatMap(server(port))

  private def port(p: Int) = p

  def spec =
    suite("VetsService")(
      testM("should return list of Vets") {

        before(port(9000)).use_ {

          val vets = ZioVetsService.VetsClient.getVets(GetVetsRequest()).map(_.vets)

          assertM(vets)(
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
                Vet(id = 6, firstName = "Sharon", lastName = "Jenkins", specialties = List.empty)
              )
            )
          ).provideCustomLayer(
              ZioVetsService
                .VetsClient
                .live(
                  ZManagedChannel(
                    ManagedChannelBuilder.forAddress("localhost", port(9000)).usePlaintext()
                  )
                )
            )
            .eventually

        }
      } @@ timeout(25.seconds)
    )
}
