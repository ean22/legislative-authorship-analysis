package org.entryPoint.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.entryPoint.model.Law;
import org.entryPoint.model.LawResponse;
import org.entryPoint.model.LawSumary;
import org.entryPoint.model.RequestResult;
import org.entryPoint.repository.DatabaseRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LawsSource {  
  private static final String urlBase = "https://api.v2.leismunicipais.com.br/v2/municipais/normas/";
  private static ObjectMapper mapper = new ObjectMapper();
  private static int baseDelay = 400; // milliseconds
  private static final int maxDelay = 3000; // milliseconds
  private static final DatabaseRepository database = new DatabaseRepository();
  private static String initialDate = "2013-01-01";
  private static String finalDate = "2021-01-06";

  public static void access() {
    int limitPerPage = 100;
    int totalPaginas = 0;
    int paginaAtual = 0;

    HttpResponse<String> response = request(limitPerPage, 1);
    // System.out.println("Response: " + response.body());
    
    try {
      JsonNode json;
      json = mapper.readTree(response.body());
   
      totalPaginas = json.get("pages").asInt();
      paginaAtual = json.get("page").asInt();

    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    
    try {
      for (int page = paginaAtual; page <= totalPaginas; page++) {
        response = request(limitPerPage, page);
        
        System.out.println("Página " + page + " de " + totalPaginas + "\n\n");
        mapper(response);
      }
      
    } catch (Exception e) {
      System.err.println(e);

    } finally {
      System.out.println(
        """
          \n\n
          -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_

          Fim do processo de busca de normas.

          -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
        """);
    }
  }

  public static void listLaws() {
    database.listLaws();
  }

  public static void setDateRange(String initialDate, String finalDate) {
    LawsSource.initialDate = initialDate;
    LawsSource.finalDate = finalDate;
  }

  private static HttpResponse<String> request(int limitPerPage, int page) {
    String url = urlBase + "_search";

    String requestPayload = """
      {
        "limit": %d,
        "page": %d,
        "q": "",
        "data_final":"%s",
        "data_inicial":"%s",
        "score_first": false,
        "cidade": 5298,
        "sort": ["ano", "numero"]
      }
      """.formatted(limitPerPage, page, initialDate, finalDate);

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
        .build();

    try {
      while (true) {
        HttpResponse<String> response =
            client.send(
              request,
              HttpResponse.BodyHandlers.ofString()
            );

        if (response.statusCode() != 429) {
          delay(10000);
          return response;
        }

        System.err.println(
          "Rate limit atingido. Aguardando " +
          (baseDelay / 1000) +
          " segundos antes de tentar novamente."
        );

        delay(baseDelay);
        baseDelay = Math.min(baseDelay + 150, maxDelay);
      }
    } catch (Exception e) {
      System.err.println(e);
    }

    return null;
  }

  private static void mapper(HttpResponse<String> response) throws Exception {  
    RequestResult result = mapper.readValue(response.body(), RequestResult.class);

    for (LawSumary law : result.getData()) {
      // System.out.println(
      //   law.getYear() + " - " +
      //   law.getTypeWritten() + " " +
      //   law.getNumber()
      // );

      // System.out.println(law.getSummary());
      // System.out.println(law.getUrl());

      // System.out.println("-------------------------");

      featchLaw(law);
      delay(500);
    }
  }

  private static void featchLaw (LawSumary lawSumary) throws Exception{
    String url = urlBase + lawSumary.getId();

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

    LawResponse result =
        mapper.readValue(response.body(), LawResponse.class);

    Law law = result.getData().getNorma();

    // System.out.println(law.toString());
    // System.out.println("\n");

    database.saveRawLaw(law);

    System.out.println(law.getTitulo() + " - " + law.getDataOriginal());
  }

  private static void delay(int milisegundos) throws InterruptedException {
    // int segundos = ThreadLocalRandom.current().nextInt(origin, bound);
    Thread.sleep(milisegundos);
  }
}