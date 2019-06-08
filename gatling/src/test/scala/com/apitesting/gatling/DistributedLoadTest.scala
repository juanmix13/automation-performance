package com.apitesting.gatling

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DistributedLoadTest extends Simulation
{
  val baseUrl = "http://127.0.0.1:8900"
  val contentType = "application/json"

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("*/*")
    .contentTypeHeader(contentType)
    .userAgentHeader("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT)")

  val myScript = new UserFlow

  setUp(
    // Adjusted based on the information got in the report for "Request per second"
    myScript.scn.inject(constantUsersPerSec(0.12) during (1 minute), constantUsersPerSec(0.36) during (1 minute),constantUsersPerSec(0.12) during (1 minute))
  ).protocols(httpProtocol)
}
