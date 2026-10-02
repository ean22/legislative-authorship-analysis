package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.entryPoint.model.ClusterInfo;
import org.entryPoint.model.EmbeddingNorma;
import org.entryPoint.model.NormaRepresentativa;
import org.entryPoint.model.ResultadoClusterizacao;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;
import org.entryPoint.service.preprocessamento.TokenizadorUtil;

/**
 * Serviço responsável por clusterizar embeddings de ementas utilizando K-Means++
 * e identificar as normas e ementas mais representativas (medoids) de cada cluster.
 */
public class ServicoClusterizacaoEmbeddings {

  private static final int DEFAULT_MAX_ITERACOES = 100;
  private static final double TOLERANCIA_CONVERGENCIA = 1e-5;

  private final RepositorioEmentasPreProcessadas repositorioEmentas;

  public ServicoClusterizacaoEmbeddings() {
    this.repositorioEmentas = new RepositorioEmentasPreProcessadas();
  }

  public ServicoClusterizacaoEmbeddings(RepositorioEmentasPreProcessadas repositorioEmentas) {
    this.repositorioEmentas = repositorioEmentas;
  }

  public ServicoClusterizacaoEmbeddings(RepositorioBancoDados repositorio) {
    this.repositorioEmentas = repositorio.getRepositorioEmentas();
  }

  /**
   * Executa a clusterização K-Means dos embeddings carregados do banco de dados.
   *
   * @param k Quantidade de clusters desejada
   * @return Resultado detalhado da clusterização contendo os medoids de cada cluster
   */
  public ResultadoClusterizacao clusterizar(int k) throws SQLException {
    return clusterizar(k, DEFAULT_MAX_ITERACOES, 42L);
  }

  /**
   * Executa a clusterização K-Means dos embeddings com parâmetros customizados.
   *
   * @param k             Quantidade de clusters
   * @param maxIteracoes  Número máximo de iterações do K-Means
   * @param seedAleatoria Semente para reprodutibilidade da inicialização K-Means++
   * @return Resultado detalhado da clusterização
   */
  public ResultadoClusterizacao clusterizar(int k, int maxIteracoes, long seedAleatoria) throws SQLException {
    long inicioTempo = System.currentTimeMillis();

    List<EmbeddingNorma> dados = repositorioEmentas.listarEmbeddingsParaClusterizacao();
    if (dados.isEmpty()) {
      System.err.println("Nenhum embedding encontrado no banco de dados para clusterização.");
      return new ResultadoClusterizacao(k, List.of(), 0.0, 0L, 0);
    }

    if (k <= 0) {
      throw new IllegalArgumentException("A quantidade de clusters (k) deve ser maior que zero.");
    }
    if (k > dados.size()) {
      k = dados.size();
    }

    int dim = dados.get(0).vetor().length;
    int n = dados.size();

    System.out.println("\n" + "=".repeat(85));
    System.out.printf("=== INICIANDO CLUSTERIZAÇÃO K-MEANS DOS EMBEDDINGS (K = %d) ===%n", k);
    System.out.println("=".repeat(85));
    System.out.printf("Total de normas com embeddings carregadas: %d%n", n);
    System.out.printf("Dimensionalidade dos embeddings:         %dD%n", dim);

    // 1. Inicialização K-Means++
    Random rng = new Random(seedAleatoria);
    float[][] centroides = inicializarKMeansPlusPlus(dados, k, dim, rng);

    // 2. Loop de Iterações do K-Means
    int[] atribuicoes = new int[n];
    boolean convergiu = false;
    int iteracao = 0;

    for (iteracao = 1; iteracao <= maxIteracoes; iteracao++) {
      int mudancas = 0;

      // Fase de atribuição de cada ponto ao centróide mais próximo (maior cosine similarity)
      for (int i = 0; i < n; i++) {
        float[] v = dados.get(i).vetor();
        int melhorCluster = 0;
        double maiorSimilaridade = -Double.MAX_VALUE;

        for (int c = 0; c < k; c++) {
          double sim = produtoEscalar(v, centroides[c]);
          if (sim > maiorSimilaridade) {
            maiorSimilaridade = sim;
            melhorCluster = c;
          }
        }

        if (atribuicoes[i] != melhorCluster) {
          atribuicoes[i] = melhorCluster;
          mudancas++;
        }
      }

      // Fase de atualização dos centróides
      float[][] novosCentroides = new float[k][dim];
      int[] contagemPorCluster = new int[k];

      for (int i = 0; i < n; i++) {
        int cluster = atribuicoes[i];
        contagemPorCluster[cluster]++;
        float[] v = dados.get(i).vetor();
        for (int d = 0; d < dim; d++) {
          novosCentroides[cluster][d] += v[d];
        }
      }

      double maxDeslocamento = 0.0;
      for (int c = 0; c < k; c++) {
        if (contagemPorCluster[c] > 0) {
          for (int d = 0; d < dim; d++) {
            novosCentroides[c][d] /= contagemPorCluster[c];
          }
          normalizarL2(novosCentroides[c]);
        } else {
          // Cluster vazio: reatribui para um ponto aleatório
          int pontoAleatorio = rng.nextInt(n);
          System.arraycopy(dados.get(pontoAleatorio).vetor(), 0, novosCentroides[c], 0, dim);
        }

        double distCentroide = 1.0 - produtoEscalar(centroides[c], novosCentroides[c]);
        if (distCentroide > maxDeslocamento) {
          maxDeslocamento = distCentroide;
        }
      }

      centroides = novosCentroides;

      if (mudancas == 0 || maxDeslocamento < TOLERANCIA_CONVERGENCIA) {
        convergiu = true;
        break;
      }
    }

    System.out.printf("K-Means convergiu em %d iterações (%s).%n",
        Math.min(iteracao, maxIteracoes), convergiu ? "convergência atingida" : "limite de iterações");

    // 3. Agrupamento e identificação da norma mais representativa (Medoid) de cada cluster
    List<List<EmbeddingNorma>> membrosPorCluster = new ArrayList<>(k);
    for (int c = 0; c < k; c++) {
      membrosPorCluster.add(new ArrayList<>());
    }
    for (int i = 0; i < n; i++) {
      membrosPorCluster.get(atribuicoes[i]).add(dados.get(i));
    }

    double inerciaTotal = 0.0;
    List<ClusterInfo> listaClusters = new ArrayList<>(k);

    for (int c = 0; c < k; c++) {
      List<EmbeddingNorma> membros = membrosPorCluster.get(c);
      int totalMembros = membros.size();
      double percentual = (totalMembros * 100.0) / n;
      float[] centroide = centroides[c];

      if (membros.isEmpty()) {
        listaClusters.add(new ClusterInfo(c + 1, 0, 0.0, 0.0, null, List.of(), List.of(), centroide));
        continue;
      }

      // Ordenar membros por similaridade decrescente com o centróide
      List<NormaRepresentativaComDistancia> ranking = new ArrayList<>(totalMembros);
      double somaSimilaridades = 0.0;

      for (EmbeddingNorma emb : membros) {
        double sim = Math.max(-1.0, Math.min(1.0, produtoEscalar(emb.vetor(), centroide)));
        double dist = Math.max(0.0, 1.0 - sim);
        inerciaTotal += dist * dist;
        somaSimilaridades += sim;

        NormaRepresentativa nr = new NormaRepresentativa(
            emb.idNorma(),
            emb.numero(),
            emb.ano(),
            emb.tipoEscrito(),
            emb.titulo(),
            emb.ementa(),
            sim,
            dist
        );
        ranking.add(new NormaRepresentativaComDistancia(nr, sim));
      }

      ranking.sort((a, b) -> Double.compare(b.similaridade, a.similaridade));

      double coesaoMedia = somaSimilaridades / totalMembros;
      NormaRepresentativa medoid = ranking.get(0).norma;

      int topN = Math.min(5, ranking.size());
      List<NormaRepresentativa> topNormas = ranking.subList(0, topN).stream()
          .map(r -> r.norma)
          .toList();

      List<String> termosFrequentes = extrairTermosFrequentesCluster(membros, 6);

      listaClusters.add(new ClusterInfo(
          c + 1,
          totalMembros,
          percentual,
          coesaoMedia,
          medoid,
          topNormas,
          termosFrequentes,
          centroide
      ));
    }

    // Ordenar clusters pelo tamanho decrescente
    listaClusters.sort((a, b) -> Integer.compare(b.totalNormas(), a.totalNormas()));

    long tempoTotalMs = System.currentTimeMillis() - inicioTempo;
    ResultadoClusterizacao resultado = new ResultadoClusterizacao(
        k,
        listaClusters,
        inerciaTotal,
        tempoTotalMs,
        n
    );

    return resultado;
  }

  /**
   * Retorna a norma/ementa que mais representa um determinado cluster (Medoid).
   */
  public NormaRepresentativa obterNormaMaisRepresentativa(int idCluster, ResultadoClusterizacao resultado) {
    if (resultado == null || resultado.clusters() == null) {
      return null;
    }
    return resultado.obterNormaMaisRepresentativaDoCluster(idCluster);
  }

  /**
   * Exibe o relatório detalhado da clusterização no console.
   */
  public void exibirRelatorioClusterizacao(ResultadoClusterizacao resultado) {
    if (resultado == null) return;

    System.out.println("\n" + "=".repeat(95));
    System.out.printf("=== RELATÓRIO DE CLUSTERIZAÇÃO DOS EMBEDDINGS (K=%d) ===%n", resultado.k());
    System.out.println("=".repeat(95));
    System.out.printf("⚡ Tempo de Execução:           %d ms (%.2f s)%n",
        resultado.tempoExecucaoMs(), resultado.tempoExecucaoMs() / 1000.0);
    System.out.printf("📊 Total de Normas Analisadas:  %d%n", resultado.totalNormasClusterizadas());
    System.out.printf("📐 Inércia (SSE de Distâncias): %.4f%n", resultado.inerciaTotal());
    System.out.println("=".repeat(95));

    for (ClusterInfo c : resultado.clusters()) {
      System.out.println();
      System.out.println(c.getResumoFormatado());
    }

    System.out.println("\n" + "=".repeat(95));
    System.out.println("=== RESUMO DOS MEDOIDS (NORMAS MAIS REPRESENTATIVAS DE CADA CLUSTER) ===");
    System.out.println("=".repeat(95));
    System.out.printf("%-10s | %-8s | %-32s | %-12s | %s%n",
        "Cluster", "Qtd", "Norma Medoid", "Similaridade", "Trecho da Ementa");
    System.out.println("-".repeat(95));

    for (ClusterInfo c : resultado.clusters()) {
      if (c.normaMaisRepresentativa() == null) continue;
      NormaRepresentativa nr = c.normaMaisRepresentativa();
      String ementaResumida = nr.ementa() != null ? nr.ementa().replaceAll("\\s+", " ").trim() : "";
      if (ementaResumida.length() > 50) {
        ementaResumida = ementaResumida.substring(0, 47) + "...";
      }

      System.out.printf("Cluster #%02d | %6d | %-32s | %10.4f   | %s%n",
          c.idCluster(),
          c.totalNormas(),
          nr.getIdentificacaoFormatada(),
          nr.similaridadeComCentroide(),
          ementaResumida
      );
    }
  }

  // --- Algoritmo K-Means++ Inicialização ---
  private float[][] inicializarKMeansPlusPlus(List<EmbeddingNorma> dados, int k, int dim, Random rng) {
    int n = dados.size();
    float[][] centroides = new float[k][dim];

    // 1º centróide escolhido uniformemente ao acaso
    int primeiroIdx = rng.nextInt(n);
    System.arraycopy(dados.get(primeiroIdx).vetor(), 0, centroides[0], 0, dim);

    double[] minDists = new double[n];
    for (int i = 0; i < n; i++) {
      double sim = produtoEscalar(dados.get(i).vetor(), centroides[0]);
      minDists[i] = Math.max(0.0, 1.0 - sim);
    }

    // Centróides 2..k escolhidos com probabilidade proporcional a D(x)^2
    for (int c = 1; c < k; c++) {
      double somaDistQuadrada = 0.0;
      for (int i = 0; i < n; i++) {
        somaDistQuadrada += minDists[i] * minDists[i];
      }

      double r = rng.nextDouble() * somaDistQuadrada;
      double acumulado = 0.0;
      int escolhido = n - 1;

      for (int i = 0; i < n; i++) {
        acumulado += minDists[i] * minDists[i];
        if (acumulado >= r) {
          escolhido = i;
          break;
        }
      }

      System.arraycopy(dados.get(escolhido).vetor(), 0, centroides[c], 0, dim);

      // Atualizar distâncias mínimas para o novo centróide
      for (int i = 0; i < n; i++) {
        double sim = produtoEscalar(dados.get(i).vetor(), centroides[c]);
        double dist = Math.max(0.0, 1.0 - sim);
        if (dist < minDists[i]) {
          minDists[i] = dist;
        }
      }
    }

    return centroides;
  }

  private List<String> extrairTermosFrequentesCluster(List<EmbeddingNorma> membros, int topK) {
    Map<String, Integer> contagem = new HashMap<>();
    for (EmbeddingNorma m : membros) {
      if (m.ementa() == null) continue;
      List<String> tokens = TokenizadorUtil.tokenizar(m.ementa(), true);
      Set<String> unicos = new HashSet<>(tokens);
      for (String t : unicos) {
        if (t.length() >= 4) {
          contagem.put(t, contagem.getOrDefault(t, 0) + 1);
        }
      }
    }

    List<Map.Entry<String, Integer>> lista = new ArrayList<>(contagem.entrySet());
    lista.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

    return lista.stream()
        .limit(topK)
        .map(Map.Entry::getKey)
        .toList();
  }

  private static double produtoEscalar(float[] a, float[] b) {
    double soma = 0.0;
    int len = Math.min(a.length, b.length);
    for (int i = 0; i < len; i++) {
      soma += (double) a[i] * b[i];
    }
    return soma;
  }

  private static void normalizarL2(float[] vetor) {
    if (vetor == null || vetor.length == 0) return;
    double soma = 0.0;
    for (float v : vetor) {
      soma += (double) v * v;
    }
    double norma = Math.sqrt(soma);
    if (norma > 1e-12) {
      for (int i = 0; i < vetor.length; i++) {
        vetor[i] = (float) (vetor[i] / norma);
      }
    }
  }

  private record NormaRepresentativaComDistancia(NormaRepresentativa norma, double similaridade) {}
}
