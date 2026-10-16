package org.entryPoint.service.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.entryPoint.model.perfil.ComparativoPerfisAutores;
import org.entryPoint.model.perfil.GrafoColaboracaoPolitica;
import org.entryPoint.model.perfil.PerfilPoliticoAutor;
import org.entryPoint.service.ServicoPerfilPolitico;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servidor HTTP embutido de alta performance para hospedar a interface web interativa
 * e fornecer endpoints REST para o frontend visual dos grafos de colaboração e perfis políticos.
 */
public class ServidorWebGrafos {
  private final ServicoPerfilPolitico servicoPerfil;
  private final ObjectMapper objectMapper;
  private HttpServer servidor;
  private int portaAtiva = -1;

  public ServidorWebGrafos() {
    this(new ServicoPerfilPolitico());
  }

  public ServidorWebGrafos(ServicoPerfilPolitico servicoPerfil) {
    this.servicoPerfil = servicoPerfil;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Inicia o servidor HTTP, tentando a porta informada ou as portas subsequentes caso ocupada.
   */
  public synchronized int iniciar(int portaPreferencial) throws IOException {
    if (servidor != null) {
      return portaAtiva;
    }

    int porta = portaPreferencial > 0 ? portaPreferencial : 8080;
    int maxTentativas = 10;
    IOException ultimoErro = null;

    for (int i = 0; i < maxTentativas; i++) {
      try {
        servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        portaAtiva = porta;
        break;
      } catch (IOException e) {
        ultimoErro = e;
        porta++;
      }
    }

    if (servidor == null) {
      throw new IOException("Não foi possível iniciar o servidor HTTP nas portas testadas: " + ultimoErro.getMessage(), ultimoErro);
    }

    // Configuração dos contextos / rotas
    servidor.createContext("/", new HandlerPaginaPrincipal());
    servidor.createContext("/api/grafo", new HandlerApiGrafo());
    servidor.createContext("/api/grafo/autor", new HandlerApiGrafoAutor());
    servidor.createContext("/api/perfil", new HandlerApiPerfil());
    servidor.createContext("/api/comparar", new HandlerApiComparar());
    servidor.createContext("/api/autores", new HandlerApiAutores());
    servidor.createContext("/api/stats", new HandlerApiEstatisticas());
    servidor.createContext("/api/estatisticas", new HandlerApiEstatisticas());
    servidor.createContext("/api/topicos", new HandlerApiTopicos());

    servidor.setExecutor(null); // Executor padrão
    servidor.start();

    return portaAtiva;
  }

  public synchronized void parar() {
    if (servidor != null) {
      servidor.stop(1);
      servidor = null;
      portaAtiva = -1;
    }
  }

  public boolean isAtivo() {
    return servidor != null;
  }

  public int getPorta() {
    return portaAtiva;
  }

  public String getUrlBase() {
    return isAtivo() ? "http://localhost:" + portaAtiva : null;
  }

  /**
   * Tenta abrir o navegador padrão do sistema operacional com a URL da interface web.
   */
  public void abrirNavegadorPadrao() {
    if (!isAtivo()) return;
    String url = getUrlBase();
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
        return;
      }
    } catch (Exception ignored) {}

    // Fallback para ambiente Linux via comando xdg-open
    try {
      Runtime.getRuntime().exec(new String[]{"xdg-open", url});
    } catch (Exception ignored) {}
  }

  private class HandlerPaginaPrincipal implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      if (!"/".equals(path) && !"/index.html".equals(path)) {
        enviarResposta(exchange, 404, "text/plain", "Não encontrado");
        return;
      }

      String html = PaginaWebGrafoTemplate.gerarHtml();
      enviarResposta(exchange, 200, "text/html; charset=UTF-8", html);
    }
  }

  private class HandlerApiGrafo implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> params = extrairParametros(exchange.getRequestURI());
      int minRank = parseOrDefault(params.get("minRank"), 1);
      int maxRank = parseOrDefault(params.get("maxRank"), parseOrDefault(params.get("limit"), 40));
      int pesoMin = parseOrDefault(params.get("minWeight"), 20);

      try {
        GrafoColaboracaoPolitica grafo = servicoPerfil.construirGrafoColaboracao(minRank, maxRank, pesoMin);
        String json = grafo.exportarJsonD3(pesoMin);
        enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiGrafoAutor implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> params = extrairParametros(exchange.getRequestURI());
      long id = parseLongOrDefault(params.get("id"), -1L);
      int pesoMin = parseOrDefault(params.get("minWeight"), 10);

      if (id <= 0) {
        enviarResposta(exchange, 400, "application/json", "{\"error\":\"ID de autor inválido\"}");
        return;
      }

      try {
        Optional<GrafoColaboracaoPolitica> optGrafo = servicoPerfil.construirGrafoColaboracaoAutor(id, pesoMin);
        if (optGrafo.isPresent()) {
          String json = optGrafo.get().exportarJsonD3(pesoMin);
          enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
        } else {
          enviarResposta(exchange, 404, "application/json", "{\"error\":\"Autor não localizado\"}");
        }
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiPerfil implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> params = extrairParametros(exchange.getRequestURI());
      String idStr = params.get("id");
      String nome = params.get("nome");

      try {
        Optional<PerfilPoliticoAutor> optPerfil = (idStr != null)
            ? servicoPerfil.gerarPerfilAutor(Long.parseLong(idStr))
            : (nome != null ? servicoPerfil.gerarPerfilAutor(nome) : Optional.empty());

        if (optPerfil.isPresent()) {
          String json = objectMapper.writeValueAsString(optPerfil.get());
          enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
        } else {
          enviarResposta(exchange, 404, "application/json", "{\"error\":\"Perfil não localizado\"}");
        }
      } catch (Exception e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiComparar implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> params = extrairParametros(exchange.getRequestURI());
      long id1 = parseLongOrDefault(params.get("id1"), -1L);
      long id2 = parseLongOrDefault(params.get("id2"), -1L);

      if (id1 <= 0 || id2 <= 0) {
        enviarResposta(exchange, 400, "application/json", "{\"error\":\"IDs de comparação inválidos\"}");
        return;
      }

      try {
        Optional<ComparativoPerfisAutores> optComp = servicoPerfil.compararAutores(id1, id2);
        if (optComp.isPresent()) {
          String json = objectMapper.writeValueAsString(optComp.get());
          enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
        } else {
          enviarResposta(exchange, 404, "application/json", "{\"error\":\"Autores não encontrados\"}");
        }
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiAutores implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      Map<String, String> params = extrairParametros(exchange.getRequestURI());
      int limite = parseOrDefault(params.get("limit"), 80);

      try {
        var perfis = servicoPerfil.gerarPerfisPrincipaisAutores(limite);
        var stats = servicoPerfil.obterEstatisticasGerais();
        Map<String, Double> vetorGlobal = new HashMap<>();
        if (stats.topicos() != null) {
          for (var t : stats.topicos()) {
            vetorGlobal.put(t.nome(), (double) t.totalNormas());
          }
        }
        double normaGlobal = 0.0;
        for (double v : vetorGlobal.values()) normaGlobal += (v * v);
        double sqrtNormaGlobal = Math.sqrt(normaGlobal);

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < perfis.size(); i++) {
          var p = perfis.get(i);
          double produto = 0.0;
          double normaP = 0.0;
          for (var dt : p.perfilTematico().distribuicaoTopicos()) {
            double vP = dt.percentual();
            normaP += (vP * vP);
            double vG = vetorGlobal.getOrDefault(dt.nomeTopico(), 0.0);
            produto += (vP * vG);
          }
          double simCosseno = (normaP > 0 && sqrtNormaGlobal > 0)
              ? (produto / (Math.sqrt(normaP) * sqrtNormaGlobal))
              : 0.0;
          double hhi = p.perfilTematico() != null ? p.perfilTematico().indiceEspecializacaoHhi() : 0.0;
          double entropia = p.perfilTematico() != null ? p.perfilTematico().entropiaTematica() : 0.0;
          double pctDominante = p.perfilTematico() != null ? p.perfilTematico().percentualTopicoDominante() : 0.0;
          int normasIndividuais = p.perfilAtuacao() != null ? p.perfilAtuacao().normasIndividuais() : 0;
          int normasColetivas = p.perfilAtuacao() != null ? p.perfilAtuacao().normasColetivas() : 0;
          double mediaAutoresPorNorma = p.perfilAtuacao() != null ? p.perfilAtuacao().mediaAutoresPorNorma() : 1.0;
          int totalParceiros = p.perfilColaboracao() != null ? p.perfilColaboracao().totalParceirosDistintos() : 0;
          double intensidadeMedia = p.perfilColaboracao() != null ? p.perfilColaboracao().intensidadeMediaColaboracao() : 0.0;
          double mediaAnual = p.perfilTemporal() != null ? p.perfilTemporal().mediaNormasPorAno() : 0.0;

          sb.append(String.format(java.util.Locale.US,
              "{\"id\":%d,\"name\":\"%s\",\"cargo\":\"%s\",\"totalNormas\":%d,\"temaDominante\":\"%s\","
              + "\"hhi\":%.4f,\"taxaCoautoria\":%.2f,\"cosseno\":%.4f,\"entropia\":%.4f,\"pctDominante\":%.2f,"
              + "\"normasIndividuais\":%d,\"normasColetivas\":%d,\"mediaAutoresPorNorma\":%.2f,"
              + "\"totalParceiros\":%d,\"intensidadeMedia\":%.2f,\"mediaAnual\":%.2f}",
              p.idAutor(), escapar(p.nomeAutor()), escapar(p.cargoPrincipal()),
              p.perfilAtuacao().totalNormas(), escapar(p.perfilTematico().topicoDominante()),
              hhi, p.perfilAtuacao().taxaCoautoria(), simCosseno, entropia, pctDominante,
              normasIndividuais, normasColetivas, mediaAutoresPorNorma,
              totalParceiros, intensidadeMedia, mediaAnual));
          if (i < perfis.size() - 1) sb.append(",");
        }
        sb.append("]");
        enviarResposta(exchange, 200, "application/json; charset=UTF-8", sb.toString());
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiEstatisticas implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        var stats = servicoPerfil.obterEstatisticasGerais();
        String json = objectMapper.writeValueAsString(stats);
        enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private class HandlerApiTopicos implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        var topicos = servicoPerfil.listarTopicosBanco();
        String json = objectMapper.writeValueAsString(topicos);
        enviarResposta(exchange, 200, "application/json; charset=UTF-8", json);
      } catch (SQLException e) {
        enviarResposta(exchange, 500, "application/json", "{\"error\":\"" + escapar(e.getMessage()) + "\"}");
      }
    }
  }

  private void enviarResposta(HttpExchange exchange, int statusCode, String contentType, String corpo) throws IOException {
    byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private Map<String, String> extrairParametros(URI uri) {
    Map<String, String> map = new HashMap<>();
    String query = uri.getRawQuery();
    if (query == null || query.isBlank()) return map;

    String[] pares = query.split("&");
    for (String par : pares) {
      String[] kv = par.split("=", 2);
      if (kv.length == 2) {
        map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
      }
    }
    return map;
  }

  private int parseOrDefault(String val, int def) {
    try {
      return (val != null) ? Integer.parseInt(val) : def;
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private long parseLongOrDefault(String val, long def) {
    try {
      return (val != null) ? Long.parseLong(val) : def;
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private String escapar(String s) {
    if (s == null) return "";
    return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
  }
}
