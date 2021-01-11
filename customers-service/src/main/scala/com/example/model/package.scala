package com.example

import zio.Has

package object model {

  type PetDao = Has[PetDao.Service]
  type DbTransactor = Has[DbTransactor.Resource]

}
