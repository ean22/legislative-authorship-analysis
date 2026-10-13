package org.entryPoint.benchmark.algoritmos;

import java.util.*;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Implementação de Fatorização de Matrizes Não-Negativas (NMF)
 * com atualizações multiplicativas de Lee & Seung.
 */
public class NmfTopicModeling implements AlgoritmoTopicModeling {

  @Override
  public ResultadoModelo executar(CorpusProcessado corpus, int k, int maxIteracoes, long seed) {
    int dSize = corpus.getNumeroDocumentos();
    int vSize = corpus.getTamanhoVocabulario();
    var matrizX = corpus.matrizTfidfEsparsa();

    if (dSize == 0 || vSize == 0 || k <= 0) {
      return new ResultadoModelo(new double[0][0], new double[0][0], List.of());
    }

    Random rng = new Random(seed);
    double[][] W = new double[dSize][k];
    double[][] H = new double[k][vSize];

    for (int d = 0; d < dSize; d++) {
      for (int t = 0; t < k; t++) {
        W[d][t] = 0.01 + rng.nextDouble() * 0.99;
      }
    }
    for (int t = 0; t < k; t++) {
      for (int v = 0; v < vSize; v++) {
        H[t][v] = 0.01 + rng.nextDouble() * 0.99;
      }
    }

    double eps = 1e-9;
    for (int iter = 1; iter <= maxIteracoes; iter++) {
      // 1. WTW = W^T * W (K x K)
      double[][] WTW = new double[k][k];
      for (int d = 0; d < dSize; d++) {
        for (int k1 = 0; k1 < k; k1++) {
          double wd1 = W[d][k1];
          for (int k2 = 0; k2 < k; k2++) {
            WTW[k1][k2] += wd1 * W[d][k2];
          }
        }
      }

      // 2. WTX = W^T * X (K x V)
      double[][] WTX = new double[k][vSize];
      for (int d = 0; d < dSize; d++) {
        Map<Integer, Double> linha = matrizX.get(d);
        for (Map.Entry<Integer, Double> entry : linha.entrySet()) {
          int wIdx = entry.getKey();
          double val = entry.getValue();
          for (int t = 0; t < k; t++) {
            WTX[t][wIdx] += W[d][t] * val;
          }
        }
      }

      // 3. Atualizar H
      for (int t = 0; t < k; t++) {
        for (int v = 0; v < vSize; v++) {
          double denom = eps;
          for (int k2 = 0; k2 < k; k2++) {
            denom += WTW[t][k2] * H[k2][v];
          }
          H[t][v] *= (WTX[t][v] + eps) / denom;
        }
      }

      // 4. HHT = H * H^T (K x K)
      double[][] HHT = new double[k][k];
      for (int v = 0; v < vSize; v++) {
        for (int k1 = 0; k1 < k; k1++) {
          double h1 = H[k1][v];
          for (int k2 = 0; k2 < k; k2++) {
            HHT[k1][k2] += h1 * H[k2][v];
          }
        }
      }

      // 5. Atualizar W
      for (int d = 0; d < dSize; d++) {
        double[] XHT = new double[k];
        Map<Integer, Double> linha = matrizX.get(d);
        for (Map.Entry<Integer, Double> entry : linha.entrySet()) {
          int wIdx = entry.getKey();
          double val = entry.getValue();
          for (int t = 0; t < k; t++) {
            XHT[t] += val * H[t][wIdx];
          }
        }

        for (int t = 0; t < k; t++) {
          double denom = eps;
          for (int k2 = 0; k2 < k; k2++) {
            denom += W[d][k2] * HHT[k2][t];
          }
          W[d][t] *= (XHT[t] + eps) / denom;
        }
      }
    }

    // Normalizar linhas de W para que representem distribuição de probabilidade
    for (int d = 0; d < dSize; d++) {
      double soma = 0.0;
      for (int t = 0; t < k; t++) {
        soma += W[d][t];
      }
      if (soma > 0) {
        for (int t = 0; t < k; t++) {
          W[d][t] /= soma;
        }
      }
    }

    // Extrair top termos por tópico
    List<TopicoTematico> topicos = new ArrayList<>(k);
    List<String> vocabulario = corpus.vocabulario();

    for (int t = 0; t < k; t++) {
      List<TermoPeso> pesos = new ArrayList<>(vSize);
      for (int v = 0; v < vSize; v++) {
        pesos.add(new TermoPeso(vocabulario.get(v), H[t][v]));
      }
      pesos.sort((a, b) -> Double.compare(b.peso, a.peso));
      List<String> topPalavras = pesos.stream().limit(6).map(TermoPeso::termo).toList();
      String nome = InferidorRotuloTema.inferirRotuloTema(topPalavras);
      topicos.add(new TopicoTematico(t + 1, nome, topPalavras));
    }

    return new ResultadoModelo(W, H, topicos);
  }

  private record TermoPeso(String termo, double peso) {}
}
