package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.TopicoTematico;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;
import org.entryPoint.repository.RepositorioTopicos;

public class ServicoTopicModeling {

  private static final int DEFAULT_K = 12;
  private static final int DEFAULT_ITERACOES = 25;
  private static final int MIN_DF = 4;
  private static final double MAX_DF_RATIO = 0.22;

  private static final Set<String> STOPWORDS_LEGIS = Set.of(
    "dispõe", "dispoe", "altera", "dá", "da", "providências", "providencias", "outras",
    "lei", "artigo", "art", "arts", "anexo", "caput", "parágrafo", "paragrafo", "inciso",
    "institui", "aprova", "estabelece", "nº", "no", "redação", "redacao", "revoga", "acresce",
    "termo", "termos", "seguinte", "seguintes", "constante", "constantes", "quadro", "tabela",
    "sobre", "para", "com", "que", "dos", "das", "aos", "nas", "nos", "pelo", "pela",
    "janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    "alei", "especifica", "conforme", "previsto", "vigor", "municipal", "município", "municipio", "cidade", "são", "paulo"
  );

  private final RepositorioEmentasPreProcessadas repositorioEmentas;
  private final RepositorioTopicos repositorioTopicos;

  public ServicoTopicModeling() {
    this.repositorioEmentas = new RepositorioEmentasPreProcessadas();
    this.repositorioTopicos = new RepositorioTopicos();
  }

  public ServicoTopicModeling(RepositorioEmentasPreProcessadas repositorioEmentas, RepositorioTopicos repositorioTopicos) {
    this.repositorioEmentas = repositorioEmentas;
    this.repositorioTopicos = repositorioTopicos;
  }

  public ServicoTopicModeling(RepositorioBancoDados repositorio) {
    this.repositorioEmentas = repositorio.getRepositorioEmentas();
    this.repositorioTopicos = repositorio.getRepositorioTopicos();
  }

  public ResultadoTopicModeling executarTopicModeling() throws SQLException {
    return executarTopicModeling(DEFAULT_K, DEFAULT_ITERACOES, true);
  }

  public ResultadoTopicModeling executarTopicModeling(int k, int maxIteracoes, boolean salvarNoBanco) throws SQLException {
    long inicioTempo = System.nanoTime();
    Runtime runtime = Runtime.getRuntime();
    long memoriaInicio = runtime.totalMemory() - runtime.freeMemory();

    System.out.println("\n" + "=".repeat(75));
    System.out.printf("Iniciando Topic Modeling (NMF) - K=%d tópicos | MaxIter=%d...%n", k, maxIteracoes);
    System.out.println("=".repeat(75));

    // 1. Carregar ementas pré-processadas
    List<EmentaParaTopico> ementas = repositorioEmentas.listarEmentasParaTopicos();
    if (ementas.isEmpty()) {
      System.err.println("Nenhuma ementa pré-processada encontrada no banco de dados.");
      return new ResultadoTopicModeling(List.of(), 0, 0, 0, 0);
    }
    System.out.printf("Total de registros carregados: %d%n", ementas.size());

    // 2. Tokenizar e limpar
    List<List<String>> docsBrutos = new ArrayList<>();
    Map<String, Integer> contagemFrequenciaDoc = new HashMap<>();

    for (EmentaParaTopico item : ementas) {
      String texto = item.termos();
      if (texto == null || texto.isBlank()) continue;

      String[] tokens = texto.split("\\s+");
      Set<String> termosUnicos = new HashSet<>();
      List<String> docLimpo = new ArrayList<>();

      for (String t : tokens) {
        String limpo = t.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]", "").trim();
        if (limpo.length() > 2 && !limpo.matches("\\d+") && !STOPWORDS_LEGIS.contains(limpo)) {
          if (termosUnicos.add(limpo)) {
            docLimpo.add(limpo);
          }
        }
      }

      if (!docLimpo.isEmpty()) {
        docsBrutos.add(docLimpo);
        for (String w : termosUnicos) {
          contagemFrequenciaDoc.put(w, contagemFrequenciaDoc.getOrDefault(w, 0) + 1);
        }
      }
    }

    // 3. Filtrar vocabulário por DF
    int maxDf = (int) (MAX_DF_RATIO * docsBrutos.size());
    List<String> vocabularioOrdenado = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : contagemFrequenciaDoc.entrySet()) {
      int df = entry.getValue();
      if (df >= MIN_DF && df <= maxDf) {
        vocabularioOrdenado.add(entry.getKey());
      }
    }
    Collections.sort(vocabularioOrdenado);

    Map<String, Integer> vocabIndex = new HashMap<>();
    for (int i = 0; i < vocabularioOrdenado.size(); i++) {
      vocabIndex.put(vocabularioOrdenado.get(i), i);
    }
    int vSize = vocabularioOrdenado.size();

    // Montar matriz esparsa TF-IDF
    List<Map<Integer, Double>> matrizX = new ArrayList<>();
    int totalTokensProcessados = 0;

    for (int d = 0; d < docsBrutos.size(); d++) {
      List<String> palavras = docsBrutos.get(d);
      Map<Integer, Integer> tfLocal = new HashMap<>();
      for (String p : palavras) {
        Integer idx = vocabIndex.get(p);
        if (idx != null) {
          tfLocal.put(idx, tfLocal.getOrDefault(idx, 0) + 1);
        }
      }
      if (!tfLocal.isEmpty()) {
        Map<Integer, Double> vetorDoc = new HashMap<>();
        double somaQuadrados = 0.0;
        int totalPalavrasDoc = tfLocal.values().stream().mapToInt(Integer::intValue).sum();
        totalTokensProcessados += totalPalavrasDoc;

        for (Map.Entry<Integer, Integer> entry : tfLocal.entrySet()) {
          int wIdx = entry.getKey();
          String palavra = vocabularioOrdenado.get(wIdx);
          int df = contagemFrequenciaDoc.get(palavra);
          double tf = (double) entry.getValue() / totalPalavrasDoc;
          double idf = Math.log((docsBrutos.size() + 1.0) / (df + 1.0)) + 1.0;
          double tfidf = tf * idf;
          vetorDoc.put(wIdx, tfidf);
          somaQuadrados += tfidf * tfidf;
        }

        double normaL2 = Math.sqrt(somaQuadrados);
        if (normaL2 > 0) {
          for (Map.Entry<Integer, Double> entry : vetorDoc.entrySet()) {
            vetorDoc.put(entry.getKey(), entry.getValue() / normaL2);
          }
        }
        matrizX.add(vetorDoc);
      }
    }

    int dSize = matrizX.size();
    System.out.printf("Vocabulário refinado: %d termos únicos em %d documentos válidos.%n", vSize, dSize);

    // 4. Inicialização de Matrizes W (D x K) e H (K x V)
    Random rng = new Random(42);
    double[][] W = new double[dSize][k];
    double[][] H = new double[k][vSize];
    for (int d = 0; d < dSize; d++) {
      for (int t = 0; t < k; t++) {
        W[d][t] = 0.01 + rng.nextDouble() * 0.99;
      }
    }
    for (int t = 0; t < k; t++) {
      for (int v = 0; v < vSize; v++) {
        H[t][v] = 0.01 + rng.nextDouble() * 0.99;
      }
    }

    // 5. Atualizações Multiplicativas NMF
    double eps = 1e-9;
    for (int iter = 1; iter <= maxIteracoes; iter++) {
      // W^T * W (K x K)
      double[][] WTW = new double[k][k];
      for (int d = 0; d < dSize; d++) {
        for (int k1 = 0; k1 < k; k1++) {
          for (int k2 = 0; k2 < k; k2++) {
            WTW[k1][k2] += W[d][k1] * W[d][k2];
          }
        }
      }

      // W^T * X (K x V)
      double[][] WTX = new double[k][vSize];
      for (int d = 0; d < dSize; d++) {
        for (Map.Entry<Integer, Double> entry : matrizX.get(d).entrySet()) {
          int wIdx = entry.getKey();
          double val = entry.getValue();
          for (int t = 0; t < k; t++) {
            WTX[t][wIdx] += W[d][t] * val;
          }
        }
      }

      // Atualizar H
      for (int t = 0; t < k; t++) {
        for (int v = 0; v < vSize; v++) {
          double denom = eps;
          for (int k2 = 0; k2 < k; k2++) {
            denom += WTW[t][k2] * H[k2][v];
          }
          H[t][v] *= (WTX[t][v] + eps) / denom;
        }
      }

      // H * H^T (K x K)
      double[][] HHT = new double[k][k];
      for (int v = 0; v < vSize; v++) {
        for (int k1 = 0; k1 < k; k1++) {
          for (int k2 = 0; k2 < k; k2++) {
            HHT[k1][k2] += H[k1][v] * H[k2][v];
          }
        }
      }

      // Atualizar W
      for (int d = 0; d < dSize; d++) {
        double[] XHT = new double[k];
        for (Map.Entry<Integer, Double> entry : matrizX.get(d).entrySet()) {
          int wIdx = entry.getKey();
          double val = entry.getValue();
          for (int t = 0; t < k; t++) {
            XHT[t] += val * H[t][wIdx];
          }
        }
        for (int t = 0; t < k; t++) {
          double denom = eps;
          for (int k2 = 0; k2 < k; k2++) {
            denom += W[d][k2] * HHT[k2][t];
          }
          W[d][t] *= (XHT[t] + eps) / denom;
        }
      }

      if (iter % 10 == 0 || iter == maxIteracoes) {
        System.out.printf("  Iteração %d/%d concluída...%n", iter, maxIteracoes);
      }
    }

    // 6. Extrair Palavras Mais Importantes por Tópico e Identificar Nomes
    List<TopicoTematico> topicos = new ArrayList<>();

    for (int t = 0; t < k; t++) {
      List<TermoPeso> pesos = new ArrayList<>();
      for (int v = 0; v < vSize; v++) {
        pesos.add(new TermoPeso(vocabularioOrdenado.get(v), H[t][v]));
      }
      pesos.sort((a, b) -> Double.compare(b.peso, a.peso));
      List<String> topPalavras = pesos.stream().limit(6).map(TermoPeso::termo).toList();
      String nomeTopico = inferirRotuloTema(topPalavras);
      topicos.add(new TopicoTematico(t + 1, nomeTopico, topPalavras));
    }

    // 7. Persistir os tópicos descobertos no banco se solicitado
    if (salvarNoBanco) {
      System.out.println("Persistindo tópicos descobertos na tabela 'topicos'...");
      repositorioTopicos.salvarTopicosDescobertosEmLote(topicos);
    }

    long tempoTotalMs = (System.nanoTime() - inicioTempo) / 1_000_000;
    long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
    long memoriaUsadaMb = Math.max(0, (memoriaFinal - memoriaInicio) / (1024 * 1024));

    System.out.println("\n" + "=".repeat(75));
    System.out.printf("Topic Modeling Concluído em %d ms! (%d tópicos identificados)%n", tempoTotalMs, topicos.size());
    System.out.println("=".repeat(75));
    for (TopicoTematico topico : topicos) {
      System.out.println(topico.getDescricaoFormatada());
    }

    return new ResultadoTopicModeling(
      topicos,
      tempoTotalMs,
      totalTokensProcessados,
      memoriaUsadaMb,
      dSize
    );
  }

  private String inferirRotuloTema(List<String> topPalavras) {
    String concat = String.join(" ", topPalavras).toLowerCase(Locale.ROOT);
    if (concat.contains("utilidade") || (concat.contains("declara") && concat.contains("pública")) || concat.contains("entidade") || concat.contains("associação") || concat.contains("instituto")) {
      return "Declaração de Utilidade Pública";
    } else if (concat.contains("infantil") || concat.contains("educação") || concat.contains("creche") || concat.contains("escola") || concat.contains("ensino") || concat.contains("cei") || concat.contains("criação")) {
      return "Educação e Creches";
    } else if (concat.contains("calendário") || concat.contains("eventos") || concat.contains("comemorado") || concat.contains("anualmente") || concat.contains("semana") || concat.contains("dia") || concat.contains("conscientização")) {
      return "Datas Comemorativas e Calendário";
    } else if (concat.contains("denomina") || concat.contains("logradouro") || concat.contains("praça") || concat.contains("rua") || concat.contains("especifica") || concat.contains("situado") || concat.contains("avenida")) {
      return "Denominação de Vias e Logradouros";
    } else if (concat.contains("suplementar") || concat.contains("abre") || concat.contains("adicional") || concat.contains("crédito") || concat.contains("orçamento") || concat.contains("subvenções") || concat.contains("econômicas") || concat.contains("imposto")) {
      return "Crédito Orçamentário e Finanças";
    } else if (concat.contains("retifica") || concat.contains("confere") || concat.contains("altera") || concat.contains("redação") || concat.contains("ementa") || concat.contains("substitui") || concat.contains("anexo")) {
      return "Legislação Complementar / Retificações";
    } else if (concat.contains("saúde") || concat.contains("médic") || concat.contains("sanitár") || concat.contains("hospital") || concat.contains("enfermagem") || concat.contains("sus")) {
      return "Saúde Pública e Assistência";
    } else if (concat.contains("imóveis") || concat.contains("imóvel") || concat.contains("particulares") || concat.contains("desafeta") || concat.contains("desapropria")) {
      return "Bens Imóveis e Patrimônio Público";
    } else if (concat.contains("zeis") || concat.contains("fundiária") || (concat.contains("social") && concat.contains("interesse")) || concat.contains("habitação")) {
      return "Habitação e Regularização Fundiária";
    } else if (concat.contains("serviço") || concat.contains("preço") || concat.contains("tarifa") || concat.contains("prestados")) {
      return "Serviços Públicos e Tarifas";
    } else if (concat.contains("trânsito") || concat.contains("transporte") || concat.contains("veículo")) {
      return "Transportes e Trânsito Urbano";
    } else if (concat.contains("meio ambiente") || concat.contains("verde") || concat.contains("parque") || concat.contains("ambiental")) {
      return "Meio Ambiente e Áreas Verdes";
    } else {
      return "Administração e Normas Gerais (" + topPalavras.get(0) + ")";
    }
  }

  private record TermoPeso(String termo, double peso) {}

  public record ResultadoTopicModeling(
    List<TopicoTematico> topicos,
    long tempoMs,
    long totalTokens,
    long memoriaMb,
    int totalNormasProcessadas
  ) {}
}
