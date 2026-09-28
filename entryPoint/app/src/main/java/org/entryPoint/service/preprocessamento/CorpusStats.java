package org.entryPoint.service.preprocessamento;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorpusStats {
  private long totalDocumentos;
  private final Map<String, Integer> documentFrequency = new HashMap<>();
  private final Map<String, Double> idfMap = new HashMap<>();

  // Estatísticas para Chi-Square e Mutual Information agrupadas por tipo/categoria de norma
  private final Map<String, Integer> documentosPorClasse = new HashMap<>();
  private final Map<String, Map<String, Integer>> termoClasseCount = new HashMap<>(); // termo -> (classe -> freqDoc)
  private final Map<String, Double> chiSquareMaxMap = new HashMap<>();
  private final Map<String, Double> mutualInformationMaxMap = new HashMap<>();

  public record DocumentoInput(long id, String texto, String classe) {}

  public void treinar(List<DocumentoInput> documentos) {
    this.totalDocumentos = documentos.size();
    this.documentFrequency.clear();
    this.idfMap.clear();
    this.documentosPorClasse.clear();
    this.termoClasseCount.clear();
    this.chiSquareMaxMap.clear();
    this.mutualInformationMaxMap.clear();

    if (totalDocumentos == 0) {
      return;
    }

    for (DocumentoInput doc : documentos) {
      String classe = (doc.classe() == null || doc.classe().isBlank()) ? "GERAL" : doc.classe();
      documentosPorClasse.put(classe, documentosPorClasse.getOrDefault(classe, 0) + 1);

      List<String> tokens = TokenizadorUtil.tokenizar(doc.texto(), true);
      Set<String> termosUnicos = new HashSet<>(tokens);

      for (String termo : termosUnicos) {
        documentFrequency.put(termo, documentFrequency.getOrDefault(termo, 0) + 1);

        termoClasseCount
            .computeIfAbsent(termo, k -> new HashMap<>())
            .merge(classe, 1, Integer::sum);
      }
    }

    // Calcular IDF
    double n = (double) totalDocumentos;
    for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
      String termo = entry.getKey();
      int df = entry.getValue();
      double idf = Math.log((n + 1.0) / (df + 1.0)) + 1.0;
      idfMap.put(termo, idf);
    }

    // Calcular Chi-Square e Mutual Information para cada termo
    calcularMetricasEstatisticasClasses();
  }

  private void calcularMetricasEstatisticasClasses() {
    double nTotal = (double) totalDocumentos;

    for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
      String termo = entry.getKey();
      int dfTermo = entry.getValue();
      Map<String, Integer> distClasse = termoClasseCount.getOrDefault(termo, Map.of());

      double maxChiSquare = 0.0;
      double maxMI = 0.0;

      for (Map.Entry<String, Integer> classeEntry : documentosPorClasse.entrySet()) {
        String classe = classeEntry.getKey();
        int totalClasse = classeEntry.getValue();

        // Tabela de Contingência 2x2:
        // A: doc contém termo E pertence à classe
        // B: doc contém termo E NÃO pertence à classe
        // C: doc NÃO contém termo E pertence à classe
        // D: doc NÃO contém termo E NÃO pertence à classe
        double a = distClasse.getOrDefault(classe, 0);
        double b = dfTermo - a;
        double c = totalClasse - a;
        double d = nTotal - a - b - c;

        // Chi-Square
        double denomChi = (a + b) * (a + c) * (b + d) * (c + d);
        if (denomChi > 0.0) {
          double numChi = nTotal * Math.pow((a * d - b * c), 2);
          double chiSquare = numChi / denomChi;
          if (chiSquare > maxChiSquare) {
            maxChiSquare = chiSquare;
          }
        }

        // Mutual Information
        // I(T; C) = sum p(t, c) * log2( p(t,c) / (p(t)*p(c)) )
        if (a > 0) {
          double pTC = a / nTotal;
          double pT = (a + b) / nTotal;
          double pC = (a + c) / nTotal;
          if (pTC > 0 && pT > 0 && pC > 0) {
            double mi = pTC * (Math.log(pTC / (pT * pC)) / Math.log(2.0));
            if (mi > maxMI) {
              maxMI = mi;
            }
          }
        }
      }

      chiSquareMaxMap.put(termo, maxChiSquare);
      mutualInformationMaxMap.put(termo, maxMI);
    }
  }

  public double getIdf(String termo) {
    return idfMap.getOrDefault(termo, Math.log((totalDocumentos + 1.0) / 1.0) + 1.0);
  }

  public double getChiSquare(String termo) {
    return chiSquareMaxMap.getOrDefault(termo, 0.0);
  }

  public double getMutualInformation(String termo) {
    return mutualInformationMaxMap.getOrDefault(termo, 0.0);
  }

  public int getDocumentFrequency(String termo) {
    return documentFrequency.getOrDefault(termo, 0);
  }

  public long getTotalDocumentos() {
    return totalDocumentos;
  }
}
