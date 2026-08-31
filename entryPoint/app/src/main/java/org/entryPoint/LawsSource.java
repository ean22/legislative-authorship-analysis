package org.entryPoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.entryPoint.model.LawSumary;
import org.entryPoint.model.RequestResult;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LawsSource {  
  public static void access() {

    String url =
      "https://api.v2.leismunicipais.com.br/v2/municipais/normas/_search";

    String requestPayload = """
      {
        "limit": 8,
        "page": 1,
        "q": "2026",
        "data_final":"2025-12-31",
        "data_inicial":"2025-01-01",
        "score_first": false,
        "cidade": 5298,
        "sort": ["ano", "numero"]
      }
      """;

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
        .build();

    try {
      HttpResponse<String> response =
          client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
          );
          
        mapper(response);

    } catch (Exception e) {
        System.err.println(e);
    }
  }

  private static void mapper(HttpResponse<String> response) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    
    RequestResult result = mapper.readValue(response.body(), RequestResult.class);

    for (LawSumary law : result.getData()) {
      System.out.println(
        law.getYear() + " - " +
        law.getTypeWritten() + " " +
        law.getNumber()
      );

      System.out.println(law.getSummary());
      System.out.println(law.getUrl());

      System.out.println("-------------------------");

      searcher(law);
    }
  }

  private static void searcher(LawSumary law) throws Exception{
    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(law.getUrl().toString()))
        .GET()
        .header("User-Agent", "Mozilla/5.0")
        .build();

    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println(response.statusCode());
    System.out.println(response.headers());
    System.out.println(response.body().substring(
        0,
        Math.min(1000, response.body().length())
    ));
  }
}