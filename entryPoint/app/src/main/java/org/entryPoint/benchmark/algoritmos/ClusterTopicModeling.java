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

  private double[][] ultimosCentroides;
  private int[] ultimosClusters;
  private double[][] ultimaMatrizH;
  private double[][] ultimaMatrizDocTopico;
  private CorpusProcessado ultimoCorpus;
  private List<TopicoTematico> ultimosTopicos;
  private double[][] ultimaFrequenciaClusterTermo;
  private int[] ultimaPresencaClusterTermo;
  private int ultimosClustersFiltradosTotal = 0;
  private int ultimosClustersOriginaisK = 0;

  @Override
  public ResultadoModelo executar(CorpusProcessado corpus, int k, int maxIteracoes, long seed) {
    int dSize = corpus.getNumeroDocumentos();
    int vSize = corpus.getTamanhoVocabulario();
    double[][] matrizDensa = corpus.matrizDensa();
    int dim = matrizDensa.length > 0 ? matrizDensa[0].length : 0;

    if (dSize == 0 || vSize == 0 || k <= 0 || dim == 0) {
      this.ultimosClustersFiltradosTotal = 0;
      this.ultimosClustersOriginaisK = k;
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

    // 3. Contagem de documentos e acumulação preliminar de c-TF-IDF por cluster
    int[] contagensDocs = new int[k];
    for (int d = 0; d < dSize; d++) {
      contagensDocs[clusterAtribuido[d]]++;
    }

    var matrizTfidf = corpus.matrizTfidfEsparsa();
    double[][] freqClusterBruta = new double[k][vSize];
    double[] somaTokensBruta = new double[k];

    for (int d = 0; d < dSize; d++) {
      int c = clusterAtribuido[d];
      var mapaDoc = matrizTfidf.get(d);
      if (mapaDoc != null) {
        for (Map.Entry<Integer, Double> entry : mapaDoc.entrySet()) {
          int wIdx = entry.getKey();
          double peso = entry.getValue();
          freqClusterBruta[c][wIdx] += peso;
          somaTokensBruta[c] += peso;
        }
      }
    }

    // 4. Filtro de Clusters Vazios: identificar clusters com documentos e termos válidos
    List<Integer> clustersValidos = new ArrayList<>();
    for (int c = 0; c < k; c++) {
      if (contagensDocs[c] > 0 && somaTokensBruta[c] > 1e-9) {
        clustersValidos.add(c);
      }
    }

    this.ultimosClustersOriginaisK = k;
    if (clustersValidos.isEmpty()) {
      this.ultimosClustersFiltradosTotal = k;
      return new ResultadoModelo(new double[dSize][0], new double[0][vSize], List.of());
    }

    int kValidos = clustersValidos.size();
    int descartados = k - kValidos;
    this.ultimosClustersFiltradosTotal = descartados;

    if (descartados > 0) {
      System.out.printf("[ClusterTopicModeling] Filtro de clusters vazios aplicado: %d cluster(s) vazio(s) descartado(s) de %d solicitado(s). Topic modeling prosseguindo com %d cluster(s) ativo(s).%n",
          descartados, k, kValidos);
    }

    // Mapeamento dos índices antigos para os novos índices sequenciais [0 .. kValidos - 1]
    int[] mapaNovoIndice = new int[k];
    Arrays.fill(mapaNovoIndice, -1);
    for (int novoIdx = 0; novoIdx < kValidos; novoIdx++) {
      mapaNovoIndice[clustersValidos.get(novoIdx)] = novoIdx;
    }

    // Centroides compactados apenas para clusters válidos
    double[][] centroidesValidos = new double[kValidos][dim];
    for (int novoIdx = 0; novoIdx < kValidos; novoIdx++) {
      int cAntigo = clustersValidos.get(novoIdx);
      System.arraycopy(centroides[cAntigo], 0, centroidesValidos[novoIdx], 0, dim);
    }

    // Reatribuição do mapa de documentos para os clusters válidos compactos
    for (int d = 0; d < dSize; d++) {
      int cAntigo = clusterAtribuido[d];
      int cNovo = mapaNovoIndice[cAntigo];
      if (cNovo >= 0) {
        clusterAtribuido[d] = cNovo;
      } else {
        int melhorC = 0;
        double maxSim = -Double.MAX_VALUE;
        for (int c = 0; c < kValidos; c++) {
          double sim = similaridadeCosseno(matrizDensa[d], centroidesValidos[c]);
          if (sim > maxSim) {
            maxSim = sim;
            melhorC = c;
          }
        }
        clusterAtribuido[d] = melhorC;
      }
    }

    // Frequência cluster-termo e soma de tokens compactada para clusters válidos
    double[][] frequenciaClusterTermo = new double[kValidos][vSize];
    double[] somaTokensCluster = new double[kValidos];
    for (int novoIdx = 0; novoIdx < kValidos; novoIdx++) {
      int cAntigo = clustersValidos.get(novoIdx);
      System.arraycopy(freqClusterBruta[cAntigo], 0, frequenciaClusterTermo[novoIdx], 0, vSize);
      somaTokensCluster[novoIdx] = somaTokensBruta[cAntigo];
    }

    // Presença vocabular entre os clusters válidos
    int[] presencaClusterTermo = new int[vSize];
    for (int v = 0; v < vSize; v++) {
      for (int c = 0; c < kValidos; c++) {
        if (frequenciaClusterTermo[c][v] > 1e-6) {
          presencaClusterTermo[v]++;
        }
      }
    }

    // Matriz H (c-TF-IDF: K_valido x V)
    double[][] H = new double[kValidos][vSize];
    for (int c = 0; c < kValidos; c++) {
      double totalC = Math.max(1e-9, somaTokensCluster[c]);
      for (int v = 0; v < vSize; v++) {
        double tf = frequenciaClusterTermo[c][v] / totalC;
        double idf = Math.log(1.0 + ((double) kValidos / (presencaClusterTermo[v] + 1.0)));
        H[c][v] = tf * idf;
      }
    }

    // Matriz W (Doc x Topico: D x K_valido) via Softmax da Similaridade Cosseno
    double[][] W = new double[dSize][kValidos];
    double temperatura = 0.3;

    for (int d = 0; d < dSize; d++) {
      double[] expSim = new double[kValidos];
      double somaExp = 0.0;
      for (int c = 0; c < kValidos; c++) {
        double sim = similaridadeCosseno(matrizDensa[d], centroidesValidos[c]);
        expSim[c] = Math.exp(sim / temperatura);
        somaExp += expSim[c];
      }
      if (somaExp > 0) {
        for (int c = 0; c < kValidos; c++) {
          W[d][c] = expSim[c] / somaExp;
        }
      }
    }

    // 5. Extração de termos e rotulação semântica EXCLUSIVAMENTE para clusters válidos
    List<TopicoTematico> topicos = new ArrayList<>(kValidos);
    List<String> vocabulario = corpus.vocabulario();
    List<List<String>> listaTopPalavras = new ArrayList<>(kValidos);

    for (int c = 0; c < kValidos; c++) {
      List<TermoPeso> pesos = new ArrayList<>(vSize);
      for (int v = 0; v < vSize; v++) {
        pesos.add(new TermoPeso(vocabulario.get(v), H[c][v]));
      }
      pesos.sort((a, b) -> Double.compare(b.peso, a.peso));
      List<String> topPalavras = pesos.stream().limit(6).map(TermoPeso::termo).toList();
      listaTopPalavras.add(topPalavras);
    }

    List<String> nomes = InferidorRotuloTema.inferirRotulosEmLote(listaTopPalavras);
    for (int c = 0; c < kValidos; c++) {
      String nome = (c < nomes.size() && nomes.get(c) != null) ? nomes.get(c) : "Tópico " + (c + 1);
      topicos.add(new TopicoTematico(c + 1, nome, listaTopPalavras.get(c)));
    }

    this.ultimosCentroides = centroidesValidos;
    this.ultimosClusters = clusterAtribuido;
    this.ultimaMatrizH = H;
    this.ultimaMatrizDocTopico = W;
    this.ultimoCorpus = corpus;
    this.ultimosTopicos = topicos;
    this.ultimaFrequenciaClusterTermo = frequenciaClusterTermo;
    this.ultimaPresencaClusterTermo = presencaClusterTermo;

    return new ResultadoModelo(W, H, topicos);
  }

  public double[][] getUltimosCentroides() {
    return ultimosCentroides;
  }

  public int[] getUltimosClusters() {
    return ultimosClusters;
  }

  public double[][] getUltimaMatrizH() {
    return ultimaMatrizH;
  }

  public double[][] getUltimaMatrizDocTopico() {
    return ultimaMatrizDocTopico;
  }

  public CorpusProcessado getUltimoCorpus() {
    return ultimoCorpus;
  }

  public List<TopicoTematico> getUltimosTopicos() {
    return ultimosTopicos;
  }

  public int getUltimosClustersFiltradosTotal() {
    return ultimosClustersFiltradosTotal;
  }

  public int getUltimosClustersOriginaisK() {
    return ultimosClustersOriginaisK;
  }

  public record DetalheTermoCTfIdf(String termo, double scoreCTfIdf, double tfCluster, int presencaClusters) {}

  public List<DetalheTermoCTfIdf> obterDetalhesCTfIdf(int clusterIdx, int limite) {
    if (ultimaMatrizH == null || clusterIdx < 0 || clusterIdx >= ultimaMatrizH.length || ultimoCorpus == null) {
      return List.of();
    }
    List<String> vocab = ultimoCorpus.vocabulario();
    int vSize = vocab.size();
    List<DetalheTermoCTfIdf> lista = new ArrayList<>(vSize);
    for (int v = 0; v < vSize; v++) {
      double score = ultimaMatrizH[clusterIdx][v];
      double tf = ultimaFrequenciaClusterTermo != null ? ultimaFrequenciaClusterTermo[clusterIdx][v] : 0.0;
      int presenca = ultimaPresencaClusterTermo != null ? ultimaPresencaClusterTermo[v] : 0;
      lista.add(new DetalheTermoCTfIdf(vocab.get(v), score, tf, presenca));
    }
    lista.sort((a, b) -> Double.compare(b.scoreCTfIdf(), a.scoreCTfIdf()));
    return lista.stream().limit(limite > 0 ? limite : 10).toList();
  }

  public double similaridadeCosseno(double[] a, double[] b) {
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
