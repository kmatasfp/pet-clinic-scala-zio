package com.example

import zio.Has

package object fixture {
  type RunningMysql = Has[RunningMysql.Service]
  type OpenPortFinder = Has[OpenPortFinder.Service]
  type VisitsGrpcService = Has[VisitsGrpcService.Service]
}
