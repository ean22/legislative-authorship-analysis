package org.entryPoint.service.preprocessamento;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EstrategiaTfidfFeatureSelection implements EstrategiaReducao {
  private final int minDocFrequency;
  private final int maxTermos;

  public EstrategiaTfidfFeatureSelection(int minDocFrequency, int maxTermos) {
    this.minDocFrequency = minDocFrequency;
    this.maxTermos = maxTermos;
  }

  public EstrategiaTfidfFeatureSelection() {
    this(2, 8);
  }

  @Override
  public String getNome() {
    return "TF-IDF como Feature Selection";
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

    List<String> featuresFiltradas = new ArrayList<>();
    for (String token : tokens) {
      if (corpus != null) {
        int df = corpus.getDocumentFrequency(token);
        // Filtrar termos com frequência extremamente baixa (hapax legomena) e preservar features informativas
        if (df >= minDocFrequency) {
          featuresFiltradas.add(token);
        }
      } else {
        featuresFiltradas.add(token);
      }
    }

    if (featuresFiltradas.isEmpty()) {
      featuresFiltradas = tokens;
    }

    // Deduplicar mantendo ordem de aparição e limitar a maxTermos
    return featuresFiltradas.stream()
        .distinct()
        .limit(maxTermos)
        .collect(Collectors.joining(" "));
  }
}
