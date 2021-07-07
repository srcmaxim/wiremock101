package com.acme;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WireMockTest {

  private WireMockServer wireMock;

  private WireMock testee;

  @BeforeEach
  void setup() {
    wireMock = new WireMockServer(options().dynamicPort());
    wireMock.start();
    String host = "http://localhost:" + wireMock.port();
    testee = new WireMock(host);
  }

  @AfterEach
  void teardown() {
    wireMock.shutdown();
  }

  @Test
  public void getWeatherId() throws ExecutionException, InterruptedException {
    wireMock.stubFor(get("/weather/1")
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(ok()
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "date": "2020-06-23",
                  "temp": 35
                }
                """)));
    var response = testee.getWeather(1L).get();
    Assertions.assertEquals(response.statusCode(), 200);
    Assertions.assertEquals(response.body().date(), LocalDate.of(2020, 6, 23));
    Assertions.assertEquals(response.body().temp(), 35);

    wireMock.verify(getRequestedFor(urlPathEqualTo("/weather/1"))
        .withHeader("Accept", equalTo("application/json")));
  }

  @Test
  public void getWeather() throws ExecutionException, InterruptedException {
    wireMock.stubFor(get("/weather")
        .withHeader("Accept", equalTo("application/json"))
        .willReturn(ok()
            .withHeader("Content-Type", "application/json")
            .withBody("""
                [
                  {
                    "date": "2020-06-23",
                    "temp": 35
                  }, {
                    "date": "2020-06-24",
                    "temp": 30
                  }
                ]
                """)));
    var response = testee.getWeather().get();
    Assertions.assertEquals(response.statusCode(), 200);
    Assertions.assertEquals(response.body().get(0).date(), LocalDate.of(2020, 6, 23));
    Assertions.assertEquals(response.body().get(0).temp(), 35);
    Assertions.assertEquals(response.body().get(1).date(), LocalDate.of(2020, 6, 24));
    Assertions.assertEquals(response.body().get(1).temp(), 30);

    wireMock.verify(getRequestedFor(urlPathEqualTo("/weather"))
        .withHeader("Accept", equalTo("application/json")));
  }

}