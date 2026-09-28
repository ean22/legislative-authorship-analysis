package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EstrategiaTextRank implements EstrategiaReducao {
  private final int topK;
  private final int tamanhoJanela;
  private final double d = 0.85; // Fator de amortecimento padrão
  private final int maxIteracoes = 30;
  private final double convergenciaThreshold = 0.0001;

  public EstrategiaTextRank(int topK, int tamanhoJanela) {
    this.topK = topK;
    this.tamanhoJanela = tamanhoJanela;
  }

  public EstrategiaTextRank() {
    this(7, 3);
  }

  @Override
  public String getNome() {
    return "TextRank (Grafo de Coocorrência)";
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
    if (tokens.size() <= topK) {
      return String.join(" ", tokens);
    }

    // Grafo de Coocorrência não direcionado ponderado
    Map<String, Map<String, Double>> grafo = new HashMap<>();
    Set<String> vocabulario = new HashSet<>(tokens);

    for (String v : vocabulario) {
      grafo.put(v, new HashMap<>());
    }

    for (int i = 0; i < tokens.size(); i++) {
      String t1 = tokens.get(i);
      for (int j = i + 1; j < Math.min(tokens.size(), i + tamanhoJanela + 1); j++) {
        String t2 = tokens.get(j);
        if (!t1.equals(t2)) {
          grafo.get(t1).merge(t2, 1.0, Double::sum);
          grafo.get(t2).merge(t1, 1.0, Double::sum);
        }
      }
    }

    // Calcular soma de pesos de saída de cada nó
    Map<String, Double> somaArestas = new HashMap<>();
    for (String node : vocabulario) {
      double soma = grafo.get(node).values().stream().mapToDouble(Double::doubleValue).sum();
      somaArestas.put(node, soma);
    }

    // Inicialização do score de PageRank
    Map<String, Double> scores = new HashMap<>();
    for (String node : vocabulario) {
      scores.put(node, 1.0);
    }

    // Iterações do TextRank / PageRank
    for (int iter = 0; iter < maxIteracoes; iter++) {
      Map<String, Double> novosScores = new HashMap<>();
      double diferencaMaxima = 0.0;

      for (String vi : vocabulario) {
        double somaVizinhos = 0.0;
        for (Map.Entry<String, Double> vizinhoEntry : grafo.get(vi).entrySet()) {
          String vj = vizinhoEntry.getKey();
          double pesoJparaI = vizinhoEntry.getValue();
          double pesoTotalVj = somaArestas.getOrDefault(vj, 1.0);
          if (pesoTotalVj > 0) {
            somaVizinhos += (pesoJparaI / pesoTotalVj) * scores.get(vj);
          }
        }

        double novoScore = (1.0 - d) + d * somaVizinhos;
        novosScores.put(vi, novoScore);
        diferencaMaxima = Math.max(diferencaMaxima, Math.abs(novoScore - scores.get(vi)));
      }

      scores = novosScores;
      if (diferencaMaxima < convergenciaThreshold) {
        break;
      }
    }

    // Ordenar os nós pelo score final do TextRank
    return scores.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" "));
  }
}
