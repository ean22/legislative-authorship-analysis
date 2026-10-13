package org.entryPoint.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import org.entryPoint.benchmark.model.DefinicaoPipeline;
import org.entryPoint.benchmark.model.ResultadoExperimento;
import org.entryPoint.model.TopicoTematico;
import org.entryPoint.repository.GerenciadorConexao;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioPipeline;
import org.entryPoint.repository.RepositorioTopicos;

/**
 * Serviço responsável por definir, persistir e gerenciar o Pipeline Oficial do sistema
 * para dar prosseguimento aos próximos passos da análise de autoria legislativa.
 */
public class ServicoPipelineLegislativo {

  private final RepositorioPipeline repositorioPipeline;
  private final RepositorioTopicos repositorioTopicos;
  private final GerenciadorConexao gerenciadorConexao;

  public ServicoPipelineLegislativo() {
    this(new RepositorioPipeline(), new RepositorioTopicos(), new GerenciadorConexao());
  }

  public ServicoPipelineLegislativo(RepositorioBancoDados repo) {
    this(repo.getRepositorioPipeline(), repo.getRepositorioTopicos(), repo.getGerenciadorConexao());
  }

  public ServicoPipelineLegislativo(
      RepositorioPipeline repositorioPipeline,
      RepositorioTopicos repositorioTopicos,
      GerenciadorConexao gerenciadorConexao
  ) {
    this.repositorioPipeline = repositorioPipeline;
    this.repositorioTopicos = repositorioTopicos;
    this.gerenciadorConexao = gerenciadorConexao;
  }

  /**
   * Define o resultado vencedor como o Pipeline Ativo Oficial para os próximos passos.
   */
  public DefinicaoPipeline definirPipelineVencedor(ResultadoExperimento melhorResultado) throws SQLException {
    if (melhorResultado == null) {
      throw new IllegalArgumentException("Resultado do experimento não pode ser nulo.");
    }

    DefinicaoPipeline definicao = DefinicaoPipeline.aPartirDeResultado(melhorResultado);

    // 1. Persistir no banco de dados SQLite
    long id = repositorioPipeline.salvarPipelineAtivo(definicao);
    System.out.printf("Pipeline Oficial #%d gravado na tabela 'pipeline_ativo'.%n", id);

    // 2. Persistir os tópicos descobertos na tabela 'topicos'
    if (melhorResultado.topicos() != null && !melhorResultado.topicos().isEmpty()) {
      repositorioTopicos.salvarTopicosDescobertosEmLote(melhorResultado.topicos());
      System.out.printf("%d tópicos oficiais persistidos na tabela 'topicos'.%n", melhorResultado.topicos().size());
    }

    // 3. Salvar em arquivo estruturado JSON para consumo desacoplado
    salvarPipelineJson(definicao);

    return definicao;
  }

  /**
   * Obtém o pipeline ativo atualmente gravado no sistema.
   */
  public Optional<DefinicaoPipeline> obterPipelineAtivo() throws SQLException {
    return repositorioPipeline.obterPipelineAtivo();
  }

  /**
   * Persiste as atribuições documento-tópico do pipeline vencedor na tabela 'norma_topicos'.
   */
  public void persistirClassificacaoNormas(
      java.util.List<Long> docIds,
      double[][] matrizDocTopico,
      java.util.List<TopicoTematico> topicos
  ) throws SQLException {
    if (docIds == null || matrizDocTopico == null || docIds.isEmpty() || matrizDocTopico.length == 0) {
      return;
    }

    String sql = """
        INSERT OR REPLACE INTO norma_topicos (
          id_norma,
          id_topico,
          id_rotulo_topico,
          score_pertinencia,
          termos_chave
        ) VALUES (?, ?, ?, ?, ?)
        """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
        int limite = Math.min(docIds.size(), matrizDocTopico.length);
        for (int i = 0; i < limite; i++) {
          long idNorma = docIds.get(i);
          double[] linha = matrizDocTopico[i];

          int topicoDominante = 1;
          double maxScore = -1.0;
          for (int t = 0; t < linha.length; t++) {
            if (linha[t] > maxScore) {
              maxScore = linha[t];
              topicoDominante = t + 1;
            }
          }

          String termosChave = "";
          if (topicoDominante <= topicos.size()) {
            termosChave = String.join(", ", topicos.get(topicoDominante - 1).termosPrincipais());
          }

          stmt.setLong(1, idNorma);
          stmt.setInt(2, topicoDominante);
          stmt.setInt(3, topicoDominante);
          stmt.setDouble(4, Math.max(0.0, maxScore));
          stmt.setString(5, termosChave);
          stmt.addBatch();

          if (i > 0 && i % 1000 == 0) {
            stmt.executeBatch();
          }
        }
        stmt.executeBatch();
        conexao.commit();
        System.out.printf("Atribuição de %d normas persistida na tabela 'norma_topicos'.%n", limite);
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      } finally {
        conexao.setAutoCommit(true);
      }
    }
  }

  private void salvarPipelineJson(DefinicaoPipeline definicao) {
    String json = definicao.toJson();
    salvarEmArquivo("pipeline_config.json", json);
    try {
      File pai = new File("..").getCanonicalFile();
      if ("entryPoint".equals(pai.getName()) || "legislative-authorship-analysis".equals(pai.getName())) {
        File arquivoPai = new File(pai, "pipeline_config.json");
        salvarEmArquivo(arquivoPai.getAbsolutePath(), json);
      }
    } catch (IOException ignored) {}
  }

  private void salvarEmArquivo(String caminho, String conteudo) {
    try (FileWriter writer = new FileWriter(caminho, StandardCharsets.UTF_8)) {
      writer.write(conteudo);
      System.out.printf("Configuração do pipeline gravada em: %s%n", caminho);
    } catch (IOException e) {
      System.err.println("Erro ao salvar pipeline_config.json: " + e.getMessage());
    }
  }
}
