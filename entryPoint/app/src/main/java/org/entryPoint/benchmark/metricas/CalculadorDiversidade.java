package org.entryPoint.benchmark.metricas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.entryPoint.model.TopicoTematico;

/**
 * Calcula a Diversidade de Tópicos (Topic Diversity).
 * Avalia se os tópicos possuem vocabulários específicos e distintos entre si,
 * penalizando o colapso de tópicos onde os mesmos termos se repetem em múltiplos tópicos.
 */
public class CalculadorDiversidade {

  public static double calcularDiversidade(List<TopicoTematico> topicos, int topN) {
    if (topicos == null || topicos.isEmpty() || topN <= 0) {
      return 0.0;
    }

    Set<String> palavrasUnicas = new HashSet<>();
    int totalPalavrasConsideradas = 0;

    for (TopicoTematico topico : topicos) {
      List<String> termos = topico.termosPrincipais();
      if (termos == null) continue;

      int limite = Math.min(topN, termos.size());
      for (int i = 0; i < limite; i++) {
        palavrasUnicas.add(termos.get(i).toLowerCase());
        totalPalavrasConsideradas++;
      }
    }

    if (totalPalavrasConsideradas == 0) {
      return 0.0;
    }

    return (double) palavrasUnicas.size() / totalPalavrasConsideradas;
  }
}
