package com.example.model.repository

import doobie.implicits._
import doobie.quill.DoobieContextBase
import doobie.util.transactor.Transactor
import zio.Task
import io.getquill.NamingStrategy
import io.getquill.context.jdbc.JdbcContextBase
import io.getquill.context.sql.idiom.SqlIdiom
import com.examples.proto.api.vet_store.Vet
import zio.interop.catz._

final private[repository] case class Database[D <: SqlIdiom, N <: NamingStrategy](
    xa: Transactor[Task],
    ctx: DoobieContextBase[D, N] with JdbcContextBase[D, N]
  ) extends VetsRepository.Service {

  import ctx._

  def all: Task[List[Vet]] =
    ctx.run(Queries.all).transact(xa)

  private object Queries {
    val all = quote(query[Vet])
  }
}
