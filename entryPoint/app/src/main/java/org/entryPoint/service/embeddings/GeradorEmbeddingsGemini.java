package org.entryPoint.service.embeddings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Gerador de embeddings utilizando a API Google Gemini (text-embedding-004).
 */
public class GeradorEmbeddingsGemini implements GeradorEmbeddings {

  private static final String MODELO_PADRAO = "text-embedding-004";
  private static final int DIMENSAO = 768;
  private static final int TAMANHO_MAX_LOTE_API = 100;

  private final String apiKey;
  private final String modelo;
  private final HttpClient httpClient;

  public GeradorEmbeddingsGemini() {
    this(System.getenv("GEMINI_API_KEY"), MODELO_PADRAO);
  }

  public GeradorEmbeddingsGemini(String apiKey) {
    this(apiKey, MODELO_PADRAO);
  }

  public GeradorEmbeddingsGemini(String apiKey, String modelo) {
    this.apiKey = apiKey != null ? apiKey.trim() : "";
    this.modelo = modelo != null && !modelo.isBlank() ? modelo.trim() : MODELO_PADRAO;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
  }

  @Override
  public String getNome() {
    return "Google Gemini (" + modelo + ")";
  }

  @Override
  public int getDimensao() {
    return DIMENSAO;
  }

  @Override
  public boolean isDisponivel() {
    return apiKey != null && !apiKey.isBlank();
  }

  @Override
  public float[] gerarEmbedding(String texto) throws Exception {
    List<float[]> lista = gerarEmbeddingsEmLote(List.of(texto != null ? texto : ""));
    return lista.isEmpty() ? new float[DIMENSAO] : lista.get(0);
  }

  @Override
  public List<float[]> gerarEmbeddingsEmLote(List<String> textos) throws Exception {
    if (!isDisponivel()) {
      throw new IllegalStateException(
          "A variável de ambiente GEMINI_API_KEY não foi configurada no sistema.\n" +
          "Defina export GEMINI_API_KEY=\"sua_chave\" ou utilize o gerador local offline.");
    }

    if (textos == null || textos.isEmpty()) {
      return List.of();
    }

    List<float[]> todosEmbeddings = new ArrayList<>(textos.size());

    for (int i = 0; i < textos.size(); i += TAMANHO_MAX_LOTE_API) {
      int fim = Math.min(i + TAMANHO_MAX_LOTE_API, textos.size());
      List<String> subLote = textos.subList(i, fim);
      List<float[]> loteProcessado = requisitarLoteComRetentativas(subLote);
      todosEmbeddings.addAll(loteProcessado);
    }

    return todosEmbeddings;
  }

  private List<float[]> requisitarLoteComRetentativas(List<String> subLote) throws Exception {
    int maxTentativas = 3;
    for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
      try {
        return requisitarBatchEmbedContents(subLote);
      } catch (Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        boolean isRateLimit = msg.contains("429") || msg.contains("503") || msg.toLowerCase().contains("quota") || msg.toLowerCase().contains("resource exhausted");
        if (isRateLimit && tentativa < maxTentativas) {
          int espera = 10 * tentativa;
          System.out.printf("  [Gemini Rate Limit] Aguardando %ds para retentativa %d/%d...%n", espera, tentativa, maxTentativas);
          Thread.sleep(espera * 1000L);
          continue;
        }
        throw e;
      }
    }
    throw new RuntimeException("Falha ao obter embeddings do Gemini após " + maxTentativas + " tentativas.");
  }

  private List<float[]> requisitarBatchEmbedContents(List<String> subLote) throws Exception {
    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo + ":batchEmbedContents?key=" + apiKey;

    JSONObject payload = new JSONObject();
    JSONArray requests = new JSONArray();

    for (String texto : subLote) {
      String textoSanitizado = texto != null ? texto.trim() : "";
      if (textoSanitizado.isBlank()) {
        textoSanitizado = "sem ementa";
      }

      JSONObject req = new JSONObject();
      req.put("model", "models/" + modelo);

      JSONObject content = new JSONObject();
      JSONArray parts = new JSONArray();
      JSONObject part = new JSONObject();
      part.put("text", textoSanitizado);
      parts.put(part);
      content.put("parts", parts);
      req.put("content", content);

      requests.put(req);
    }

    payload.put("requests", requests);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
        .timeout(Duration.ofSeconds(60))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException("HTTP " + response.statusCode() + " na API Gemini Embeddings: " + response.body());
    }

    JSONObject responseJson = new JSONObject(response.body());
    JSONArray embeddingsArray = responseJson.optJSONArray("embeddings");

    if (embeddingsArray == null) {
      throw new RuntimeException("Resposta da API Gemini não continha o array 'embeddings': " + response.body());
    }

    List<float[]> resultado = new ArrayList<>(embeddingsArray.length());
    for (int i = 0; i < embeddingsArray.length(); i++) {
      JSONObject embObj = embeddingsArray.getJSONObject(i);
      JSONArray values = embObj.getJSONArray("values");
      float[] vetor = new float[values.length()];
      for (int v = 0; v < values.length(); v++) {
        vetor[v] = (float) values.getDouble(v);
      }
      normalizarL2(vetor);
      resultado.add(vetor);
    }

    return resultado;
  }

  private static void normalizarL2(float[] vetor) {
    if (vetor == null || vetor.length == 0) return;
    double soma = 0.0;
    for (float v : vetor) {
      soma += (double) v * v;
    }
    double norma = Math.sqrt(soma);
    if (norma > 1e-12) {
      for (int i = 0; i < vetor.length; i++) {
        vetor[i] = (float) (vetor[i] / norma);
      }
    }
  }
}
