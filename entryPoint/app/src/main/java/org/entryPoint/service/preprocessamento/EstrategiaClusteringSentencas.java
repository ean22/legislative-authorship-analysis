package org.entryPoint.service.preprocessamento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EstrategiaClusteringSentencas implements EstrategiaReducao {
  private final int kClusters;

  public EstrategiaClusteringSentencas(int kClusters) {
    this.kClusters = kClusters;
  }

  public EstrategiaClusteringSentencas() {
    this(2);
  }

  @Override
  public String getNome() {
    return "TF-IDF + Clustering de Sentenças";
  }

  private static class SentencaPonto {
    final int index;
    final String texto;
    final Map<String, Double> vetor;
    final double norma;
    int clusterAtribuido = 0;

    SentencaPonto(int index, String texto, Map<String, Double> vetor) {
      this.index = index;
      this.texto = texto;
      this.vetor = vetor;
      double somaQuadrados = vetor.values().stream().mapToDouble(v -> v * v).sum();
      this.norma = Math.sqrt(somaQuadrados);
    }
  }

  private double distanciaEuclidiana(SentencaPonto p, Map<String, Double> centroide) {
    Set<String> todasChaves = new HashSet<>(p.vetor.keySet());
    todasChaves.addAll(centroide.keySet());

    double somaDiferencas = 0.0;
    for (String key : todasChaves) {
      double v1 = p.vetor.getOrDefault(key, 0.0);
      double v2 = centroide.getOrDefault(key, 0.0);
      double diff = v1 - v2;
      somaDiferencas += diff * diff;
    }
    return Math.sqrt(somaDiferencas);
  }

  @Override
  public String processar(String ementa, CorpusStats corpus) {
    if (ementa == null || ementa.isBlank()) {
      return "";
    }

    List<String> sentencas = TokenizadorUtil.dividirEmSentencas(ementa);
    if (sentencas.isEmpty()) {
      return "";
    }
    if (sentencas.size() <= 1) {
      return sentencas.get(0);
    }

    List<SentencaPonto> pontos = new ArrayList<>();
    for (int i = 0; i < sentencas.size(); i++) {
      String s = sentencas.get(i);
      Map<String, Double> vetor = new HashMap<>();
      for (String t : TokenizadorUtil.tokenizar(s, true)) {
        double idf = corpus != null ? corpus.getIdf(t) : 1.0;
        vetor.merge(t, idf, Double::sum);
      }
      pontos.add(new SentencaPonto(i, s, vetor));
    }

    int kEfetivo = Math.min(kClusters, pontos.size());

    // Inicialização dos centroides com os primeiros K pontos
    List<Map<String, Double>> centroides = new ArrayList<>();
    for (int i = 0; i < kEfetivo; i++) {
      centroides.add(new HashMap<>(pontos.get(i).vetor));
    }

    // Algoritmo K-Means
    int maxIteracoes = 15;
    for (int iter = 0; iter < maxIteracoes; iter++) {
      boolean mudou = false;

      // Atribuição de clusters
      for (SentencaPonto p : pontos) {
        int melhorCluster = 0;
        double menorDist = Double.MAX_VALUE;
        for (int c = 0; c < kEfetivo; c++) {
          double dist = distanciaEuclidiana(p, centroides.get(c));
          if (dist < menorDist) {
            menorDist = dist;
            melhorCluster = c;
          }
        }
        if (p.clusterAtribuido != melhorCluster) {
          p.clusterAtribuido = melhorCluster;
          mudou = true;
        }
      }

      if (!mudou && iter > 0) {
        break;
      }

      // Atualização dos centroides
      for (int c = 0; c < kEfetivo; c++) {
        final int cIdx = c;
        List<SentencaPonto> membros = pontos.stream()
            .filter(p -> p.clusterAtribuido == cIdx)
            .toList();

        if (membros.isEmpty()) {
          continue;
        }

        Map<String, Double> novoCentroide = new HashMap<>();
        for (SentencaPonto m : membros) {
          for (Map.Entry<String, Double> entry : m.vetor.entrySet()) {
            novoCentroide.merge(entry.getKey(), entry.getValue() / membros.size(), Double::sum);
          }
        }
        centroides.set(c, novoCentroide);
      }
    }

    // Selecionar o ponto mais próximo do centroide de cada cluster
    List<SentencaPonto> representantes = new ArrayList<>();
    for (int c = 0; c < kEfetivo; c++) {
      final int cIdx = c;
      List<SentencaPonto> membros = pontos.stream()
          .filter(p -> p.clusterAtribuido == cIdx)
          .toList();

      if (!membros.isEmpty()) {
        Map<String, Double> centroide = centroides.get(c);
        SentencaPonto maisProximo = Collections.min(membros,
            Comparator.comparingDouble(p -> distanciaEuclidiana(p, centroide)));
        representantes.add(maisProximo);
      }
    }

    return representantes.stream()
        .sorted(Comparator.comparingInt(p -> p.index))
        .map(p -> p.texto)
        .collect(Collectors.joining(" "));
  }
}
