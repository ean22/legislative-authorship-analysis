package org.entryPoint.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import org.entryPoint.benchmark.algoritmos.*;
import org.entryPoint.benchmark.metricas.*;
import org.entryPoint.benchmark.model.*;
import org.entryPoint.benchmark.representacao.*;
import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.TopicoTematico;
import org.entryPoint.repository.RepositorioAutores;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;
import org.entryPoint.repository.RepositorioTopicos;
import org.entryPoint.service.ServicoPipelineLegislativo;

/**
 * Orquestrador do Benchmark Experimental Reproduzível de Topic Modeling.
 * Itera sobre técnicas de redução de texto inteligente e semântica (stopwords, chunking,
 * embeddings, Chi-Square, MMR, clustering semântico) e algoritmos de modelagem eficientes,
 * evidenciando com métricas reais o porquê de cada escolha e definindo o melhor pipeline
 * para dar prosseguimento às próximas etapas.
 */
public class ServicoBenchmark {

  private final RepositorioEmentasPreProcessadas repositorioEmentas;
  private final RepositorioAutores repositorioAutores;
  private final RepositorioTopicos repositorioTopicos;
  private final ServicoPipelineLegislativo servicoPipeline;
  private final ExtratorRepresentacao extratorRepresentacao;

  private final NmfTopicModeling nmfModel = new NmfTopicModeling();
  private final LdaTopicModeling ldaModel = new LdaTopicModeling();
  private final ClusterTopicModeling clusterModel = new ClusterTopicModeling();

  private List<Long> ultimosDocIds = new ArrayList<>();

  public ServicoBenchmark() {
    this(new RepositorioEmentasPreProcessadas(), new RepositorioAutores(), new RepositorioTopicos());
  }

  public ServicoBenchmark(RepositorioBancoDados repositorio) {
    this(repositorio.getRepositorioEmentas(), repositorio.getRepositorioAutores(), repositorio.getRepositorioTopicos());
  }

  public ServicoBenchmark(
      RepositorioEmentasPreProcessadas repositorioEmentas,
      RepositorioAutores repositorioAutores,
      RepositorioTopicos repositorioTopicos
  ) {
    this.repositorioEmentas = repositorioEmentas;
    this.repositorioAutores = repositorioAutores;
    this.repositorioTopicos = repositorioTopicos;
    this.servicoPipeline = new ServicoPipelineLegislativo();
    this.extratorRepresentacao = new ExtratorRepresentacao();
  }

  /**
   * Executa uma grade de benchmark rápida com amostra representativa (1.500 normas)
   * permitindo verificação imediata e testes reproduzíveis.
   */
  public RelatorioBenchmark executarBenchmarkRapido() throws SQLException {
    List<ConfiguracaoExperimento> grade = criarGradeExperimentosPadrao();
    return executarGrade(grade, 1500, true);
  }

  /**
   * Executa a grade de benchmark completa sobre todo o corpus de normas do banco.
   */
  public RelatorioBenchmark executarBenchmarkCompleto() throws SQLException {
    List<ConfiguracaoExperimento> grade = criarGradeExperimentosPadrao();
    return executarGrade(grade, 0, true);
  }

  /**
   * Constrói a grade completa de experimentos comparativos iterando sobre:
   * - Reduções inteligentes: Stopwords, Chunking, Embeddings, Chi-Square, MMR, Clustering Semântico, TextRank, Raw.
   * - Topic Modeling: NMF, LDA, Cluster + c-TF-IDF em múltiplos K.
   */
  public List<ConfiguracaoExperimento> criarGradeExperimentosPadrao() {
    List<ConfiguracaoExperimento> grade = new ArrayList<>();

    // 1. Baseline de Controle: NMF K=12 com Stopwords TF-IDF
    grade.add(ConfiguracaoExperimento.criarBaseline());

    // 2. Variação de K sobre NMF
    grade.add(new ConfiguracaoExperimento(
        "nmf_k06",
        "NMF Compacto (K=6)",
        TipoReducaoTextual.STOPWORDS_TFIDF,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        6, 25, 42L, false));

    grade.add(new ConfiguracaoExperimento(
        "nmf_k08",
        "NMF Equilibrado (K=8)",
        TipoReducaoTextual.STOPWORDS_TFIDF,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    grade.add(new ConfiguracaoExperimento(
        "nmf_k16",
        "NMF Granular (K=16)",
        TipoReducaoTextual.STOPWORDS_TFIDF,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        16, 25, 42L, false));

    // 3. Técnicas de Redução Inteligente e Semântica solicitadas:
    // TextRank (Grafo)
    grade.add(new ConfiguracaoExperimento(
        "textrank_nmf_k08",
        "TextRank + NMF (K=8)",
        TipoReducaoTextual.TEXTRANK,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Maximal Marginal Relevance (MMR)
    grade.add(new ConfiguracaoExperimento(
        "mmr_nmf_k08",
        "MMR Diversificado + NMF (K=8)",
        TipoReducaoTextual.MMR,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Chi-Square (Qui-Quadrado supervisionado)
    grade.add(new ConfiguracaoExperimento(
        "chisquare_nmf_k08",
        "Qui-Quadrado + NMF (K=8)",
        TipoReducaoTextual.CHI_SQUARE,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Chunking Sentencial / Dispositivos
    grade.add(new ConfiguracaoExperimento(
        "chunking_nmf_k08",
        "Chunking Sentencial + NMF (K=8)",
        TipoReducaoTextual.CHUNKING_SENTENCAS,
        TipoChunking.SENTENCE_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Embeddings Espaço Latente (SVD/LSA)
    grade.add(new ConfiguracaoExperimento(
        "embeddings_nmf_k08",
        "Embeddings SVD + NMF (K=8)",
        TipoReducaoTextual.EMBEDDINGS_SVD,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.DENSE_SVD,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Clustering Semântico de Sentenças
    grade.add(new ConfiguracaoExperimento(
        "clustering_sentencas_nmf_k08",
        "Clustering Semântico + NMF (K=8)",
        TipoReducaoTextual.CLUSTERING_SEMANTICO,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // Baseline sem redução (Texto Integral da Ementa)
    grade.add(new ConfiguracaoExperimento(
        "raw_nmf_k08",
        "Texto Integral (Raw) + NMF (K=8)",
        TipoReducaoTextual.RAW_EMENTA,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        8, 25, 42L, false));

    // 4. Outras Técnicas de Topic Modeling: LDA e Clustering Vetorial + c-TF-IDF
    grade.add(new ConfiguracaoExperimento(
        "lda_k08",
        "LDA Probabilístico (K=8)",
        TipoReducaoTextual.STOPWORDS_TFIDF,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.LDA,
        8, 25, 42L, false));

    grade.add(new ConfiguracaoExperimento(
        "cluster_ctfidf_textrank_k08",
        "Cluster c-TF-IDF + TextRank (K=8)",
        TipoReducaoTextual.TEXTRANK,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.DENSE_SVD,
        TipoAlgoritmoTopicModeling.CLUSTER_C_TFIDF,
        8, 25, 42L, false));

    grade.add(new ConfiguracaoExperimento(
        "cluster_ctfidf_clustering_k08",
        "Cluster c-TF-IDF + Clustering Sentencial (K=8)",
        TipoReducaoTextual.CLUSTERING_SEMANTICO,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.DENSE_SVD,
        TipoAlgoritmoTopicModeling.CLUSTER_C_TFIDF,
        8, 25, 42L, false));

    return grade;
  }

  /**
   * Executa a grade de experimentos especificada.
   */
  public RelatorioBenchmark executarGrade(
      List<ConfiguracaoExperimento> grade,
      int limiteDocs,
      boolean salvarRelatorioEmArquivo
  ) throws SQLException {
    long tempoInicioTotal = System.currentTimeMillis();

    System.out.println("=".repeat(110));
    System.out.println("BENCHMARK EXPERIMENTAL DE TOPIC MODELING E REDUÇÃO INTELIGENTE DE TEXTO LEGISLATIVO");
    System.out.println("=".repeat(110));
    System.out.printf("Total de configurações avaliadas: %d | Amostragem de normas: %s%n",
        grade.size(), limiteDocs > 0 ? String.valueOf(limiteDocs) : "Todas as 14.375 normas");

    // Carregar metadados de autoria e órgãos emissores
    Map<String, List<Long>> orgaosComNormas = repositorioAutores.listarOrgaosComNormas();
    Map<Long, List<Long>> autoresComNormas = repositorioAutores.listarAutoresComNormas();
    Map<Long, String> nomesAutores = repositorioAutores.listarNomesAutores();

    // Cache de corpora por TipoReducaoTextual para otimização de I/O
    Map<TipoReducaoTextual, CorpusProcessado> cacheCorpora = new HashMap<>();

    List<ResultadoExperimento> resultados = new ArrayList<>();
    ResultadoExperimento resultadoBaseline = null;
    int totalNormasAnalisadas = 0;

    for (int idx = 0; idx < grade.size(); idx++) {
      ConfiguracaoExperimento config = grade.get(idx);
      System.out.printf("%n[%02d/%02d] Experimento: %-38s [Redução: %-18s | K=%2d]...%n",
          idx + 1, grade.size(), config.nome(), config.reducao().name(), config.k());

      // 1. Obter ou construir o Corpus com a redução solicitada
      CorpusProcessado corpus = cacheCorpora.get(config.reducao());
      if (corpus == null) {
        List<EmentaParaTopico> ementas = repositorioEmentas.listarEmentasPorEstrategia(config.reducao(), limiteDocs);
        if (ementas.isEmpty()) {
          System.err.printf("  Nenhuma ementa encontrada para a redução %s, pulando...%n", config.reducao());
          continue;
        }
        totalNormasAnalisadas = Math.max(totalNormasAnalisadas, ementas.size());
        corpus = extratorRepresentacao.processar(ementas, config.representacao());
        cacheCorpora.put(config.reducao(), corpus);
        this.ultimosDocIds = new ArrayList<>(corpus.docIds());
      }

      // 2. Executar Modelagem com Seed Primária (42) e Secundária (101 para Estabilidade)
      long tInicioExp = System.currentTimeMillis();
      Runtime rt = Runtime.getRuntime();
      long memInicio = rt.totalMemory() - rt.freeMemory();

      AlgoritmoTopicModeling algoritmo = obterAlgoritmo(config.algoritmo());
      var resModelo1 = algoritmo.executar(corpus, config.k(), config.maxIteracoes(), config.seed());
      var resModelo2 = algoritmo.executar(corpus, config.k(), config.maxIteracoes(), config.seed() + 59);

      long tempoExpMs = System.currentTimeMillis() - tInicioExp;
      long memUsada = Math.max(0, (rt.totalMemory() - rt.freeMemory() - memInicio) / (1024 * 1024));

      // 3. Avaliar Métricas Reais e Utilidade Autoral com Diagnóstico
      var avaliacao = AvaliadorBenchmark.avaliar(
          resModelo1.topicos(),
          resModelo2.topicos(),
          corpus,
          resModelo1.matrizDocTopico(),
          orgaosComNormas,
          autoresComNormas,
          nomesAutores,
          config
      );

      ResultadoExperimento resExp = new ResultadoExperimento(
          config,
          resModelo1.topicos(),
          resModelo1.matrizDocTopico(),
          avaliacao.metricas(),
          tempoExpMs,
          memUsada,
          avaliacao.perfisOrgaos(),
          avaliacao.perfisAutores(),
          avaliacao.justificativaEscolha()
      );

      resultados.add(resExp);
      if (config.isBaseline()) {
        resultadoBaseline = resExp;
      }

      System.out.printf("  -> Concluído em %d ms. %s%n", tempoExpMs, avaliacao.metricas().getSumarioFormatado());
      System.out.printf("     Justificativa empírica: %s%n", avaliacao.justificativaEscolha());
    }

    if (resultadoBaseline == null && !resultados.isEmpty()) {
      resultadoBaseline = resultados.get(0);
    }

    // Ordenar resultados pelo Thematic Utility Score decrescente
    resultados.sort((a, b) -> Double.compare(
        b.metricas().scoreCompostoGlobal(),
        a.metricas().scoreCompostoGlobal()
    ));

    ResultadoExperimento melhorResultado = resultados.get(0);
    double scoreMelhor = melhorResultado.metricas().scoreCompostoGlobal();
    double scoreBaseline = resultadoBaseline != null ? resultadoBaseline.metricas().scoreCompostoGlobal() : scoreMelhor;
    double ganhoPercentual = scoreBaseline > 0 ? ((scoreMelhor - scoreBaseline) / scoreBaseline) * 100.0 : 0.0;

    RelatorioBenchmark relatorio = new RelatorioBenchmark(
        LocalDateTime.now(),
        totalNormasAnalisadas,
        resultadoBaseline,
        melhorResultado,
        resultados,
        ganhoPercentual
    );

    // Exibir ranking consolidado e tabela detalhada de justificativas no console
    imprimirRelatorioConsole(relatorio);

    // Salvar relatório em arquivo Markdown
    if (salvarRelatorioEmArquivo) {
      salvarRelatorioDisco(relatorio);
    }

    long tempoTotalBenchmark = System.currentTimeMillis() - tempoInicioTotal;
    System.out.printf("%nTempo total de execução do benchmark: %.2f segundos.%n", tempoTotalBenchmark / 1000.0);
    System.out.println("=".repeat(110));

    return relatorio;
  }

  /**
   * Define oficialmente o resultado vencedor como o Pipeline Padrão do sistema,
   * persistindo-o no SQLite, no arquivo JSON, na tabela 'topicos' e em 'norma_topicos'.
   */
  public DefinicaoPipeline definirEFixarMelhorPipeline(RelatorioBenchmark relatorio) throws SQLException {
    ResultadoExperimento melhor = relatorio.melhorResultado();
    if (melhor == null) {
      throw new IllegalStateException("Nenhum resultado vencedor encontrado no relatório.");
    }

    System.out.println("\n" + "=".repeat(85));
    System.out.println("DEFININDO E FIXANDO O MELHOR PIPELINE PARA OS PRÓXIMOS PASSOS");
    System.out.println("=".repeat(85));

    DefinicaoPipeline pipelineOficial = servicoPipeline.definirPipelineVencedor(melhor);

    // Persistir atribuição documento-tópico para todo o corpus testado
    if (!ultimosDocIds.isEmpty()) {
      servicoPipeline.persistirClassificacaoNormas(ultimosDocIds, melhor.matrizDocTopico(), melhor.topicos());
    }

    System.out.println(pipelineOficial.getFormatacaoVisual());
    return pipelineOficial;
  }

  public ServicoPipelineLegislativo getServicoPipeline() {
    return servicoPipeline;
  }

  private AlgoritmoTopicModeling obterAlgoritmo(TipoAlgoritmoTopicModeling tipo) {
    return switch (tipo) {
      case NMF -> nmfModel;
      case LDA -> ldaModel;
      case CLUSTER_C_TFIDF -> clusterModel;
    };
  }

  private void imprimirRelatorioConsole(RelatorioBenchmark relatorio) {
    System.out.println("\n" + "=".repeat(140));
    System.out.println("TABELA COMPARATIVA DE TÓPICOS E REDUÇÕES TEXTUAIS (EVIDENCIANDO O PORQUÊ DA ESCOLHA COM MÉTRICAS REAIS)");
    System.out.println("=".repeat(140));
    System.out.println("| Rank | Configuração             | Redução      |  K |  Score | Coerência | Div.  | UtilAut | Por que escolher? (Diagnóstico Empírico das Métricas)                  |");
    System.out.println("|:----:|:-------------------------|:-------------|:--:|:------:|:---------:|:-----:|:-------:|:-----------------------------------------------------------------------|");

    List<ResultadoExperimento> lista = relatorio.todosResultados();
    for (int i = 0; i < lista.size(); i++) {
      System.out.println(lista.get(i).getLinhaTabelaComJustificativa(i + 1));
    }
    System.out.println("=".repeat(140));

    System.out.printf("%n🏆 MELHOR PIPELINE ELEITO: %s%n", relatorio.melhorResultado().configuracao().nome());
    System.out.printf("   Score Global: %.3f (Ganho sobre baseline: +%.1f%%)%n",
        relatorio.melhorResultado().metricas().scoreCompostoGlobal(),
        relatorio.ganhoSobreBaselinePercentual());
    System.out.printf("   Justificativa: %s%n", relatorio.melhorResultado().justificativaEscolha());
    System.out.println("   Tópicos Descobertos:");
    for (TopicoTematico topico : relatorio.melhorResultado().topicos()) {
      System.out.printf("     • Tópico %02d [%s]: %s%n",
          topico.id(), topico.nome(), String.join(", ", topico.termosPrincipais()));
    }

    System.out.println("\n🏛️ PERFIS TEMÁTICOS DOS PRINCIPAIS ÓRGÃOS EMISSORES:");
    int orgaosExibidos = 0;
    for (var perfil : relatorio.melhorResultado().perfisOrgaos()) {
      System.out.println("   " + perfil.getFormatacaoVisual());
      if (++orgaosExibidos >= 8) break;
    }

    System.out.println("\n👤 PERFIS TEMÁTICOS DOS PRINCIPAIS AUTORES POLÍTICOS:");
    int autoresExibidos = 0;
    for (var perfil : relatorio.melhorResultado().perfisAutores()) {
      System.out.println("   " + perfil.getFormatacaoVisual());
      if (++autoresExibidos >= 8) break;
    }
  }

  private void salvarRelatorioDisco(RelatorioBenchmark relatorio) {
    String conteudo = relatorio.gerarMarkdown();
    File arquivoLocal = new File("benchmark_results.md");
    try (FileWriter writer = new FileWriter(arquivoLocal, StandardCharsets.UTF_8)) {
      writer.write(conteudo);
      System.out.printf("%nRelatório detalhado do benchmark salvo com sucesso em: %s%n", arquivoLocal.getAbsolutePath());
    } catch (IOException e) {
      System.err.println("Erro ao salvar relatório de benchmark em disco: " + e.getMessage());
    }

    try {
      File pai = new File("..").getCanonicalFile();
      if ("entryPoint".equals(pai.getName()) || "legislative-authorship-analysis".equals(pai.getName())) {
        File arquivoPai = new File(pai, "benchmark_results.md");
        try (FileWriter writer = new FileWriter(arquivoPai, StandardCharsets.UTF_8)) {
          writer.write(conteudo);
        }
      }
    } catch (IOException ignored) {}
  }
}
