package com.apitesting.gatling

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class UnitTest extends Simulation {

  val baseUrl = "http://127.0.0.1:8900"
  val contentType = "application/json"

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("*/*")
    .contentTypeHeader(contentType)
    .userAgentHeader("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT)")

  val myScript = new UserFlow

  setUp(myScript.scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}