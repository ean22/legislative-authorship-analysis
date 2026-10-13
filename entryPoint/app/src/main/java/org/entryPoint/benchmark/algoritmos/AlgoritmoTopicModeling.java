package org.entryPoint.benchmark.algoritmos;

import java.util.List;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Interface unificada para algoritmos de modelagem de tópicos no benchmark.
 */
public interface AlgoritmoTopicModeling {

  record ResultadoModelo(
      double[][] matrizDocTopico,    // W (D x K)
      double[][] matrizTopicoTermo,  // H (K x V)
      List<TopicoTematico> topicos
  ) {}

  ResultadoModelo executar(CorpusProcessado corpus, int k, int maxIteracoes, long seed);
}
