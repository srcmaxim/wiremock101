package com.acme;

import static java.net.http.HttpResponse.BodySubscribers.mapping;
import static java.net.http.HttpResponse.BodySubscribers.ofInputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WireMock {

  private final ObjectMapper mapper;
  private final HttpClient client;
  private final URI weatherService;
  private final BodyHandler<Weather> weatherBody = ofJson(new TypeReference<>() {

  });
  private final BodyHandler<List<Weather>> listWeatherBody = ofJson(new TypeReference<>() {

  });
  private final Function<Long, HttpRequest> getWeatherId;
  private final HttpRequest getWeather;

  public WireMock(String service) {
    weatherService = URI.create(service + "/weather");
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    client = HttpClient.newBuilder()
        .build();
    getWeatherId = id -> HttpRequest.newBuilder(URI.create(weatherService + "/" + id))
        .header("Accept", "application/json")
        .build();
    getWeather = HttpRequest.newBuilder(weatherService)
        .header("Accept", "application/json")
        .build();
  }

  public CompletableFuture<Response<List<Weather>>> getWeather() {
    return client.sendAsync(getWeather, listWeatherBody)
        .thenApply(Response::new);
  }

  public CompletableFuture<Response<Weather>> getWeather(Long id) {
    return client.sendAsync(getWeatherId.apply(id), weatherBody)
        .thenApply(Response::new);
  }

  private <T> BodyHandler<T> ofJson(Class<T> valueType) {
    CollectionType collectionType = mapper.getTypeFactory()
        .constructCollectionType(List.class, valueType);
    Function<InputStream, T> ofType = tryFunction(content -> mapper.readValue(content, collectionType));
    return (responseInfo) -> mapping(ofInputStream(), ofType);
  }

  public static <T, R> Function<T, R> tryFunction(TryFunction<T, R> f) {
    return a -> {
      try {
        return f.apply(a);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  @FunctionalInterface
  public interface TryFunction<T, R> {

    R apply(T t) throws Exception;
  }

  public static record Response<T>(int statusCode, T body) {
    public Response(HttpResponse<T> response) {
      this(response.statusCode(), response.body());
    }
  }

  public static record Weather(int temp, LocalDate date) {

  }

}
