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
  }

  public ServicoTopicModeling(RepositorioBancoDados repositorio) {
    this(repositorio.getRepositorioEmentas(), repositorio.getRepositorioTopicos());
  }

  public ResultadoTopicModeling executarTopicModeling() throws SQLException {
    return executarTopicModeling(DEFAULT_K, DEFAULT_ITERACOES, true);
  }

  public ResultadoTopicModeling executarTopicModeling(int k, int maxIteracoes, boolean salvarNoBanco) throws SQLException {
    long inicioTempo = System.nanoTime();
    Runtime runtime = Runtime.getRuntime();
    long memoriaInicio = runtime.totalMemory() - runtime.freeMemory();

    System.out.println("\n" + "=".repeat(85));
    System.out.printf("Executando Topic Modeling Oficial (Pipeline Vencedor: Cluster c-TF-IDF | TextRank | K=%d)...%n", k);
    System.out.println("=".repeat(85));

    // 1. Carregar ementas utilizando a redução TextRank do pipeline vencedor
    List<EmentaParaTopico> ementas = repositorioEmentas.listarEmentasParaTopicos();
    if (ementas.isEmpty()) {
      System.err.println("Nenhuma ementa processada encontrada no banco de dados.");
      return new ResultadoTopicModeling(List.of(), 0, 0, 0, 0);
    }
    System.out.printf("Total de registros carregados: %d normas legislativas.%n", ementas.size());

    // 2. Extrair representação semântica densa contínua (DENSE_SVD)
    CorpusProcessado corpus = extratorRepresentacao.processar(ementas, TipoRepresentacaoSemantica.DENSE_SVD);
    System.out.printf("Vocabulário refinado: %d termos em %d documentos válidos.%n",
        corpus.getTamanhoVocabulario(), corpus.getNumeroDocumentos());

    // 3. Executar o modelo vencedor (Cluster c-TF-IDF) com semente determinística 42
    var resultadoModelo = algoritmoCluster.executar(corpus, k, maxIteracoes, 42L);
    List<TopicoTematico> topicos = resultadoModelo.topicos();

    // 4. Persistir os tópicos descobertos e a classificação das normas se solicitado
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
    System.out.printf("Topic Modeling Concluído em %d ms! (%d tópicos oficiais identificados)%n",
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

  public record ResultadoTopicModeling(
      List<TopicoTematico> topicos,
      long tempoMs,
      long totalTokens,
      long memoriaMb,
      int totalNormasProcessadas
  ) {}
}
