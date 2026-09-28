package org.entryPoint.service.preprocessamento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EstrategiaSvdLsa implements EstrategiaReducao {
  private final int topK;

  public EstrategiaSvdLsa(int topK) {
    this.topK = topK;
  }

  public EstrategiaSvdLsa() {
    this(7);
  }

  @Override
  public String getNome() {
    return "SVD / LSA (Latent Semantic Analysis)";
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

    List<String> todosTokens = TokenizadorUtil.tokenizar(ementa, true);
    if (todosTokens.isEmpty()) {
      return "";
    }

    List<String> vocabularioLocal = todosTokens.stream().distinct().toList();
    int m = vocabularioLocal.size(); // termos
    int n = sentencas.size();        // documentos/sentenças locais

    if (m <= topK) {
      return String.join(" ", vocabularioLocal);
    }

    // Matriz Termo-Documento ponderada por TF-IDF: A (m x n)
    double[][] a = new double[m][n];
    for (int col = 0; col < n; col++) {
      String s = sentencas.get(col);
      List<String> sTokens = TokenizadorUtil.tokenizar(s, true);
      int totalTokensS = Math.max(1, sTokens.size());
      Map<String, Integer> freqS = new HashMap<>();
      for (String tok : sTokens) {
        freqS.put(tok, freqS.getOrDefault(tok, 0) + 1);
      }

      for (int lin = 0; lin < m; lin++) {
        String termo = vocabularioLocal.get(lin);
        int tf = freqS.getOrDefault(termo, 0);
        double idf = corpus != null ? corpus.getIdf(termo) : 1.0;
        a[lin][col] = ((double) tf / totalTokensS) * idf;
      }
    }

    // Construir matriz de covariância de termos C = A * A^T (m x m)
    double[][] c = new double[m][m];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < m; j++) {
        double soma = 0.0;
        for (int k = 0; k < n; k++) {
          soma += a[i][k] * a[j][k];
        }
        c[i][j] = soma;
      }
    }

    // Power Iteration para encontrar o vetor singular esquerdo dominante (primeiro autovetor de C)
    double[] v = new double[m];
    for (int i = 0; i < m; i++) {
      v[i] = 1.0 / Math.sqrt(m);
    }

    for (int iter = 0; iter < 25; iter++) {
      double[] novoV = new double[m];
      for (int i = 0; i < m; i++) {
        double soma = 0.0;
        for (int j = 0; j < m; j++) {
          soma += c[i][j] * v[j];
        }
        novoV[i] = soma;
      }

      double norma = 0.0;
      for (double val : novoV) {
        norma += val * val;
      }
      norma = Math.sqrt(norma);

      if (norma > 1e-12) {
        for (int i = 0; i < m; i++) {
          novoV[i] /= norma;
        }
        v = novoV;
      } else {
        break;
      }
    }

    // Mapear cada termo ao valor absoluto de sua carga no primeiro componente latente
    record TermoLsa(String termo, double peso) {}
    List<TermoLsa> termosRankeados = new ArrayList<>();
    for (int i = 0; i < m; i++) {
      termosRankeados.add(new TermoLsa(vocabularioLocal.get(i), Math.abs(v[i])));
    }

    return termosRankeados.stream()
        .sorted(Comparator.comparingDouble(TermoLsa::peso).reversed()
            .thenComparing(TermoLsa::termo))
        .limit(topK)
        .map(TermoLsa::termo)
        .collect(Collectors.joining(" "));
  }
}
