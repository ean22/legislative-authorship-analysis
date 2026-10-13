package org.entryPoint.benchmark.algoritmos;

import java.util.*;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Topic Modeling baseado em Clustering Vetorial + Class-based TF-IDF (c-TF-IDF).
 * Agrupa documentos no espaço geométrico via K-Means com distância cosseno
 * e extrai a representação semântica de cada tópico através de c-TF-IDF.
 */
public class ClusterTopicModeling implements AlgoritmoTopicModeling {

  @Override
  public ResultadoModelo executar(CorpusProcessado corpus, int k, int maxIteracoes, long seed) {
    int dSize = corpus.getNumeroDocumentos();
    int vSize = corpus.getTamanhoVocabulario();
    double[][] matrizDensa = corpus.matrizDensa();
    int dim = matrizDensa.length > 0 ? matrizDensa[0].length : 0;

    if (dSize == 0 || vSize == 0 || k <= 0 || dim == 0) {
      return new ResultadoModelo(new double[0][0], new double[0][0], List.of());
    }

    Random rng = new Random(seed);

    // 1. Inicialização dos Centroides (K-Means++)
    double[][] centroides = new double[k][dim];
    int primeiroIdx = rng.nextInt(dSize);
    System.arraycopy(matrizDensa[primeiroIdx], 0, centroides[0], 0, dim);

    for (int t = 1; t < k; t++) {
      double[] distancias = new double[dSize];
      double somaDist = 0.0;
      for (int d = 0; d < dSize; d++) {
        double minDist = Double.MAX_VALUE;
        for (int c = 0; c < t; c++) {
          double dist = 1.0 - similaridadeCosseno(matrizDensa[d], centroides[c]);
          if (dist < minDist) {
            minDist = dist;
          }
        }
        distancias[d] = minDist * minDist;
        somaDist += distancias[d];
      }

      double r = rng.nextDouble() * somaDist;
      double acum = 0.0;
      int escolhido = 0;
      for (int d = 0; d < dSize; d++) {
        acum += distancias[d];
        if (r <= acum) {
          escolhido = d;
          break;
        }
      }
      System.arraycopy(matrizDensa[escolhido], 0, centroides[t], 0, dim);
    }

    // 2. Iterações K-Means com Similaridade Cosseno
    int[] clusterAtribuido = new int[dSize];
    int iteracoesKmeans = Math.min(maxIteracoes, 20);

    for (int iter = 0; iter < iteracoesKmeans; iter++) {
      boolean mudou = false;

      // Atribuição de clusters
      for (int d = 0; d < dSize; d++) {
        int melhorC = 0;
        double maxSim = -Double.MAX_VALUE;
        for (int c = 0; c < k; c++) {
          double sim = similaridadeCosseno(matrizDensa[d], centroides[c]);
          if (sim > maxSim) {
            maxSim = sim;
            melhorC = c;
          }
        }
        if (clusterAtribuido[d] != melhorC) {
          clusterAtribuido[d] = melhorC;
          mudou = true;
        }
      }

      if (!mudou && iter > 2) {
        break;
      }

      // Recálculo dos centroides
      double[][] novosCentroides = new double[k][dim];
      int[] contagens = new int[k];

      for (int d = 0; d < dSize; d++) {
        int c = clusterAtribuido[d];
        contagens[c]++;
        for (int j = 0; j < dim; j++) {
          novosCentroides[c][j] += matrizDensa[d][j];
        }
      }

      for (int c = 0; c < k; c++) {
        if (contagens[c] > 0) {
          double somaQuad = 0.0;
          for (int j = 0; j < dim; j++) {
            novosCentroides[c][j] /= contagens[c];
            somaQuad += novosCentroides[c][j] * novosCentroides[c][j];
          }
          double norma = Math.sqrt(somaQuad);
          if (norma > 1e-9) {
            for (int j = 0; j < dim; j++) {
              centroides[c][j] = novosCentroides[c][j] / norma;
            }
          }
        }
      }
    }

    // 3. Class-based TF-IDF (c-TF-IDF) para extrair termos por cluster
    var matrizTfidf = corpus.matrizTfidfEsparsa();
    double[][] frequenciaClusterTermo = new double[k][vSize];
    double[] somaTokensCluster = new double[k];
    int[] presencaClusterTermo = new int[vSize];

    for (int d = 0; d < dSize; d++) {
      int c = clusterAtribuido[d];
      for (Map.Entry<Integer, Double> entry : matrizTfidf.get(d).entrySet()) {
        int wIdx = entry.getKey();
        double peso = entry.getValue();
        frequenciaClusterTermo[c][wIdx] += peso;
        somaTokensCluster[c] += peso;
      }
    }

    for (int v = 0; v < vSize; v++) {
      for (int c = 0; c < k; c++) {
        if (frequenciaClusterTermo[c][v] > 1e-6) {
          presencaClusterTermo[v]++;
        }
      }
    }

    // Matriz H (c-TF-IDF: K x V)
    double[][] H = new double[k][vSize];
    for (int c = 0; c < k; c++) {
      double totalC = Math.max(1e-9, somaTokensCluster[c]);
      for (int v = 0; v < vSize; v++) {
        double tf = frequenciaClusterTermo[c][v] / totalC;
        double idf = Math.log(1.0 + ((double) k / (presencaClusterTermo[v] + 1.0)));
        H[c][v] = tf * idf;
      }
    }

    // Matriz W (Doc x Topico) via Softmax da Similaridade Cosseno
    double[][] W = new double[dSize][k];
    double temperatura = 0.3;

    for (int d = 0; d < dSize; d++) {
      double[] expSim = new double[k];
      double somaExp = 0.0;
      for (int c = 0; c < k; c++) {
        double sim = similaridadeCosseno(matrizDensa[d], centroides[c]);
        expSim[c] = Math.exp(sim / temperatura);
        somaExp += expSim[c];
      }
      if (somaExp > 0) {
        for (int c = 0; c < k; c++) {
          W[d][c] = expSim[c] / somaExp;
        }
      }
    }

    // Extrair termos de cada tópico
    List<TopicoTematico> topicos = new ArrayList<>(k);
    List<String> vocabulario = corpus.vocabulario();

    for (int c = 0; c < k; c++) {
      List<TermoPeso> pesos = new ArrayList<>(vSize);
      for (int v = 0; v < vSize; v++) {
        pesos.add(new TermoPeso(vocabulario.get(v), H[c][v]));
      }
      pesos.sort((a, b) -> Double.compare(b.peso, a.peso));
      List<String> topPalavras = pesos.stream().limit(6).map(TermoPeso::termo).toList();
      String nome = InferidorRotuloTema.inferirRotuloTema(topPalavras);
      topicos.add(new TopicoTematico(c + 1, nome, topPalavras));
    }

    return new ResultadoModelo(W, H, topicos);
  }

  private double similaridadeCosseno(double[] a, double[] b) {
    double dot = 0.0;
    double nA = 0.0;
    double nB = 0.0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      nA += a[i] * a[i];
      nB += b[i] * b[i];
    }
    double denom = Math.sqrt(nA) * Math.sqrt(nB);
    return denom > 1e-9 ? dot / denom : 0.0;
  }

  private record TermoPeso(String termo, double peso) {}
}
