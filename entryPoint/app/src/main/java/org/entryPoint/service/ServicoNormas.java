package org.entryPoint.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.entryPoint.model.Norma;
import org.entryPoint.model.RespostaNorma;
import org.entryPoint.model.ResumoNorma;
import org.entryPoint.model.ResultadoRequisicao;
import org.entryPoint.repository.RepositorioBancoDados;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServicoNormas {
  private final String URL_BASE = "https://api.v2.leismunicipais.com.br/v2/municipais/normas/";
  private final ObjectMapper MAPEADOR = new ObjectMapper();
  private int atrasoAtual = 400;
  private final int ATRASO_MAXIMO = 3000;
  private final RepositorioBancoDados repositorio = new RepositorioBancoDados();
  private String dataInicial = "2013-01-01";
  private String dataFinal = "2021-01-06";

  public void buscarNormas() {
    int limitePorPagina = 100;
    int totalPaginas = 0;
    int paginaAtual = 0;

    HttpResponse<String> resposta = solicitarPagina(limitePorPagina, 1);
    // System.out.println("Response: " + resposta.body());
    
    try {
      JsonNode json;
      json = MAPEADOR.readTree(resposta.body());
   
      totalPaginas = json.get("pages").asInt();
      paginaAtual = json.get("page").asInt();

    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    
    try {
      for (int pagina = paginaAtual; pagina <= totalPaginas; pagina++) {
        resposta = solicitarPagina(limitePorPagina, pagina);
        
        System.out.println("Página " + pagina + " de " + totalPaginas + "\n\n");
        processarResposta(resposta);
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

  public void listarNormas() {
    repositorio.listarNormas();
  }

  public void definirIntervaloDatas(String dataInicial, String dataFinal) {
    this.dataInicial = dataInicial;
    this.dataFinal = dataFinal;
  }

  private HttpResponse<String> solicitarPagina(int limitePorPagina, int pagina) {
    String url = URL_BASE + "_search";

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
      """.formatted(limitePorPagina, pagina, dataInicial, dataFinal);

    HttpClient cliente = HttpClient.newHttpClient();

    HttpRequest requisicao = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
        .build();

    try {
      while (true) {
        HttpResponse<String> resposta =
            cliente.send(
              requisicao,
              HttpResponse.BodyHandlers.ofString()
            );

        if (resposta.statusCode() != 429) {
          aguardar(10000);
          return resposta;
        }

        System.err.println(
          "Rate limit atingido. Aguardando " +
          (atrasoAtual / 1000) +
          " segundos antes de tentar novamente."
        );

        aguardar(atrasoAtual);
        atrasoAtual = Math.min(atrasoAtual + 150, ATRASO_MAXIMO);
      }
    } catch (Exception e) {
      System.err.println(e);
    }

    return null;
  }

  private void processarResposta(HttpResponse<String> resposta) throws Exception {
    ResultadoRequisicao resultado = MAPEADOR.readValue(resposta.body(), ResultadoRequisicao.class);

    for (ResumoNorma resumoNorma : resultado.getData()) {
      // System.out.println(
      //   law.getYear() + " - " +
      //   law.getTypeWritten() + " " +
      //   law.getNumber()
      // );

      // System.out.println(law.getSummary());
      // System.out.println(law.getUrl());

      // System.out.println("-------------------------");

      buscarNorma(resumoNorma);
      aguardar(500);
    }
  }

  private void buscarNorma(ResumoNorma resumoNorma) throws Exception {
    String url = URL_BASE + resumoNorma.getId();

    HttpClient cliente = HttpClient.newHttpClient();

    HttpRequest requisicao = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .header("Accept", "application/json")
        .build();

    HttpResponse<String> resposta =
        cliente.send(
          requisicao,
          HttpResponse.BodyHandlers.ofString()
        );

    if (resposta.statusCode() != 200) {
      System.err.println(
        "Erro ao buscar norma " +
        resumoNorma.getId() +
        ": " +
        resposta.statusCode()
      );

      return;
    }

    RespostaNorma resultado =
      MAPEADOR.readValue(resposta.body(), RespostaNorma.class);

    Norma norma = resultado.getData().getNorma();

    // System.out.println(law.toString());
    // System.out.println("\n");

    repositorio.salvarNorma(norma);

    System.out.println(norma.getTitulo() + " - " + norma.getDataOriginal());
  }

  private void aguardar(int milissegundos) throws InterruptedException {
    // int segundos = ThreadLocalRandom.current().nextInt(origin, bound);
    Thread.sleep(milissegundos);
  }
}