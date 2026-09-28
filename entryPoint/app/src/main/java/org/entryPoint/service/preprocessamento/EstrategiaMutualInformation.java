package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstrategiaMutualInformation implements EstrategiaReducao {
  private final int topK;

  public EstrategiaMutualInformation(int topK) {
    this.topK = topK;
  }

  public EstrategiaMutualInformation() {
    this(7);
  }

  @Override
  public String getNome() {
    return "Mutual Information (Informação Mútua)";
  }

  @Override
  public String processar(String ementa, CorpusStats corpus) {
    if (ementa == null || ementa.isBlank()) {
      return "";
    }

    List<String> tokens = TokenizadorUtil.tokenizar(ementa, true);
    if (tokens.isEmpty()) {
      return "";
    }

    Map<String, Double> miMap = new HashMap<>();
    for (String token : tokens) {
      double scoreMi = corpus != null ? corpus.getMutualInformation(token) : 1.0;
      miMap.put(token, scoreMi);
    }

    return miMap.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" "));
  }
}
