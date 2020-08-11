package com.example

import zio.Has

package object model {

  type VetDao = Has[VetDao.Service]
  type DbTransactor = Has[DbTransactor.Resource]

}
