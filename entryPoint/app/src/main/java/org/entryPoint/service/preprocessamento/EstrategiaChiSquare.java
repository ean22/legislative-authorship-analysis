package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstrategiaChiSquare implements EstrategiaReducao {
  private final int topK;

  public EstrategiaChiSquare(int topK) {
    this.topK = topK;
  }

  public EstrategiaChiSquare() {
    this(7);
  }

  @Override
  public String getNome() {
    return "Chi-Square (Qui-Quadrado)";
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

    Map<String, Double> chiMap = new HashMap<>();
    for (String token : tokens) {
      double scoreChi = corpus != null ? corpus.getChiSquare(token) : 1.0;
      chiMap.put(token, scoreChi);
    }

    return chiMap.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" "));
  }
}
