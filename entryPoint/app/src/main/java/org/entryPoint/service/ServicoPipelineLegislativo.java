package org.entryPoint.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
      try {
        // 1. Sincronizar rotulos_topicos para garantir integridade referencial com os 8 tópicos
        if (topicos != null && !topicos.isEmpty()) {
          try (PreparedStatement stmtRotulos = conexao.prepareStatement(
              "INSERT OR REPLACE INTO rotulos_topicos (id, nome) VALUES (?, ?)"
          )) {
            for (TopicoTematico topico : topicos) {
              stmtRotulos.setInt(1, topico.id());
              stmtRotulos.setString(2, topico.nome());
              stmtRotulos.addBatch();
            }
            stmtRotulos.executeBatch();
          }
        }

        // 2. Limpar atribuições anteriores para assegurar que apenas os tópicos oficiais permaneçam
        try (PreparedStatement deleteStmt = conexao.prepareStatement("DELETE FROM norma_topicos")) {
          deleteStmt.executeUpdate();
        }

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

          int idTopico = (topicoDominante > 0 && topicoDominante <= topicos.size())
              ? topicos.get(topicoDominante - 1).id()
              : topicoDominante;

          String termosChave = "";
          if (topicoDominante > 0 && topicoDominante <= topicos.size()) {
            termosChave = String.join(", ", topicos.get(topicoDominante - 1).termosPrincipais());
          }

          stmt.setLong(1, idNorma);
          stmt.setInt(2, idTopico);
          stmt.setInt(3, idTopico);
          stmt.setDouble(4, Math.max(0.0, maxScore));
          stmt.setString(5, termosChave);
          stmt.addBatch();

          if (i > 0 && i % 1000 == 0) {
            stmt.executeBatch();
          }
        }
        stmt.executeBatch();
      }

      // Técnica B: Resolução de Revogações por Herança Relacional
      int herdadas = resolverRevogacoesPorHerancaRelacional(conexao, topicos);

      conexao.commit();
      int limite = Math.min(docIds.size(), matrizDocTopico.length);
      System.out.printf("Atribuição de %d normas persistida na tabela 'norma_topicos'.%n", limite);
      if (herdadas > 0) {
        System.out.printf("Técnica B (Herança Relacional): %d normas revogatórias classificadas por herança relacional.%n", herdadas);
      }
    } catch (SQLException e) {
      conexao.rollback();
      throw e;
    } finally {
      conexao.setAutoCommit(true);
    }
  }
  }

  /**
   * Técnica B: Resolução de Revogações por Herança Relacional.
   * Identifica normas não classificadas no corpus (ex: atos puramente revogatórios que perderam
   * tokens no pré-processamento) e resolve sua temática herdando da norma revogada
   * via relacionamento legislativo ou aplicando fallback administrativo de gestão municipal.
   */
  public int resolverRevogacoesPorHerancaRelacional(Connection conexao, java.util.List<TopicoTematico> topicos) throws SQLException {
    String sqlBuscarRevogacoesNaoClassificadas = """
        SELECT n.id, n.ementa
        FROM normas n
        LEFT JOIN norma_topicos nt ON n.id = nt.id_norma
        WHERE nt.id_topico IS NULL
          AND (
            n.id IN (SELECT id_norma_origem FROM norma_relacionamentos WHERE tipo_relacionamento = 'REVOGACAO')
            OR n.ementa LIKE 'REVOGA O DECRETO%'
            OR n.ementa LIKE 'REVOGA A LEI%'
            OR n.ementa LIKE 'DISPÕE SOBRE A REVOGAÇÃO%'
            OR n.ementa LIKE 'REVOGAM-SE%'
            OR n.ementa LIKE 'REVOGA O ARTIGO%'
          )
        """;

    String sqlBuscarHerancaDestino = """
        SELECT nt.id_topico, nt.id_rotulo_topico, nt.score_pertinencia, nt.termos_chave
        FROM norma_relacionamentos r
        JOIN norma_topicos nt ON r.id_norma_destino = nt.id_norma
        WHERE r.id_norma_origem = ? AND r.tipo_relacionamento = 'REVOGACAO'
        LIMIT 1
        """;

    String sqlInsert = """
        INSERT OR REPLACE INTO norma_topicos (
          id_norma,
          id_topico,
          id_rotulo_topico,
          score_pertinencia,
          termos_chave
        ) VALUES (?, ?, ?, ?, ?)
        """;

    int idTopicoGestaoMunicipal = 5;
    if (topicos != null) {
      for (TopicoTematico t : topicos) {
        if (t.nome() != null && t.nome().contains("Serviços Públicos")) {
          idTopicoGestaoMunicipal = t.id();
          break;
        }
      }
    }

    int totalResolvidas = 0;
    try (PreparedStatement stmtBusca = conexao.prepareStatement(sqlBuscarRevogacoesNaoClassificadas);
         PreparedStatement stmtHeranca = conexao.prepareStatement(sqlBuscarHerancaDestino);
         PreparedStatement stmtInsert = conexao.prepareStatement(sqlInsert);
         ResultSet rs = stmtBusca.executeQuery()) {

      while (rs.next()) {
        long idNorma = rs.getLong("id");

        int idTopicoFinal = idTopicoGestaoMunicipal;
        int idRotuloFinal = idTopicoGestaoMunicipal;
        double scoreFinal = 0.85;
        String termosChaveFinal = "heranca_relacional_revogacao";

        // Tentar herdar da norma revogada vinculada
        stmtHeranca.setLong(1, idNorma);
        try (ResultSet rsHeranca = stmtHeranca.executeQuery()) {
          if (rsHeranca.next()) {
            idTopicoFinal = rsHeranca.getInt("id_topico");
            idRotuloFinal = rsHeranca.getInt("id_rotulo_topico");
            scoreFinal = rsHeranca.getDouble("score_pertinencia");
            termosChaveFinal = rsHeranca.getString("termos_chave");
          }
        }

        stmtInsert.setLong(1, idNorma);
        stmtInsert.setInt(2, idTopicoFinal);
        stmtInsert.setInt(3, idRotuloFinal);
        stmtInsert.setDouble(4, Math.max(0.70, scoreFinal));
        stmtInsert.setString(5, termosChaveFinal != null ? termosChaveFinal : "heranca_relacional_revogacao");
        stmtInsert.addBatch();
        totalResolvidas++;

        if (totalResolvidas % 500 == 0) {
          stmtInsert.executeBatch();
        }
      }
      stmtInsert.executeBatch();
    }

    return totalResolvidas;
  }

  public int resolverRevogacoesPorHerancaRelacional() throws SQLException {
    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try {
        var topicos = repositorioTopicos.listarTopicosSalvos();
        int total = resolverRevogacoesPorHerancaRelacional(conexao, topicos);
        conexao.commit();
        return total;
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
