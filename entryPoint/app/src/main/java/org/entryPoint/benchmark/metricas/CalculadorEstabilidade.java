package org.entryPoint.benchmark.metricas;

import java.util.*;
import org.entryPoint.model.TopicoTematico;

/**
 * Avalia a estabilidade e reprodutibilidade do modelo entre inicializações com sementes diferentes.
 * Calcula a similaridade de Jaccard média dos termos-chave entre as execuções alinhadas.
 */
public class CalculadorEstabilidade {

  public static double calcularEstabilidadeJaccard(
      List<TopicoTematico> topicosA,
      List<TopicoTematico> topicosB
  ) {
    if (topicosA == null || topicosB == null || topicosA.isEmpty() || topicosB.isEmpty()) {
      return 0.0;
    }

    int kA = topicosA.size();
    int kB = topicosB.size();

    // Matriz de Jaccard entre pares de tópicos
    double[][] matrizJaccard = new double[kA][kB];
    for (int i = 0; i < kA; i++) {
      Set<String> setA = new HashSet<>(topicosA.get(i).termosPrincipais());
      for (int j = 0; j < kB; j++) {
        Set<String> setB = new HashSet<>(topicosB.get(j).termosPrincipais());
        matrizJaccard[i][j] = calcularJaccard(setA, setB);
      }
    }

    // Alinhamento guloso (Greedy Matching)
    boolean[] usadoB = new boolean[kB];
    double somaJaccard = 0.0;
    int emparelhados = 0;

    for (int i = 0; i < kA; i++) {
      double maxJ = -1.0;
      int melhorJ = -1;
      for (int j = 0; j < kB; j++) {
        if (!usadoB[j] && matrizJaccard[i][j] > maxJ) {
          maxJ = matrizJaccard[i][j];
          melhorJ = j;
        }
      }
      if (melhorJ != -1) {
        usadoB[melhorJ] = true;
        somaJaccard += maxJ;
        emparelhados++;
      }
    }

    return emparelhados > 0 ? (somaJaccard / emparelhados) : 0.0;
  }

  private static double calcularJaccard(Set<String> a, Set<String> b) {
    if (a.isEmpty() && b.isEmpty()) return 1.0;
    if (a.isEmpty() || b.isEmpty()) return 0.0;

    Set<String> intersecao = new HashSet<>(a);
    intersecao.retainAll(b);

    Set<String> uniao = new HashSet<>(a);
    uniao.addAll(b);

    return (double) intersecao.size() / uniao.size();
  }
}
