package org.entryPoint.service.agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Agente de IA para normalização de cargos via Google Gemini API.
 * Adaptado rigorosamente aos limites do Free Tier:
 * - Máx 5 requisições/minuto (controlado com lotes grandes e intervalo de 15s)
 * - Máx 250k tokens de entrada/minuto (lotes de 200 cargos consom ~3k tokens)
 * - Máx 20 requisições/dia (com lote de 200, 360 cargos são processados em apenas 2 requisições)
 */
public class AgenteGemini {

  private static final String MODELO_PADRAO = "gemini-3.5-flash-lite";
  private static final String[] MODELOS_CANDIDATOS = {
    "gemini-3.5-flash-lite",
    "gemini-3.5-flash",
    "gemini-3.8-flash",
    "gemini-flash-latest",
    "gemini-3.7-flash"
  };

  // 200 cargos por lote -> ~2.5k tokens por requisição (bem abaixo dos 250k TPM)
  // Processa toda a base (361 cargos) em apenas 2 requisições, consumindo só 2 da cota diária de 20 RPD!
  private static final int TAMANHO_LOTE = 200;

  // 15 segundos de intervalo entre requisições -> Máximo de 4 RPM (garante ficar abaixo de 5 RPM)
  private static final int INTERVALO_ENTRE_LOTES_MS = 15000;

  private final String apiKey;
  private String modelo;
  private final HttpClient httpClient;

  public AgenteGemini() {
    this(System.getenv("GEMINI_API_KEY"), MODELO_PADRAO);
  }

  public AgenteGemini(String apiKey, String modelo) {
    this.apiKey = apiKey != null ? apiKey.trim() : "";
    this.modelo = modelo != null ? modelo : MODELO_PADRAO;
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  }

  public String getNome() {
    return "Google Gemini (" + modelo + ")";
  }

  public boolean isConfigurado() {
    return apiKey != null && !apiKey.isBlank();
  }

  public Map<String, String> normalizarCargos(List<String> cargosBrutos) throws Exception {
    return normalizarCargos(cargosBrutos, null);
  }

  public Map<String, String> normalizarCargos(List<String> cargosBrutos, LoteProcessadoCallback callback) throws Exception {
    if (!isConfigurado()) {
      throw new IllegalStateException(
        "A variável de ambiente GEMINI_API_KEY não foi encontrada.\n"
        + "Por favor, defina a variável no ambiente do sistema (ex: export GEMINI_API_KEY=\"sua_chave\") antes de executar."
      );
    }

    Map<String, String> resultadoFinal = new LinkedHashMap<>();
    List<List<String>> lotes = criarLotes(cargosBrutos, TAMANHO_LOTE);

    System.out.printf("Iniciando processamento de %d cargos divididos em %d requisição(ões) (Consumo diário: %d de 20)...%n",
      cargosBrutos.size(), lotes.size(), lotes.size());

    for (int i = 0; i < lotes.size(); i++) {
      List<String> lote = lotes.get(i);
      System.out.printf("%n[Lote %d/%d] Enviando %d cargos para o %s...%n",
        i + 1, lotes.size(), lote.size(), getNome());

      Map<String, String> normalizadosLote = processarLoteComRetentativas(lote);
      resultadoFinal.putAll(normalizadosLote);

      if (callback != null) {
        callback.aoProcessarLote(normalizadosLote);
      }

      // Respeitar limite de 5 requisições por minuto com pausa de 15 segundos
      if (i + 1 < lotes.size()) {
        System.out.printf("Aguardando %d segundos antes da próxima requisição (respeitando limite de 5 RPM)...%n",
          INTERVALO_ENTRE_LOTES_MS / 1000);
        Thread.sleep(INTERVALO_ENTRE_LOTES_MS);
      }
    }

    return resultadoFinal;
  }

  private Map<String, String> processarLoteComRetentativas(List<String> lote) throws Exception {
    int maxTentativas = 3;
    for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
      try {
        return processarLoteComFallback(lote);
      } catch (Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        boolean isRateLimit = msg.contains("429") || msg.contains("503") || msg.toLowerCase().contains("quota") || msg.toLowerCase().contains("demand");

        if (isRateLimit && tentativa < maxTentativas) {
          int esperaSegundos = 20 * tentativa;
          System.out.printf("Limite temporário ou alta demanda atingido. Aguardando %d segundos para retentativa %d/%d...%n",
            esperaSegundos, tentativa, maxTentativas);
          Thread.sleep(esperaSegundos * 1000L);
          continue;
        }
        throw e;
      }
    }
    throw new RuntimeException("Falha ao processar lote após múltiplas tentativas.");
  }

  private Map<String, String> processarLoteComFallback(List<String> lote) throws Exception {
    Exception ultimoErro = null;

    // Tentar com o modelo atual
    try {
      return processarLote(lote, this.modelo);
    } catch (Exception e) {
      ultimoErro = e;
    }

    // Se falhar (ex: modelo indisponível ou 404), tentar outros modelos candidatos
    for (String modeloCandidato : MODELOS_CANDIDATOS) {
      if (modeloCandidato.equals(this.modelo)) continue;
      try {
        System.out.printf("Tentando modelo alternativo '%s'...%n", modeloCandidato);
        Map<String, String> resultado = processarLote(lote, modeloCandidato);
        this.modelo = modeloCandidato; // Atualizar modelo ativo
        return resultado;
      } catch (Exception e) {
        ultimoErro = e;
      }
    }

    throw new RuntimeException("Erro ao processar lote no Gemini após testar modelos: " + (ultimoErro != null ? ultimoErro.getMessage() : "Desconhecido"), ultimoErro);
  }

  private Map<String, String> processarLote(List<String> lote, String modeloAlvo) throws Exception {
    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modeloAlvo + ":generateContent?key=" + apiKey;
    String prompt = construirPrompt(lote);

    JSONObject payload = new JSONObject();
    JSONArray contents = new JSONArray();
    JSONObject content = new JSONObject();
    JSONArray parts = new JSONArray();
    JSONObject part = new JSONObject();

    part.put("text", prompt);
    parts.put(part);
    content.put("parts", parts);
    contents.put(content);
    payload.put("contents", contents);

    JSONObject genConfig = new JSONObject();
    genConfig.put("responseMimeType", "application/json");
    payload.put("generationConfig", genConfig);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
      .timeout(Duration.ofSeconds(90))
      .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
    }

    return parsearResposta(response.body(), lote);
  }

  private String construirPrompt(List<String> lote) {
    JSONArray arrayJson = new JSONArray(lote);
    return """
      Você é um assistente especializado em higienização e padronização de dados de cargos públicos municipais e normas legislativas brasileiras.
      Sua tarefa é normalizar a lista de cargos em formato JSON abaixo, seguindo RIGOROSAMENTE as seguintes instruções:

      1. REMOVER TODOS OS ACENTOS E CEDILHAS (ex: SECRETÁRIO -> SECRETARIO, MUNICÍPIO -> MUNICIPIO, JUSTIÇA -> JUSTICA, GESTÃO -> GESTAO, SÃO -> SAO).
      2. CONVERTER TUDO PARA MAIÚSCULAS (UPPERCASE).
      3. REMOVER pontuações no início ou no fim (como '.', ';', ',', ']', '['. Ex: 'PREFEITO.' -> 'PREFEITO', 'PREFEITO;' -> 'PREFEITO').
      4. CORRIGIR fragmentações de sílabas, hífens de OCR e espaçamentos corrompidos (ex: 'FINAN-ÇAS' ou 'FINAN ÇAS' -> 'FINANCAS', 'PLANEJA MENTO' -> 'PLANEJAMENTO', 'ESPOR TES' -> 'ESPORTES', 'INFRA-ESTRUTURA' -> 'INFRAESTRUTURA').
      5. CORRIGIR palavras coladas e erros de digitação (ex: 'DOSNEGÓCIOS' -> 'DOS NEGOCIOS', 'MUNINICPAL' -> 'MUNICIPAL', 'SUSBSTITUTO' -> 'SUBSTITUTO', 'MUNICIPALDE' -> 'MUNICIPAL DE').
      6. REMOVER nomes de outros autores inseridos indevidamente no cargo por falha de scraping (ex: 'SECRETÁRIO MUNICIPAL DE ESPORTES E LAZER ORLANDO LINDÓRIO DE FARIA, SECRETÁRIO MUNICIPAL DA CASA CIVIL.' -> 'SECRETARIO MUNICIPAL DE ESPORTES E LAZER').
      7. PADRONIZAR designações de substituto e em exercício (ex: '= SUBSTITUTA' ou '-SUBSTITUTO' ou 'SUBSTITUTO' -> ' - SUBSTITUTO' / ' - SUBSTITUTA', 'EM EXERCICIO').
      8. PADRONIZAR 'RESPONDENDO PELO CARGO SECRETARIO' para 'RESPONDENDO PELO CARGO DE SECRETARIO'.

      Entrada (JSON):
      %s

      Retorne EXCLUSIVAMENTE um JSON estruturado no formato:
      {
        "resultados": [
          {
            "original": "texto original",
            "normalizado": "TEXTO NORMALIZADO SEM ACENTO"
          }
        ]
      }
      """.formatted(arrayJson.toString());
  }

  public static Map<String, String> parsearResposta(String responseBody, List<String> loteOriginal) {
    Map<String, String> mapa = new LinkedHashMap<>();
    try {
      JSONObject json = new JSONObject(responseBody);
      JSONArray candidates = json.optJSONArray("candidates");
      if (candidates == null || candidates.isEmpty()) {
        throw new RuntimeException("Resposta do Gemini vazia: " + responseBody);
      }

      JSONObject firstCandidate = candidates.getJSONObject(0);
      JSONObject content = firstCandidate.getJSONObject("content");
      JSONArray parts = content.getJSONArray("parts");
      
      // Encontrar a parte que contém o texto de resposta
      String text = null;
      for (int i = 0; i < parts.length(); i++) {
        JSONObject part = parts.getJSONObject(i);
        if (part.has("text") && !part.optBoolean("thought", false)) {
          text = part.getString("text");
        }
      }
      if (text == null && parts.length() > 0) {
        text = parts.getJSONObject(0).optString("text", "");
      }

      JSONObject respostaJson = new JSONObject(text);
      JSONArray resultados = respostaJson.getJSONArray("resultados");

      for (int i = 0; i < resultados.length(); i++) {
        JSONObject item = resultados.getJSONObject(i);
        String original = item.getString("original");
        String normalizado = item.getString("normalizado");
        normalizado = sanitizarTexto(normalizado);
        mapa.put(original, normalizado);
      }

    } catch (Exception e) {
      throw new RuntimeException("Falha ao processar resposta JSON da API Gemini: " + e.getMessage(), e);
    }

    // Garantir que todos os itens do lote original possuam valor normalizado
    for (String item : loteOriginal) {
      if (!mapa.containsKey(item)) {
        mapa.put(item, sanitizarTexto(item));
      }
    }

    return mapa;
  }

  public static String sanitizarTexto(String texto) {
    if (texto == null) return "";
    String nfd = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
    String semAcento = nfd.replaceAll("\\p{M}", "");
    return semAcento
      .replaceAll("[.,;:\\]\\[=\\-–—\\s]+$", "")
      .replaceAll("^[.,;:\\]\\[=\\-–—\\s]+", "")
      .replaceAll("\\s+", " ")
      .toUpperCase(Locale.ROOT)
      .trim();
  }

  private List<List<String>> criarLotes(List<String> lista, int tamanhoLote) {
    List<List<String>> lotes = new ArrayList<>();
    for (int i = 0; i < lista.size(); i += tamanhoLote) {
      int fim = Math.min(i + tamanhoLote, lista.size());
      lotes.add(lista.subList(i, fim));
    }
    return lotes;
  }

  @FunctionalInterface
  public interface LoteProcessadoCallback {
    void aoProcessarLote(Map<String, String> loteNormalizado) throws Exception;
  }
}
