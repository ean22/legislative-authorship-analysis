package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * Redução Textual via TextRank (Mihalcea & Tarau, 2004)
 * implementada sobre a biblioteca consagrada JGraphT.
 */
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

    // Grafo de Coocorrência não direcionado ponderado utilizando JGraphT
    Graph<String, DefaultWeightedEdge> grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    for (String token : tokens) {
      grafo.addVertex(token);
    }

    for (int i = 0; i < tokens.size(); i++) {
      String t1 = tokens.get(i);
      for (int j = i + 1; j < Math.min(tokens.size(), i + tamanhoJanela + 1); j++) {
        String t2 = tokens.get(j);
        if (!t1.equals(t2)) {
          DefaultWeightedEdge edge = grafo.getEdge(t1, t2);
          if (edge == null) {
            edge = grafo.addEdge(t1, t2);
            if (edge != null) {
              grafo.setEdgeWeight(edge, 1.0);
            }
          } else {
            grafo.setEdgeWeight(edge, grafo.getEdgeWeight(edge) + 1.0);
          }
        }
      }
    }

    // Execução do algoritmo de PageRank consagrado da biblioteca JGraphT
    PageRank<String, DefaultWeightedEdge> pageRank = new PageRank<>(grafo, d, maxIteracoes, convergenciaThreshold);
    Map<String, Double> scores = pageRank.getScores();

    // Ordenar os nós pelo score final do TextRank
    return scores.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(topK)
        .map(Map.Entry::getKey)
        .collect(Collectors.joining(" "));
  }
}
