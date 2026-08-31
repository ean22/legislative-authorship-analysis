package org.entryPoint.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

import org.entryPoint.model.Law;
import org.entryPoint.model.LawResponse;
import org.entryPoint.model.LawSumary;
import org.entryPoint.model.RequestResult;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LawsSource {  
  private static final DatabaseService database =
    new DatabaseService();

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

      featchLaw(law);
      delayAleatorio();
    }
  }

  private static void featchLaw (LawSumary lawSumary) throws Exception{
    String url =
        "https://api.v2.leismunicipais.com.br/v2/municipais/normas/"
        + lawSumary.getId();

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()  
        .uri(URI.create(url))
        .GET()
        .header("Accept", "application/json")
        .build();

    HttpResponse<String> response =
        client.send(
          request,
          HttpResponse.BodyHandlers.ofString()
        );

    if (response.statusCode() != 200) {
      System.err.println(
        "Erro ao buscar norma " +
        lawSumary.getId() +
        ": " +
        response.statusCode()
      );

      return;
    }

    ObjectMapper mapper = new ObjectMapper();

    LawResponse result =
        mapper.readValue(response.body(), LawResponse.class);

    Law law = result.getData().getNorma();

    database.save(law);

    System.out.println(law.getTitulo());
    System.out.println(law.getEmenta());
    System.out.println(law.getIntegra());
  }

  private static void delayAleatorio() throws InterruptedException {
    int segundos = ThreadLocalRandom.current().nextInt(5, 9);
    Thread.sleep(segundos * 1000L);
  }
}