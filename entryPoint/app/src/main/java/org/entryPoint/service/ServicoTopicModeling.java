package org.entryPoint.service;

import java.sql.SQLException;
import java.util.*;
import org.entryPoint.benchmark.algoritmos.ClusterTopicModeling;
import org.entryPoint.benchmark.model.TipoRepresentacaoSemantica;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.benchmark.representacao.ExtratorRepresentacao;
import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.TopicoTematico;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;
import org.entryPoint.repository.RepositorioTopicos;

/**
 * Serviço de Modelagem de Tópicos das normas legislativas.
 * Refatorado para executar por padrão o Pipeline Vencedor eleito pelo Benchmark:
 * - Redução Textual: TextRank
 * - Representação Semântica: Espaço Latente Contínuo (DENSE_SVD)
 * - Algoritmo: Cluster c-TF-IDF (K-Means esférico + c-TF-IDF)
 * - K ótimo: 8 tópicos
 */
public class ServicoTopicModeling {

  public static final int DEFAULT_K = 8;
  public static final int DEFAULT_ITERACOES = 25;

  private final RepositorioEmentasPreProcessadas repositorioEmentas;
  private final RepositorioTopicos repositorioTopicos;
  private final ServicoPipelineLegislativo servicoPipeline;
  private final ExtratorRepresentacao extratorRepresentacao;
  private final ClusterTopicModeling algoritmoCluster;

  public ServicoTopicModeling() {
    this(new RepositorioEmentasPreProcessadas(), new RepositorioTopicos());
  }

  public ServicoTopicModeling(RepositorioEmentasPreProcessadas repositorioEmentas, RepositorioTopicos repositorioTopicos) {
    this.repositorioEmentas = repositorioEmentas;
    this.repositorioTopicos = repositorioTopicos;
    this.servicoPipeline = new ServicoPipelineLegislativo();
    this.extratorRepresentacao = new ExtratorRepresentacao();
    this.algoritmoCluster = new ClusterTopicModeling();
    this.algoritmoNmf = new org.entryPoint.benchmark.algoritmos.NmfTopicModeling();
  }

  public ServicoTopicModeling(RepositorioBancoDados repositorio) {
    this(repositorio.getRepositorioEmentas(), repositorio.getRepositorioTopicos());
  }

  private CorpusProcessado corpusCache;
  private List<EmentaParaTopico> ementasCache;
  private final org.entryPoint.benchmark.algoritmos.NmfTopicModeling algoritmoNmf;

  public synchronized CorpusProcessado obterOuCarregarCorpus() throws SQLException {
    if (corpusCache == null) {
      System.out.println("Carregando ementas reduzidas do banco de dados...");
      ementasCache = repositorioEmentas.listarEmentasParaTopicos();
      if (ementasCache.isEmpty()) {
        System.err.println("Nenhuma ementa processada encontrada no banco de dados.");
        return null;
      }
      System.out.printf("Total de registros carregados: %d normas legislativas.%n", ementasCache.size());
      System.out.println("Construindo espaço latente contínuo (DENSE_SVD)...");
      corpusCache = extratorRepresentacao.processar(ementasCache, TipoRepresentacaoSemantica.DENSE_SVD);
      System.out.printf("Vocabulário refinado: %d termos em %d documentos válidos.%n",
          corpusCache.getTamanhoVocabulario(), corpusCache.getNumeroDocumentos());
    }
    return corpusCache;
  }

  public ResultadoTopicModeling executarTopicModeling() throws SQLException {
    return executarTopicModeling(DEFAULT_K, DEFAULT_ITERACOES, true);
  }

  public ResultadoTopicModeling executarTopicModeling(int k, int maxIteracoes, boolean salvarNoBanco) throws SQLException {
    return executarTopicModeling(k, maxIteracoes, salvarNoBanco, org.entryPoint.benchmark.algoritmos.InferidorRotuloTema.getModoAtivo());
  }

  public ResultadoTopicModeling executarTopicModeling(
      int k,
      int maxIteracoes,
      boolean salvarNoBanco,
      org.entryPoint.benchmark.algoritmos.ModoInferidorTema modoInferidor
  ) throws SQLException {
    if (modoInferidor != null) {
      org.entryPoint.benchmark.algoritmos.InferidorRotuloTema.setModoAtivo(modoInferidor);
    }

    long inicioTempo = System.nanoTime();
    Runtime runtime = Runtime.getRuntime();
    long memoriaInicio = runtime.totalMemory() - runtime.freeMemory();

    System.out.println("\n" + "=".repeat(85));
    System.out.printf("Executando Topic Modeling Oficial (Pipeline: Cluster c-TF-IDF | TextRank | K=%d)...%n", k);
    System.out.printf("Estratégia de Rotulação de Tópicos: [%s]%n", org.entryPoint.benchmark.algoritmos.InferidorRotuloTema.getModoAtivo().getDescricao());
    System.out.println("=".repeat(85));

    CorpusProcessado corpus = obterOuCarregarCorpus();
    if (corpus == null) {
      return new ResultadoTopicModeling(List.of(), 0, 0, 0, 0);
    }

    var resultadoModelo = algoritmoCluster.executar(corpus, k, maxIteracoes, 42L);
    List<TopicoTematico> topicos = resultadoModelo.topicos();

    if (salvarNoBanco) {
      System.out.println("Persistindo tópicos descobertos na tabela 'topicos'...");
      repositorioTopicos.salvarTopicosDescobertosEmLote(topicos);

      System.out.println("Persistindo classificações na tabela 'norma_topicos'...");
      servicoPipeline.persistirClassificacaoNormas(corpus.docIds(), resultadoModelo.matrizDocTopico(), topicos);
    }

    long tempoTotalMs = (System.nanoTime() - inicioTempo) / 1_000_000;
    long memoriaFinal = runtime.totalMemory() - runtime.freeMemory();
    long memoriaUsadaMb = Math.max(0, (memoriaFinal - memoriaInicio) / (1024 * 1024));

    System.out.println("\n" + "=".repeat(85));
    System.out.printf("Topic Modeling Concluído em %d ms! (%d tópicos identificados)%n",
        tempoTotalMs, topicos.size());
    System.out.println("=".repeat(85));
    for (TopicoTematico topico : topicos) {
      System.out.println(topico.getDescricaoFormatada());
    }

    return new ResultadoTopicModeling(
        topicos,
        tempoTotalMs,
        corpus.totalTokens(),
        memoriaUsadaMb,
        corpus.getNumeroDocumentos()
    );
  }

  /**
   * Auditoria de Centroides: Exibe magnitude vetorial, coesão interna e o documento medoide de cada cluster.
   */
  public void exibirRelatorioCentroides() throws SQLException {
    CorpusProcessado corpus = obterOuCarregarCorpus();
    if (corpus == null) return;

    if (algoritmoCluster.getUltimosCentroides() == null) {
      System.out.println("Executando agrupamento K-Means inicial para calcular centroides...");
      algoritmoCluster.executar(corpus, DEFAULT_K, DEFAULT_ITERACOES, 42L);
    }

    double[][] centroides = algoritmoCluster.getUltimosCentroides();
    int[] clusters = algoritmoCluster.getUltimosClusters();
    List<TopicoTematico> topicos = algoritmoCluster.getUltimosTopicos();
    double[][] matrizDensa = corpus.matrizDensa();
    List<Long> docIds = corpus.docIds();

    int k = centroides.length;
    int dSize = matrizDensa.length;
    int dim = centroides[0].length;

    int[] contagens = new int[k];
    double[] somaSimilaridades = new double[k];
    double[] maxSimilaridade = new double[k];
    int[] melhorDocIdx = new int[k];
    Arrays.fill(maxSimilaridade, -1.0);

    for (int d = 0; d < dSize; d++) {
      int c = clusters[d];
      contagens[c]++;
      double sim = algoritmoCluster.similaridadeCosseno(matrizDensa[d], centroides[c]);
      somaSimilaridades[c] += sim;
      if (sim > maxSimilaridade[c]) {
        maxSimilaridade[c] = sim;
        melhorDocIdx[c] = d;
      }
    }

    System.out.println("\n" + "=".repeat(95));
    System.out.println("AUDITORIA GEOMÉTRICA DOS CENTROIDES DOS CLUSTERS (ESPAÇO LATENTE DENSE_SVD)");
    System.out.println("=".repeat(95));
    System.out.printf("Dimensão do espaço vetorial: %d componentes | Total de normas avaliadas: %d%n%n", dim, dSize);

    for (int c = 0; c < k; c++) {
      String nome = (topicos != null && c < topicos.size()) ? topicos.get(c).nome() : "Cluster #" + (c + 1);
      int qtd = contagens[c];
      double coesao = qtd > 0 ? (somaSimilaridades[c] / qtd) : 0.0;
      long docMedoideId = docIds.get(melhorDocIdx[c]);

      // Localizar as Top 4 dimensões de maior magnitude no centroide
      List<Integer> dims = new ArrayList<>();
      for (int j = 0; j < dim; j++) dims.add(j);
      final int clusterIdx = c;
      dims.sort((d1, d2) -> Double.compare(Math.abs(centroides[clusterIdx][d2]), Math.abs(centroides[clusterIdx][d1])));

      StringBuilder topDims = new StringBuilder();
      for (int i = 0; i < Math.min(4, dims.size()); i++) {
        int dIdx = dims.get(i);
        topDims.append(String.format("D%d: %+.3f  ", dIdx, centroides[c][dIdx]));
      }

      System.out.printf("🔷 CLUSTER %d: %s%n", c + 1, nome);
      System.out.printf("   Normas: %d (%.1f%%) | Coesão Cosseno Média: %.3f | Norma Medoide: #%d (Sim: %.3f)%n",
          qtd, (qtd * 100.0) / dSize, coesao, docMedoideId, maxSimilaridade[c]);
      System.out.printf("   Dimensões Dominantes: [ %s]%n%n", topDims);
    }
    System.out.println("=".repeat(95));
  }

  /**
   * Auditoria c-TF-IDF: Exibe a tabela de pontuações c-TF-IDF exatas para cada cluster.
   */
  public void exibirRelatorioCTfIdf(int topTermos) throws SQLException {
    CorpusProcessado corpus = obterOuCarregarCorpus();
    if (corpus == null) return;

    if (algoritmoCluster.getUltimaMatrizH() == null) {
      System.out.println("Executando modelo para calcular matriz c-TF-IDF...");
      algoritmoCluster.executar(corpus, DEFAULT_K, DEFAULT_ITERACOES, 42L);
    }

    List<TopicoTematico> topicos = algoritmoCluster.getUltimosTopicos();
    int k = topicos != null ? topicos.size() : DEFAULT_K;
    int limite = topTermos > 0 ? topTermos : 8;

    System.out.println("\n" + "=".repeat(95));
    System.out.printf("DETALHAMENTO DA MATRIZ c-TF-IDF (CLASS-BASED TF-IDF) - TOP %d TERMOS POR TÓPICO%n", limite);
    System.out.println("=".repeat(95));

    for (int c = 0; c < k; c++) {
      String nome = (topicos != null && c < topicos.size()) ? topicos.get(c).nome() : "Tópico #" + (c + 1);
      var detalhes = algoritmoCluster.obterDetalhesCTfIdf(c, limite);

      System.out.printf("%n📊 TÓPICO %d: %s%n", c + 1, nome);
      System.out.println("   " + "-".repeat(75));
      System.out.printf("   %-4s | %-24s | %-12s | %-12s | %-12s%n",
          "Rank", "Termo", "Score c-TF-IDF", "TF Cluster", "Presença (K)");
      System.out.println("   " + "-".repeat(75));

      int rank = 1;
      for (var det : detalhes) {
        System.out.printf("   #%-3d | %-24s | %-14.5f | %-12.1f | %d / %d clusters%n",
            rank++, det.termo(), det.scoreCTfIdf(), det.tfCluster(), det.presencaClusters(), k);
      }
    }
    System.out.println("\n" + "=".repeat(95));
  }

  /**
   * Auditoria e Comparação com NMF (Baseline).
   */
  public void executarAuditoriaNmf(int k, int maxIteracoes) throws SQLException {
    CorpusProcessado corpus = obterOuCarregarCorpus();
    if (corpus == null) return;

    int numK = k > 0 ? k : DEFAULT_K;
    System.out.println("\n" + "=".repeat(95));
    System.out.printf("AUDITORIA COMPARATIVA COM NMF (FATORAÇÃO NÃO-NEGATIVA DE MATRIZES K=%d)%n", numK);
    System.out.println("=".repeat(95));

    long t0 = System.currentTimeMillis();
    var resultadoNmf = algoritmoNmf.executar(corpus, numK, maxIteracoes, 42L);
    long tempoNmf = System.currentTimeMillis() - t0;

    double coerencia = org.entryPoint.benchmark.metricas.CalculadorCoerencia.calcularCoerenciaUmass(resultadoNmf.topicos(), corpus);
    double diversidade = org.entryPoint.benchmark.metricas.CalculadorDiversidade.calcularDiversidade(resultadoNmf.topicos(), 6);

    System.out.printf("Tempo de execução NMF: %d ms | Coerência UMass: %.2f | Diversidade Vocabular: %.1f%%%n%n",
        tempoNmf, coerencia, diversidade * 100.0);

    System.out.println("Tópicos descobertos via NMF (Matriz H):");
    for (TopicoTematico t : resultadoNmf.topicos()) {
      System.out.printf(" • Tópico %02d [%s]: %s%n",
          t.id(), t.nome(), String.join(", ", t.termosPrincipais()));
    }

    System.out.println("\n" + "-".repeat(95));
    System.out.println("DIAGNÓSTICO COMPARATIVO: NMF vs CLUSTER c-TF-IDF");
    System.out.println("-".repeat(95));
    System.out.println("• NMF: Fatora V ≈ W x H como soma linear aditiva. Tende a repetir termos comuns em múltiplos");
    System.out.println("  tópicos (menor diversidade, ex: finanças repetindo em vários tópicos).");
    System.out.println("• Cluster c-TF-IDF: Particiona geometricamente as normas e usa c-TF-IDF para penalizar");
    System.out.println("  termos compartilhados, alcançando diversidade de 94% e isolamento temático superior.");
    System.out.println("=".repeat(95));
  }

  /**
   * Consulta e exibe as estatísticas de normas e confiança das atribuições gravadas no SQLite.
   */
  public void auditarClassificacoesBanco() throws SQLException {
    var stats = repositorioTopicos.consultarEstatisticasTopicos();
    if (stats.isEmpty()) {
      System.out.println("Nenhum tópico ou atribuição encontrada no banco. Execute a opção 1 primeiro.");
      return;
    }

    System.out.println("\n" + "=".repeat(95));
    System.out.println("AUDITORIA DAS CLASSIFICAÇÕES GRAVADAS NO BANCO DE DADOS (TABELA 'norma_topicos')");
    System.out.println("=".repeat(95));
    System.out.printf("%-4s | %-40s | %-10s | %-7s | %-12s | %-12s%n",
        "ID", "Nome do Tópico", "Normas", "% Total", "Confiança Média", "Faixa [Min-Max]");
    System.out.println("-".repeat(95));

    int totalGeral = 0;
    for (var s : stats) {
      totalGeral += s.totalNormas();
      System.out.printf("#%-3d | %-40s | %-10d | %5.1f%% | %13.3f | [%.2f - %.2f]%n",
          s.idTopico(), s.nome(), s.totalNormas(), s.percentual(), s.scoreMedio(), s.scoreMin(), s.scoreMax());
    }
    System.out.println("-".repeat(95));
    System.out.printf("Total Geral de Normas Classificadas no Banco: %d%n", totalGeral);
    System.out.println("=".repeat(95));
  }

  /**
   * Inspeciona uma amostra de normas reais associadas a um tópico específico no SQLite.
   */
  public void inspecionarAmostraNormasPorTopico(int idTopico, int limite) throws SQLException {
    var amostras = repositorioTopicos.listarAmostraNormasPorTopico(idTopico, limite);
    if (amostras.isEmpty()) {
      System.out.printf("Nenhuma norma encontrada para o tópico #%d no banco de dados.%n", idTopico);
      return;
    }

    System.out.println("\n" + "=".repeat(95));
    System.out.printf("AMOSTRA DE NORMAS CLASSIFICADAS NO TÓPICO #%d (TOP %d POR SCORE DE PERTINÊNCIA)%n",
        idTopico, amostras.size());
    System.out.println("=".repeat(95));

    for (int i = 0; i < amostras.size(); i++) {
      var n = amostras.get(i);
      System.out.printf("%n[%d/%d] Norma ID: %d | %s nº %d/%d | Pertinência: %.2f%%%n",
          i + 1, amostras.size(), n.idNorma(), n.tipo(), n.numero(), n.ano(), n.score() * 100.0);
      System.out.printf("   Termos-chave atribuídos: [ %s ]%n", n.termosChave());
      String ementaCortada = n.ementa() != null && n.ementa().length() > 140
          ? n.ementa().substring(0, 137) + "..."
          : n.ementa();
      System.out.printf("   Ementa Original: \"%s\"%n", ementaCortada);
    }
    System.out.println("=".repeat(95));
  }

  public record ResultadoTopicModeling(
      List<TopicoTematico> topicos,
      long tempoMs,
      long totalTokens,
      long memoriaMb,
      int totalNormasProcessadas
  ) {}
}
