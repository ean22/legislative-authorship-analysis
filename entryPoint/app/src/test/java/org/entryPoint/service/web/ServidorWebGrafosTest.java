package org.entryPoint.service.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;

import org.entryPoint.service.ServicoPerfilPolitico;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServidorWebGrafosTest {
  private ServidorWebGrafos servidor;
  private ServicoPerfilPolitico servicoPerfil;
  private HttpClient httpClient;

  @BeforeEach
  public void setUp() {
    servicoPerfil = new ServicoPerfilPolitico();
    servidor = new ServidorWebGrafos(servicoPerfil);
    httpClient = HttpClient.newHttpClient();
  }

  @AfterEach
  public void tearDown() {
    if (servidor != null && servidor.isAtivo()) {
      servidor.parar();
    }
  }

  @Test
  public void testInicializacaoServidorEEndpoints() throws IOException, InterruptedException {
    int porta = servidor.iniciar(8890);
    assertTrue(porta > 0);
    assertTrue(servidor.isAtivo());
    String baseUrl = servidor.getUrlBase();
    assertNotNull(baseUrl);

    // 1. Testar Rota Principal HTML (/)
    HttpRequest reqHtml = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/"))
        .GET()
        .build();
    HttpResponse<String> resHtml = httpClient.send(reqHtml, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resHtml.statusCode());
    assertTrue(resHtml.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    assertTrue(resHtml.body().contains("Rede Legislativa Interativa"));
    assertTrue(resHtml.body().contains("vis-network"));

    // 2. Testar Endpoint REST /api/grafo
    HttpRequest reqGrafo = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/grafo?limit=15&minWeight=25"))
        .GET()
        .build();
    HttpResponse<String> resGrafo = httpClient.send(reqGrafo, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resGrafo.statusCode());
    assertTrue(resGrafo.body().contains("\"nodes\": ["));
    assertTrue(resGrafo.body().contains("\"links\": ["));

    // 3. Testar Endpoint REST /api/autores
    HttpRequest reqAutores = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/autores?limit=5"))
        .GET()
        .build();
    HttpResponse<String> resAutores = httpClient.send(reqAutores, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resAutores.statusCode());
    assertTrue(resAutores.body().contains("FERNANDO HADDAD") || resAutores.body().contains("RICARDO NUNES"));

    // 4. Testar Endpoint REST /api/perfil
    HttpRequest reqPerfil = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/perfil?id=1731"))
        .GET()
        .build();
    HttpResponse<String> resPerfil = httpClient.send(reqPerfil, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resPerfil.statusCode());
    assertTrue(resPerfil.body().contains("FERNANDO HADDAD"));
    assertTrue(resPerfil.body().contains("perfilTematico"));

    // 5. Testar Endpoint REST /api/comparar
    HttpRequest reqComparar = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/comparar?id1=1731&id2=2006"))
        .GET()
        .build();
    HttpResponse<String> resComparar = httpClient.send(reqComparar, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resComparar.statusCode());
    assertTrue(resComparar.body().contains("similaridadeTematicaCosseno"));

    // Parar servidor
    servidor.parar();
    assertFalse(servidor.isAtivo());
  }

  @Test
  public void testGeracaoArquivoHtmlInterativo() throws SQLException, IOException {
    File permHtml = new File("../../rede_interativa.html");
    File gerado = servicoPerfil.gerarPaginaWebInterativa(permHtml);
    assertNotNull(gerado);
    assertTrue(gerado.exists());
    assertTrue(gerado.length() > 5000, "Arquivo HTML deve conter template e dados estruturados");
  }
}
