package com.example.flightbooking.Loadtest;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class FlightApiSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:3000")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // Test trực tiếp API Tìm kiếm chuyến bay (Endpoint Public, không tốn tài nguyên Auth)
    ScenarioBuilder scn = scenario("Stress Test Tim Chuyen Bay")
            .exec(
                    http("Tìm chuyến bay")
                            .get("/flights/search?sourceAirportCode=HAN&destAirportCode=SGN&departureDate=2026-10-25")
                            .check(status().is(200))
            );

    {
        setUp(
                scn.injectOpen(
                        // Nấc 1: 100 users/s trong 15s
                        rampUsersPerSec(1).to(100).during(15),
                        constantUsersPerSec(100).during(20),

                        // Nấc 2: Đẩy lên 300 users/s
                        rampUsersPerSec(100).to(300).during(15),
                        constantUsersPerSec(300).during(20),

                        // Nấc 3: Đẩy lên 600 users/s để ép ngưỡng nghẽn
                        rampUsersPerSec(300).to(600).during(15),
                        constantUsersPerSec(600).during(20)
                )
        ).protocols(httpProtocol);
    }
}