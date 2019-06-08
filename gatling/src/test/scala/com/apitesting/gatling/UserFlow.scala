package com.apitesting.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent.duration._


class UserFlow extends Simulation {

  val airportCodes = csv("IATA_Europe_Codes.csv").batch.random
  val name = csv("passengerNames.csv").random
  val email = csv("domainMail.csv").random

  val baseUrl = "http://127.0.0.1:8900"
  val contentType = "application/json"

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("*/*")
    .contentTypeHeader(contentType)
    .userAgentHeader("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT)")

  val format = new SimpleDateFormat("yyyy-MM-dd")
  val myDate = format.format(Calendar.getInstance().getTime())
  val oldDate = Calendar.getInstance()
  oldDate.add(Calendar.DATE, -20)
  val myOldDate = format.format(oldDate.getTime())

  // Fuction to replace the @ by a % to use in the last transaction
  def replaceBadCharacters(s: String): String = {
    s.replaceAll("@", "%")
  }

  val scn = scenario("RYANAIR_TESTFLOW")

    .feed(airportCodes)
    .feed(name)
    .feed(email)

    // Preparation of data: email, cities and dates
    .exec(session => session.set("passengerMail", session("namePassenger").as[String] + session("emailDomain").as[String]))
    .exec(session => session.set("cityOrigin", session("airportIATACodes").as[String]))
    .feed(airportCodes)
    .exec(session => session.set("cityDestination", session("airportIATACodes").as[String]))
    .exec(session => session.set("dateNow", myDate.toString))
    .exec(session => session.set("dateOld", myOldDate.toString))

    .group("01_CREATE_USER") {

      exec(http("createUser")
        .post(baseUrl + "/user")
        .body(StringBody("""{"email":"${passengerMail}","name":"${namePassenger}"}""".stripMargin)).asJson
        .check(headerRegex("transfer-encoding", "chunked"))
        .check(regex("${passengerMail}"))
        .check(regex("bookings"))
        .check(regex("id\":\"(.*?)\",").find.saveAs("User_ID"))
        .check(status.is(201)))
    }

    // I m used to have "think times" between transactions
    .pause(1500 milliseconds)

    .group("02a_CREATE_BOOKING_PAST") {
      exec(http("createBookingPast")
        .post(baseUrl + "/booking")
        .body(StringBody("""{"date":"${dateOld}","destination":"${cityOrigin}","id":"${User_ID}","origin":"${cityDestination}"}""".stripMargin)).asJson
        .check(headerRegex("transfer-encoding", "chunked"))
        .check(regex("idBooking"))
        .check(regex("idBooking\":\"(.*?)\",").find.saveAs("Booking_ID1"))
        .check(status.is(201)))
    }

    .pause(1500 milliseconds)

    .group("02b_CREATE_BOOKING_TODAY") {
      exec(http("createBookingToday")
        .post(baseUrl + "/booking")
        .body(StringBody("""{"date":"${dateNow}","destination":"${cityOrigin}","id":"${User_ID}","origin":"${cityDestination}"}""".stripMargin)).asJson
        .check(headerRegex("transfer-encoding", "chunked"))
        .check(regex("idBooking"))
        .check(regex("idBooking\":\"(.*?)\",").find.saveAs("Booking_ID2"))
        .check(status.is(201)))
    }

    .pause(1500 milliseconds)

    .group("03_GET_ALL_BOOKINGS") {
      exec(http("getAllBookings")
        .get(baseUrl + "/booking"+"?date=${dateNow}")
        .check(headerRegex("transfer-encoding", "chunked"))
        .check(regex("idBooking"))
        .check(bodyString.saveAs("All_Bookings_Today"))
        .check(status.is(200)))
    }

    .pause(1500 milliseconds)


    .group("04a_GET_ALL_USERS") {

      exec(http("getAllUsers")
        .get(baseUrl + "/user/all")
        .check(headerRegex("transfer-encoding", "chunked"))
        // I write idUser because I want to get all the users that have a reservation, if I would want
        // all users with/without reservation I would set "id" as criteria in jsonPath
        .check(jsonPath("$[*]..idUser").findAll.saveAs("usersWithReservationList"))
        .check(status.is(200)))

    }
    .group("04b_GET_RESERVATION_RANDOM_USER") {

      exec(http("getReservationRandom")
        .get(baseUrl + "/booking?id=" + replaceBadCharacters("${usersWithReservationList.random()}"))
        .check(headerRegex("transfer-encoding", "chunked"))
        .check(bodyString.saveAs("All_Bookings_RandomUser"))
        .check(status.is(200)))
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}