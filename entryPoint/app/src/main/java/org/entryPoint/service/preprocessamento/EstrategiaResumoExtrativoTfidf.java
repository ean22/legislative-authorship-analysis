package org.entryPoint.service.preprocessamento;

import java.util.Comparator;
import java.util.List;

public class EstrategiaResumoExtrativoTfidf implements EstrategiaReducao {
  private final int maxSentencas;

  public EstrategiaResumoExtrativoTfidf(int maxSentencas) {
    this.maxSentencas = maxSentencas;
  }

  public EstrategiaResumoExtrativoTfidf() {
    this(1);
  }

  @Override
  public String getNome() {
    return "Resumo Extrativo TF-IDF";
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
      double scoreTotal = 0.0;
      for (String token : tokens) {
        double idf = corpus != null ? corpus.getIdf(token) : 1.0;
        scoreTotal += idf;
      }
      double scoreMedio = tokens.isEmpty() ? 0.0 : scoreTotal / Math.sqrt(tokens.size());
      pontuadas.add(new SentencaScore(i, sentenca, scoreMedio));
    }

    return pontuadas.stream()
        .sorted(Comparator.comparingDouble(SentencaScore::score).reversed())
        .limit(maxSentencas)
        .sorted(Comparator.comparingInt(SentencaScore::indiceOriginal))
        .map(SentencaScore::texto)
        .collect(java.util.stream.Collectors.joining(" "));
  }
}
