package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.List;

public class EstrategiaTfidfPosicaoSentenca implements EstrategiaReducao {
  private final int maxSentencas;

  public EstrategiaTfidfPosicaoSentenca(int maxSentencas) {
    this.maxSentencas = maxSentencas;
  }

  public EstrategiaTfidfPosicaoSentenca() {
    this(1);
  }

  @Override
  public String getNome() {
    return "TF-IDF + Posição da Sentença";
  }

  private record SentencaScore(int indiceOriginal, String texto, double score) {}

  @Override
  public String processar(String ementa, CorpusStats corpus) {
    if (ementa == null || ementa.isBlank()) {
      return "";
    }

    List<String> sentencas = TokenizadorUtil.dividirEmSentencas(ementa);
    if (sentencas.isEmpty()) {
      return ementa.trim();
    }
    if (sentencas.size() <= maxSentencas) {
      return String.join(" ", sentencas);
    }

    List<SentencaScore> pontuadas = new java.util.ArrayList<>();

    for (int i = 0; i < sentencas.size(); i++) {
      String sentenca = sentencas.get(i);
      List<String> tokens = TokenizadorUtil.tokenizar(sentenca, true);
      double tfidfTotal = 0.0;
      for (String token : tokens) {
        double idf = corpus != null ? corpus.getIdf(token) : 1.0;
        tfidfTotal += idf;
      }
      double tfidfBase = tokens.isEmpty() ? 0.0 : tfidfTotal / Math.sqrt(tokens.size());

      // Ponderação posicional (Lead Bias: pos 0 tem peso maior 1 + 1/(0+1) = 2.0)
      double pesoPosicao = 1.0 + (1.0 / (i + 1.0));
      double scoreFinal = tfidfBase * pesoPosicao;

      pontuadas.add(new SentencaScore(i, sentenca, scoreFinal));
    }

    return pontuadas.stream()
        .sorted(Comparator.comparingDouble(SentencaScore::score).reversed())
        .limit(maxSentencas)
        .sorted(Comparator.comparingInt(SentencaScore::indiceOriginal))
        .map(SentencaScore::texto)
        .collect(java.util.stream.Collectors.joining(" "));
  }
}
