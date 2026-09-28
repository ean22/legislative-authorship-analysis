package org.entryPoint.service.preprocessamento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstrategiaTfidfSimilaridadeFrases implements EstrategiaReducao {
  private final int maxSentencas;

  public EstrategiaTfidfSimilaridadeFrases(int maxSentencas) {
    this.maxSentencas = maxSentencas;
  }

  public EstrategiaTfidfSimilaridadeFrases() {
    this(1);
  }

  @Override
  public String getNome() {
    return "TF-IDF + Similaridade entre Frases (Centralidade)";
  }

  private static class SentencaInfo {
    final int index;
    final String texto;
    final Map<String, Double> vetor;
    final double norma;
    double centralidade = 0.0;

    SentencaInfo(int index, String texto, Map<String, Double> vetor) {
      this.index = index;
      this.texto = texto;
      this.vetor = vetor;
      double somaQuadrados = vetor.values().stream().mapToDouble(v -> v * v).sum();
      this.norma = Math.sqrt(somaQuadrados);
    }
  }

  private double cosseno(SentencaInfo s1, SentencaInfo s2) {
    if (s1.norma == 0.0 || s2.norma == 0.0) {
      return 0.0;
    }
    double produtoEscalar = 0.0;
    for (Map.Entry<String, Double> e : s1.vetor.entrySet()) {
      Double v2 = s2.vetor.get(e.getKey());
      if (v2 != null) {
        produtoEscalar += e.getValue() * v2;
      }
    }
    return produtoEscalar / (s1.norma * s2.norma);
  }

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

    List<SentencaInfo> lista = new ArrayList<>();
    for (int i = 0; i < sentencas.size(); i++) {
      String s = sentencas.get(i);
      Map<String, Double> vetor = new HashMap<>();
      for (String t : TokenizadorUtil.tokenizar(s, true)) {
        double idf = corpus != null ? corpus.getIdf(t) : 1.0;
        vetor.merge(t, idf, Double::sum);
      }
      lista.add(new SentencaInfo(i, s, vetor));
    }

    // Calcular matriz de similaridade e centralidade
    for (int i = 0; i < lista.size(); i++) {
      for (int j = 0; j < lista.size(); j++) {
        if (i != j) {
          double sim = cosseno(lista.get(i), lista.get(j));
          lista.get(i).centralidade += sim;
        }
      }
    }

    return lista.stream()
        .sorted(Comparator.comparingDouble((SentencaInfo s) -> s.centralidade).reversed())
        .limit(maxSentencas)
        .sorted(Comparator.comparingInt(s -> s.index))
        .map(s -> s.texto)
        .collect(java.util.stream.Collectors.joining(" "));
  }
}
