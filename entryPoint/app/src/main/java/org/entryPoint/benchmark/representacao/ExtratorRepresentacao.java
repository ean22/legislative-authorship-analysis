package org.entryPoint.benchmark.representacao;

import java.util.*;
import org.entryPoint.benchmark.model.TipoRepresentacaoSemantica;
import org.entryPoint.model.EmentaParaTopico;

/**
 * Constrói as representações esparsas e densas do corpus legislativo,
 * calculando o vocabulário, TF-IDF, projeção latente contínua (SVD/LSA) e coocorrência de termos.
 */
public class ExtratorRepresentacao {

  public static final Set<String> STOPWORDS_LEGIS = Set.of(
      "dispõe", "dispoe", "altera", "dá", "da", "providências", "providencias", "outras",
      "lei", "artigo", "art", "arts", "anexo", "caput", "parágrafo", "paragrafo", "inciso",
      "institui", "aprova", "estabelece", "nº", "no", "redação", "redacao", "revoga", "acresce",
      "termo", "termos", "seguinte", "seguintes", "constante", "constantes", "quadro", "tabela",
      "sobre", "para", "com", "que", "dos", "das", "aos", "nas", "nos", "pelo", "pela",
      "janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro",
      "outubro", "novembro", "dezembro", "alei", "especifica", "conforme", "previsto", "vigor",
      "municipal", "município", "municipio", "cidade", "são", "paulo"
  );

  private final int minDf;
  private final double maxDfRatio;

  public ExtratorRepresentacao() {
    this(3, 0.35);
  }

  public ExtratorRepresentacao(int minDf, double maxDfRatio) {
    this.minDf = minDf;
    this.maxDfRatio = maxDfRatio;
  }

  public CorpusProcessado processar(List<EmentaParaTopico> ementas, TipoRepresentacaoSemantica tipoRepresentacao) {
    List<Long> docIdsValidos = new ArrayList<>();
    List<List<String>> docsTokens = new ArrayList<>();
    Map<String, Integer> docFreq = new HashMap<>();

    for (EmentaParaTopico item : ementas) {
      String texto = item.termos();
      if (texto == null || texto.isBlank()) {
        continue;
      }

      String[] tokens = texto.split("\\s+");
      Set<String> termosUnicos = new HashSet<>();
      List<String> docLimpo = new ArrayList<>();

      for (String t : tokens) {
        String limpo = t.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]", "").trim();
        if (limpo.length() > 2 && !limpo.matches("\\d+") && !STOPWORDS_LEGIS.contains(limpo)) {
          docLimpo.add(limpo);
          termosUnicos.add(limpo);
        }
      }

      if (!docLimpo.isEmpty()) {
        docIdsValidos.add(item.idNorma());
        docsTokens.add(docLimpo);
        for (String w : termosUnicos) {
          docFreq.put(w, docFreq.getOrDefault(w, 0) + 1);
        }
      }
    }

    int totalDocs = docsTokens.size();
    int maxDf = (int) Math.max(minDf + 1, maxDfRatio * totalDocs);

    List<String> vocabulario = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
      int df = entry.getValue();
      if (df >= minDf && df <= maxDf) {
        vocabulario.add(entry.getKey());
      }
    }
    Collections.sort(vocabulario);

    Map<String, Integer> vocabIndex = new HashMap<>();
    for (int i = 0; i < vocabulario.size(); i++) {
      vocabIndex.put(vocabulario.get(i), i);
    }
    int vSize = vocabulario.size();

    // 1. Matriz Esparsa TF-IDF e contagem de coocorrência de pares
    List<Map<Integer, Double>> matrizTfidf = new ArrayList<>(totalDocs);
    Map<Long, Integer> coocorrencias = new HashMap<>();
    int totalTokens = 0;

    for (int d = 0; d < totalDocs; d++) {
      List<String> palavras = docsTokens.get(d);
      Map<Integer, Integer> tfLocal = new HashMap<>();
      Set<Integer> indicesNoDoc = new HashSet<>();

      for (String p : palavras) {
        Integer idx = vocabIndex.get(p);
        if (idx != null) {
          tfLocal.put(idx, tfLocal.getOrDefault(idx, 0) + 1);
          indicesNoDoc.add(idx);
        }
      }

      // Par a par para cálculo de coerência
      List<Integer> listaIndicesDoc = new ArrayList<>(indicesNoDoc);
      Collections.sort(listaIndicesDoc);
      for (int i = 0; i < listaIndicesDoc.size(); i++) {
        long w1 = listaIndicesDoc.get(i);
        for (int j = i + 1; j < listaIndicesDoc.size(); j++) {
          long w2 = listaIndicesDoc.get(j);
          long parChave = (w1 << 32) | (w2 & 0xFFFFFFFFL);
          coocorrencias.put(parChave, coocorrencias.getOrDefault(parChave, 0) + 1);
        }
      }

      Map<Integer, Double> vetorDoc = new HashMap<>();
      if (!tfLocal.isEmpty()) {
        int docLen = tfLocal.values().stream().mapToInt(Integer::intValue).sum();
        totalTokens += docLen;
        double somaQuad = 0.0;

        for (Map.Entry<Integer, Integer> entry : tfLocal.entrySet()) {
          int wIdx = entry.getKey();
          int df = docFreq.get(vocabulario.get(wIdx));
          double tf = (double) entry.getValue() / docLen;
          double idf = Math.log((totalDocs + 1.0) / (df + 1.0)) + 1.0;
          double tfidf = tf * idf;
          vetorDoc.put(wIdx, tfidf);
          somaQuad += tfidf * tfidf;
        }

        double normaL2 = Math.sqrt(somaQuad);
        if (normaL2 > 0) {
          for (Map.Entry<Integer, Double> entry : vetorDoc.entrySet()) {
            vetorDoc.put(entry.getKey(), entry.getValue() / normaL2);
          }
        }
      }
      matrizTfidf.add(vetorDoc);
    }

    // 2. Matriz Densa (se solicitada ou para clustering/SVD)
    double[][] matrizDensa = null;
    int dimensoesDensas = Math.min(32, Math.max(8, vSize / 4));
    if (vSize > 0 && totalDocs > 0) {
      matrizDensa = projetarEspacoLatenteDenso(matrizTfidf, totalDocs, vSize, dimensoesDensas);
    } else {
      matrizDensa = new double[totalDocs][1];
    }

    return new CorpusProcessado(
        docIdsValidos,
        docsTokens,
        vocabulario,
        vocabIndex,
        docFreq,
        matrizTfidf,
        matrizDensa,
        totalTokens,
        coocorrencias
    );
  }

  /**
   * Projeta a matriz esparsa TF-IDF em um espaço vetorial contínuo denso
   * via projeção aleatória ortogonalizável (Random Projection / LSA aproximada).
   */
  private double[][] projetarEspacoLatenteDenso(
      List<Map<Integer, Double>> matrizTfidf,
      int numDocs,
      int vSize,
      int dimensoes
  ) {
    double[][] densa = new double[numDocs][dimensoes];
    Random rng = new Random(42L);

    // Matriz de projeção R (vSize x dimensoes) esparsa de Achlioptas
    double[][] R = new double[vSize][dimensoes];
    for (int v = 0; v < vSize; v++) {
      for (int c = 0; c < dimensoes; c++) {
        double r = rng.nextDouble();
        if (r < 1.0 / 6.0) {
          R[v][c] = Math.sqrt(3.0);
        } else if (r < 2.0 / 6.0) {
          R[v][c] = -Math.sqrt(3.0);
        } else {
          R[v][c] = 0.0;
        }
      }
    }

    // Multiplicação X * R
    for (int d = 0; d < numDocs; d++) {
      Map<Integer, Double> linha = matrizTfidf.get(d);
      for (Map.Entry<Integer, Double> entry : linha.entrySet()) {
        int vIdx = entry.getKey();
        double val = entry.getValue();
        for (int c = 0; c < dimensoes; c++) {
          densa[d][c] += val * R[vIdx][c];
        }
      }

      // Normalização L2 do vetor denso resultante
      double somaQuad = 0.0;
      for (int c = 0; c < dimensoes; c++) {
        somaQuad += densa[d][c] * densa[d][c];
      }
      double norma = Math.sqrt(somaQuad);
      if (norma > 1e-9) {
        for (int c = 0; c < dimensoes; c++) {
          densa[d][c] /= norma;
        }
      }
    }

    return densa;
  }
}
