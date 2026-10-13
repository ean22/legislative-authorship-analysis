package org.entryPoint.benchmark.representacao;

import java.util.List;
import java.util.Map;

/**
 * Representa os dados textuais do corpus estruturados após tokenização,
 * filtragem de vocabulário e vetorização.
 */
public record CorpusProcessado(
    List<Long> docIds,
    List<List<String>> docsTokens,
    List<String> vocabulario,
    Map<String, Integer> vocabIndex,
    Map<String, Integer> documentFrequency,
    List<Map<Integer, Double>> matrizTfidfEsparsa,
    double[][] matrizDensa,
    int totalTokens,
    Map<Long, Integer> coocorrenciaParesDoc // (w1 << 32 | w2) -> freq conjunta de docs
) {

  public int getNumeroDocumentos() {
    return docIds.size();
  }

  public int getTamanhoVocabulario() {
    return vocabulario.size();
  }

  public int getCoocorrencia(int w1, int w2) {
    if (w1 == w2) {
      return documentFrequency.getOrDefault(vocabulario.get(w1), 0);
    }
    int min = Math.min(w1, w2);
    int max = Math.max(w1, w2);
    long chave = (((long) min) << 32) | (max & 0xFFFFFFFFL);
    return coocorrenciaParesDoc.getOrDefault(chave, 0);
  }

  public int getFrequenciaDocumento(int w) {
    return documentFrequency.getOrDefault(vocabulario.get(w), 0);
  }
}
