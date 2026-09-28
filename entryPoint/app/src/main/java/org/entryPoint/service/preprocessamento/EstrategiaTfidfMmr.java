package org.entryPoint.service.preprocessamento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstrategiaTfidfMmr implements EstrategiaReducao {
  private final int maxSentencas;
  private final double lambda; // trade-off entre relevância e diversidade (ex: 0.65)

  public EstrategiaTfidfMmr(int maxSentencas, double lambda) {
    this.maxSentencas = maxSentencas;
    this.lambda = lambda;
  }

  public EstrategiaTfidfMmr() {
    this(1, 0.65);
  }

  @Override
  public String getNome() {
    return "TF-IDF + MMR (Maximal Marginal Relevance)";
  }

  private static class SentencaVector {
    final int index;
    final String texto;
    final Map<String, Double> vetor;
    final double norma;

    SentencaVector(int index, String texto, Map<String, Double> vetor) {
      this.index = index;
      this.texto = texto;
      this.vetor = vetor;
      double somaQuadrados = vetor.values().stream().mapToDouble(v -> v * v).sum();
      this.norma = Math.sqrt(somaQuadrados);
    }
  }

  private double similaridadeCosseno(SentencaVector v1, SentencaVector v2) {
    if (v1.norma == 0.0 || v2.norma == 0.0) {
      return 0.0;
    }
    double produtoEscalar = 0.0;
    for (Map.Entry<String, Double> e : v1.vetor.entrySet()) {
      Double val2 = v2.vetor.get(e.getKey());
      if (val2 != null) {
        produtoEscalar += e.getValue() * val2;
      }
    }
    return produtoEscalar / (v1.norma * v2.norma);
  }

  private double similaridadeCosseno(SentencaVector v, Map<String, Double> docVetor, double docNorma) {
    if (v.norma == 0.0 || docNorma == 0.0) {
      return 0.0;
    }
    double produtoEscalar = 0.0;
    for (Map.Entry<String, Double> e : v.vetor.entrySet()) {
      Double valDoc = docVetor.get(e.getKey());
      if (valDoc != null) {
        produtoEscalar += e.getValue() * valDoc;
      }
    }
    return produtoEscalar / (v.norma * docNorma);
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

    // Vetor do documento completo
    Map<String, Double> docVetor = new HashMap<>();
    List<String> docTokens = TokenizadorUtil.tokenizar(ementa, true);
    for (String t : docTokens) {
      double idf = corpus != null ? corpus.getIdf(t) : 1.0;
      docVetor.merge(t, idf, Double::sum);
    }
    double docNorma = Math.sqrt(docVetor.values().stream().mapToDouble(v -> v * v).sum());

    // Vetores de cada sentença
    List<SentencaVector> candidatos = new ArrayList<>();
    for (int i = 0; i < sentencas.size(); i++) {
      String s = sentencas.get(i);
      Map<String, Double> sVetor = new HashMap<>();
      for (String t : TokenizadorUtil.tokenizar(s, true)) {
        double idf = corpus != null ? corpus.getIdf(t) : 1.0;
        sVetor.merge(t, idf, Double::sum);
      }
      candidatos.add(new SentencaVector(i, s, sVetor));
    }

    List<SentencaVector> selecionados = new ArrayList<>();

    while (selecionados.size() < maxSentencas && !candidatos.isEmpty()) {
      SentencaVector melhorCandidato = null;
      double melhorScoreMmr = -Double.MAX_VALUE;

      for (SentencaVector cand : candidatos) {
        double simDoc = similaridadeCosseno(cand, docVetor, docNorma);

        double maxSimSelecionados = 0.0;
        for (SentencaVector sel : selecionados) {
          double sim = similaridadeCosseno(cand, sel);
          if (sim > maxSimSelecionados) {
            maxSimSelecionados = sim;
          }
        }

        double scoreMmr = lambda * simDoc - (1.0 - lambda) * maxSimSelecionados;
        if (scoreMmr > melhorScoreMmr) {
          melhorScoreMmr = scoreMmr;
          melhorCandidato = cand;
        }
      }

      if (melhorCandidato != null) {
        selecionados.add(melhorCandidato);
        candidatos.remove(melhorCandidato);
      } else {
        break;
      }
    }

    return selecionados.stream()
        .sorted(Comparator.comparingInt(s -> s.index))
        .map(s -> s.texto)
        .collect(Collectors.joining(" "));
  }
}
