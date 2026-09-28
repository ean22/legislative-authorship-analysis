package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstrategiaTfidfTopK implements EstrategiaReducao {
  private final int topK;

  public EstrategiaTfidfTopK(int topK) {
    this.topK = topK;
  }

  public EstrategiaTfidfTopK() {
    this(7);
  }

  @Override
  public String getNome() {
    return "TF-IDF + Top-K Termos";
  }

  @Override
  public String processar(String ementa, CorpusStats corpus) {
    if (ementa == null || ementa.isBlank()) {
      return "";
    }

    List<String> tokens = TokenizadorUtil.tokenizar(ementa, false);
    if (tokens.isEmpty()) {
      return "";
    }

    Map<String, Integer> frequenciaTermos = new HashMap<>();
    for (String token : tokens) {
      frequenciaTermos.put(token, frequenciaTermos.getOrDefault(token, 0) + 1);
    }

    int totalTokens = tokens.size();
    Map<String, Double> scoreTfidf = new HashMap<>();

    for (Map.Entry<String, Integer> entry : frequenciaTermos.entrySet()) {
      String termo = entry.getKey();
      double tf = (double) entry.getValue() / totalTokens;
      double idf = corpus != null ? corpus.getIdf(termo) : 1.0;
      scoreTfidf.put(termo, tf * idf);
    }

    return scoreTfidf.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" "));
  }
}
