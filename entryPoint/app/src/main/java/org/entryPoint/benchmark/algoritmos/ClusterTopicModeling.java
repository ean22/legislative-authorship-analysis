package org.entryPoint.benchmark.algoritmos;

import java.util.*;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Topic Modeling baseado em Clustering Vetorial + Class-based TF-IDF (c-TF-IDF).
 * Agrupa documentos no espaço geométrico via KMeansPlusPlusClusterer (Apache Commons Math)
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
    var matrizTfidf = corpus.matrizTfidfEsparsa();

    // Determina se opera no espaço esparso de termos (pipeline simplificado direto)
    // ou no espaço vetorial contínuo denso (legado / DENSE_SVD)
    boolean usarEsparsa = (matrizDensa == null || matrizDensa.length == 0 || matrizDensa[0].length == 0)
        && (matrizTfidf != null && !matrizTfidf.isEmpty());

    int dim = usarEsparsa ? vSize : (matrizDensa != null && matrizDensa.length > 0 ? matrizDensa[0].length : 0);

    if (dSize == 0 || vSize == 0 || k <= 0 || dim == 0) {
      this.ultimosClustersFiltradosTotal = 0;
      this.ultimosClustersOriginaisK = k;
      return new ResultadoModelo(new double[0][0], new double[0][0], List.of());
    }

    // 1. Definição do espaço de atributos para o KMeans (otimizado para até 300 dimensões discriminantes)
    final int maxDimKmeans = 300;
    int[] indicesKmeans;
    int dimKmeans;

    if (usarEsparsa && dim > maxDimKmeans) {
      dimKmeans = maxDimKmeans;
      var dfMap = corpus.documentFrequency();
      var vocab = corpus.vocabulario();
      List<Integer> sortedIndices = new ArrayList<>(dim);
      for (int i = 0; i < dim; i++) sortedIndices.add(i);
      sortedIndices.sort((i1, i2) -> {
        int df1 = (dfMap != null && i1 < vocab.size()) ? dfMap.getOrDefault(vocab.get(i1), 0) : 0;
        int df2 = (dfMap != null && i2 < vocab.size()) ? dfMap.getOrDefault(vocab.get(i2), 0) : 0;
        return Integer.compare(df2, df1);
      });
      indicesKmeans = new int[dimKmeans];
      for (int i = 0; i < dimKmeans; i++) {
        indicesKmeans[i] = sortedIndices.get(i);
      }
    } else {
      dimKmeans = dim;
      indicesKmeans = null;
    }

    List<DocumentPoint> pontos = new ArrayList<>(dSize);
    for (int d = 0; d < dSize; d++) {
      double[] vec = new double[dimKmeans];
      if (usarEsparsa) {
        Map<Integer, Double> linha = matrizTfidf.get(d);
        if (linha != null) {
          if (indicesKmeans != null) {
            for (int kIdx = 0; kIdx < dimKmeans; kIdx++) {
              Double val = linha.get(indicesKmeans[kIdx]);
              if (val != null) {
                vec[kIdx] = val;
              }
            }
          } else {
            for (Map.Entry<Integer, Double> entry : linha.entrySet()) {
              int idx = entry.getKey();
              if (idx < dimKmeans) {
                vec[idx] = entry.getValue();
              }
            }
          }
        }
      } else {
        System.arraycopy(matrizDensa[d], 0, vec, 0, dimKmeans);
      }
      double somaQuad = 0.0;
      for (double val : vec) {
        somaQuad += val * val;
      }
      double norma = Math.sqrt(somaQuad);
      if (norma > 1e-9) {
        for (int j = 0; j < dimKmeans; j++) {
          vec[j] /= norma;
        }
      }
      pontos.add(new DocumentPoint(d, vec));
    }

    // 2. Execução do algoritmo consagrado KMeansPlusPlusClusterer (Apache Commons Math)
    RandomGenerator rng = new MersenneTwister(seed);
    int iteracoesKmeans = Math.min(maxIteracoes, 30);
    int kKmeans = Math.min(k, dSize);

    KMeansPlusPlusClusterer<DocumentPoint> clusterer = new KMeansPlusPlusClusterer<>(
        kKmeans,
        iteracoesKmeans,
        new DistanciaCosseno(),
        rng,
        KMeansPlusPlusClusterer.EmptyClusterStrategy.LARGEST_VARIANCE
    );

    List<CentroidCluster<DocumentPoint>> resultadoClusters = clusterer.cluster(pontos);
    int totalClustersGerados = resultadoClusters.size();

    int[] clusterAtribuido = new int[dSize];
    for (int c = 0; c < totalClustersGerados; c++) {
      CentroidCluster<DocumentPoint> cluster = resultadoClusters.get(c);
      for (DocumentPoint ponto : cluster.getPoints()) {
        clusterAtribuido[ponto.getDocId()] = c;
      }
    }

    // Cálculo dos centroides na dimensão completa (dim) para representação precisa de tópicos
    double[][] centroides = new double[k][dim];
    int[] contagensCluster = new int[k];
    for (int d = 0; d < dSize; d++) {
      int c = clusterAtribuido[d];
      contagensCluster[c]++;
      if (usarEsparsa) {
        Map<Integer, Double> linha = matrizTfidf.get(d);
        if (linha != null) {
          for (Map.Entry<Integer, Double> entry : linha.entrySet()) {
            int idx = entry.getKey();
            if (idx < dim) {
              centroides[c][idx] += entry.getValue();
            }
          }
        }
      } else {
        for (int j = 0; j < dim; j++) {
          centroides[c][j] += matrizDensa[d][j];
        }
      }
    }

    for (int c = 0; c < totalClustersGerados; c++) {
      if (contagensCluster[c] > 0) {
        double somaQuad = 0.0;
        for (int j = 0; j < dim; j++) {
          centroides[c][j] /= contagensCluster[c];
          somaQuad += centroides[c][j] * centroides[c][j];
        }
        double norma = Math.sqrt(somaQuad);
        if (norma > 1e-9) {
          for (int j = 0; j < dim; j++) {
            centroides[c][j] /= norma;
          }
        }
      }
    }

    // 3. Contagem de documentos e acumulação preliminar de c-TF-IDF por cluster
    int[] contagensDocs = new int[k];
    for (int d = 0; d < dSize; d++) {
      contagensDocs[clusterAtribuido[d]]++;
    }

    double[][] freqClusterBruta = new double[k][vSize];
    double[] somaTokensBruta = new double[k];

    for (int d = 0; d < dSize; d++) {
      int c = clusterAtribuido[d];
      var mapaDoc = matrizTfidf != null && d < matrizTfidf.size() ? matrizTfidf.get(d) : null;
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
          double sim = usarEsparsa
              ? calcularDot(matrizTfidf.get(d), centroidesValidos[c])
              : similaridadeCosseno(matrizDensa[d], centroidesValidos[c]);
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
        double sim = usarEsparsa
            ? calcularDot(matrizTfidf.get(d), centroidesValidos[c])
            : similaridadeCosseno(matrizDensa[d], centroidesValidos[c]);
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

  private void copiarDocParaCentroide(Map<Integer, Double> doc, double[] centroide) {
    Arrays.fill(centroide, 0.0);
    if (doc == null || doc.isEmpty()) return;
    for (Map.Entry<Integer, Double> entry : doc.entrySet()) {
      int idx = entry.getKey();
      if (idx < centroide.length) {
        centroide[idx] = entry.getValue();
      }
    }
    double somaQuad = 0.0;
    for (double val : centroide) {
      somaQuad += val * val;
    }
    double norma = Math.sqrt(somaQuad);
    if (norma > 1e-9) {
      for (int i = 0; i < centroide.length; i++) {
        centroide[i] /= norma;
      }
    }
  }

  private double calcularDot(Map<Integer, Double> docSparse, double[] centroide) {
    if (docSparse == null || docSparse.isEmpty() || centroide == null) {
      return 0.0;
    }
    double dot = 0.0;
    for (Map.Entry<Integer, Double> entry : docSparse.entrySet()) {
      int idx = entry.getKey();
      if (idx < centroide.length) {
        dot += entry.getValue() * centroide[idx];
      }
    }
    return dot;
  }

  public double calcularSimilaridadeDocCentroide(CorpusProcessado corpus, int docIdx, int clusterIdx) {
    if (ultimosCentroides == null || clusterIdx < 0 || clusterIdx >= ultimosCentroides.length || corpus == null) {
      return 0.0;
    }
    double[] centroide = ultimosCentroides[clusterIdx];
    var matrizTfidf = corpus.matrizTfidfEsparsa();
    if (matrizTfidf != null && docIdx < matrizTfidf.size() && !matrizTfidf.get(docIdx).isEmpty()) {
      return calcularDot(matrizTfidf.get(docIdx), centroide);
    }
    double[][] matrizDensa = corpus.matrizDensa();
    if (matrizDensa != null && docIdx < matrizDensa.length && matrizDensa[docIdx].length == centroide.length) {
      return similaridadeCosseno(matrizDensa[docIdx], centroide);
    }
    return 0.0;
  }

  private record TermoPeso(String termo, double peso) {}

  public static class DocumentPoint implements Clusterable {
    private final int docId;
    private final double[] point;

    public DocumentPoint(int docId, double[] point) {
      this.docId = docId;
      this.point = point;
    }

    public int getDocId() {
      return docId;
    }

    @Override
    public double[] getPoint() {
      return point;
    }
  }

  public static class DistanciaCosseno implements DistanceMeasure {
    private static final long serialVersionUID = 1L;

    @Override
    public double compute(double[] a, double[] b) {
      double dot = 0.0;
      double nA = 0.0;
      double nB = 0.0;
      for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        nA += a[i] * a[i];
        nB += b[i] * b[i];
      }
      double denom = Math.sqrt(nA) * Math.sqrt(nB);
      if (denom <= 1e-9) return 1.0;
      double sim = dot / denom;
      return Math.max(0.0, 1.0 - sim);
    }
  }
}
