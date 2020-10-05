package com.example

import zio.Has

package object model {

  type VisitDao = Has[VisitDao.Service]
  type DbTransactor = Has[DbTransactor.Resource]

}
