package org.entryPoint.benchmark.metricas;

import java.util.List;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Calcula a Coerência Temática (UMass Coherence) a partir da coocorrência real
 * de termos nos documentos legislativos do corpus.
 */
public class CalculadorCoerencia {

  public static double calcularCoerenciaUmass(List<TopicoTematico> topicos, CorpusProcessado corpus) {
    if (topicos == null || topicos.isEmpty() || corpus == null) {
      return 0.0;
    }

    var vocabIndex = corpus.vocabIndex();
    double somaTotalCoerencia = 0.0;
    int topicosValidos = 0;

    for (TopicoTematico topico : topicos) {
      List<String> palavras = topico.termosPrincipais();
      if (palavras == null || palavras.size() < 2) {
        continue;
      }

      int m = palavras.size();
      double somaTopico = 0.0;
      int pares = 0;

      for (int i = 1; i < m; i++) {
        String wm = palavras.get(i);
        Integer idxM = vocabIndex.get(wm);
        if (idxM == null) continue;

        for (int j = 0; j < i; j++) {
          String wl = palavras.get(j);
          Integer idxL = vocabIndex.get(wl);
          if (idxL == null) continue;

          int docFreqL = corpus.getFrequenciaDocumento(idxL);
          int coocorr = corpus.getCoocorrencia(idxM, idxL);

          double val = Math.log((coocorr + 1.0) / (docFreqL + 1.0));
          somaTopico += val;
          pares++;
        }
      }

      if (pares > 0) {
        somaTotalCoerencia += (somaTopico / pares);
        topicosValidos++;
      }
    }

    return topicosValidos > 0 ? (somaTotalCoerencia / topicosValidos) : -10.0;
  }

  /**
   * Normaliza o valor de UMass (tipicamente entre -14.0 e -1.0) para a faixa [0.0, 1.0].
   */
  public static double normalizarUmass(double umass) {
    double minUmass = -12.0;
    double maxUmass = -1.5;
    if (umass <= minUmass) return 0.05;
    if (umass >= maxUmass) return 1.0;
    return Math.max(0.05, Math.min(1.0, (umass - minUmass) / (maxUmass - minUmass)));
  }
}
