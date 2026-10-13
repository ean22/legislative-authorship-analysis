package org.entryPoint.benchmark.algoritmos;

import java.util.*;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Implementação de Alocação Latente de Dirichlet (LDA)
 * via Collapsed Gibbs Sampling determinístico.
 */
public class LdaTopicModeling implements AlgoritmoTopicModeling {

  @Override
  public ResultadoModelo executar(CorpusProcessado corpus, int k, int maxIteracoes, long seed) {
    int dSize = corpus.getNumeroDocumentos();
    int vSize = corpus.getTamanhoVocabulario();
    var docsTokens = corpus.docsTokens();
    var vocabIndex = corpus.vocabIndex();

    if (dSize == 0 || vSize == 0 || k <= 0) {
      return new ResultadoModelo(new double[0][0], new double[0][0], List.of());
    }

    double alpha = 50.0 / k;
    double beta = 0.05;

    // Converter documentos em arrays de índices de vocabulário
    List<int[]> docsIndices = new ArrayList<>(dSize);
    for (int d = 0; d < dSize; d++) {
      List<String> tokens = docsTokens.get(d);
      List<Integer> validos = new ArrayList<>();
      for (String t : tokens) {
        Integer idx = vocabIndex.get(t);
        if (idx != null) {
          validos.add(idx);
        }
      }
      int[] arr = new int[validos.size()];
      for (int i = 0; i < validos.size(); i++) {
        arr[i] = validos.get(i);
      }
      docsIndices.add(arr);
    }

    // Contadores de Gibbs Sampling
    int[][] nDocTopic = new int[dSize][k];
    int[][] nTopicWord = new int[k][vSize];
    int[] nTopicSum = new int[k];

    // Atribuições iniciais z
    List<int[]> z = new ArrayList<>(dSize);
    Random rng = new Random(seed);

    for (int d = 0; d < dSize; d++) {
      int[] docWords = docsIndices.get(d);
      int[] docZ = new int[docWords.length];
      for (int i = 0; i < docWords.length; i++) {
        int topic = rng.nextInt(k);
        docZ[i] = topic;
        int word = docWords[i];
        nDocTopic[d][topic]++;
        nTopicWord[topic][word]++;
        nTopicSum[topic]++;
      }
      z.add(docZ);
    }

    // Amostragem de Gibbs
    double vBeta = vSize * beta;
    double[] pProb = new double[k];

    for (int iter = 1; iter <= maxIteracoes; iter++) {
      for (int d = 0; d < dSize; d++) {
        int[] docWords = docsIndices.get(d);
        int[] docZ = z.get(d);

        for (int i = 0; i < docWords.length; i++) {
          int word = docWords[i];
          int oldTopic = docZ[i];

          // Remover contagem anterior
          nDocTopic[d][oldTopic]--;
          nTopicWord[oldTopic][word]--;
          nTopicSum[oldTopic]--;

          // Calcular probabilidades condicionais
          double somaP = 0.0;
          for (int t = 0; t < k; t++) {
            double pTopicWord = (nTopicWord[t][word] + beta) / (nTopicSum[t] + vBeta);
            double pDocTopic = nDocTopic[d][t] + alpha;
            double p = pTopicWord * pDocTopic;
            pProb[t] = p;
            somaP += p;
          }

          // Amostrar novo tópico
          double u = rng.nextDouble() * somaP;
          double acum = 0.0;
          int newTopic = 0;
          for (int t = 0; t < k; t++) {
            acum += pProb[t];
            if (u <= acum) {
              newTopic = t;
              break;
            }
          }

          // Atualizar novo tópico
          docZ[i] = newTopic;
          nDocTopic[d][newTopic]++;
          nTopicWord[newTopic][word]++;
          nTopicSum[newTopic]++;
        }
      }
    }

    // Estimar matriz documento-tópico W (theta)
    double[][] W = new double[dSize][k];
    double kAlpha = k * alpha;
    for (int d = 0; d < dSize; d++) {
      int totalTokensDoc = docsIndices.get(d).length;
      for (int t = 0; t < k; t++) {
        W[d][t] = (nDocTopic[d][t] + alpha) / (totalTokensDoc + kAlpha);
      }
    }

    // Estimar matriz tópico-termo H (phi)
    double[][] H = new double[k][vSize];
    for (int t = 0; t < k; t++) {
      for (int v = 0; v < vSize; v++) {
        H[t][v] = (nTopicWord[t][v] + beta) / (nTopicSum[t] + vBeta);
      }
    }

    // Extrair tópicos temáticos
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
