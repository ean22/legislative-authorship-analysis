package org.entryPoint.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.entryPoint.benchmark.model.*;

/**
 * Repositório responsável por persistir e recuperar o Pipeline Legislativo Ativo
 * selecionado pelo benchmark experimental.
 */
public class RepositorioPipeline {

  private final GerenciadorConexao gerenciadorConexao;
  private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public RepositorioPipeline() {
    this(new GerenciadorConexao());
  }

  public RepositorioPipeline(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public long salvarPipelineAtivo(DefinicaoPipeline definicao) throws SQLException {
    String desativarSql = "UPDATE pipeline_ativo SET ativo = 0";

    String inserirSql = """
        INSERT INTO pipeline_ativo (
          nome,
          reducao_textual,
          chunking,
          representacao_semantica,
          algoritmo,
          k,
          score_global,
          coerencia_umass,
          diversidade,
          estabilidade,
          utilidade_autoral,
          justificativa_escolha,
          data_definicao,
          ativo
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
        """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try {
        try (PreparedStatement stmtDesativar = conexao.prepareStatement(desativarSql)) {
          stmtDesativar.executeUpdate();
        }

        long idGerado;
        try (PreparedStatement stmtInserir = conexao.prepareStatement(inserirSql, Statement.RETURN_GENERATED_KEYS)) {
          stmtInserir.setString(1, definicao.nome());
          stmtInserir.setString(2, definicao.reducao().name());
          stmtInserir.setString(3, definicao.chunking().name());
          stmtInserir.setString(4, definicao.representacao().name());
          stmtInserir.setString(5, definicao.algoritmo().name());
          stmtInserir.setInt(6, definicao.k());
          stmtInserir.setDouble(7, definicao.scoreGlobal());
          stmtInserir.setDouble(8, definicao.coerenciaUmass());
          stmtInserir.setDouble(9, definicao.diversidadeTopicos());
          stmtInserir.setDouble(10, definicao.estabilidadeJaccard());
          stmtInserir.setDouble(11, definicao.utilidadeAutoral());
          stmtInserir.setString(12, definicao.justificativaEscolha());
          stmtInserir.setString(13, definicao.dataDefinicao() != null ? definicao.dataDefinicao().format(DTF) : LocalDateTime.now().format(DTF));
          stmtInserir.executeUpdate();

          try (ResultSet rs = stmtInserir.getGeneratedKeys()) {
            if (rs.next()) {
              idGerado = rs.getLong(1);
            } else {
              idGerado = 1L;
            }
          }
        }

        conexao.commit();
        return idGerado;
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      } finally {
        conexao.setAutoCommit(true);
      }
    }
  }

  public Optional<DefinicaoPipeline> obterPipelineAtivo() throws SQLException {
    String sql = """
        SELECT id, nome, reducao_textual, chunking, representacao_semantica,
               algoritmo, k, score_global, coerencia_umass, diversidade,
               estabilidade, utilidade_autoral, justificativa_escolha,
               data_definicao, ativo
        FROM pipeline_ativo
        WHERE ativo = 1
        ORDER BY id DESC
        LIMIT 1
        """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

      if (rs.next()) {
        return Optional.of(mapearLinha(rs));
      }
    }
    return Optional.empty();
  }

  public List<DefinicaoPipeline> listarHistoricoPipelines() throws SQLException {
    String sql = "SELECT * FROM pipeline_ativo ORDER BY id DESC";
    List<DefinicaoPipeline> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        lista.add(mapearLinha(rs));
      }
    }
    return lista;
  }

  private DefinicaoPipeline mapearLinha(ResultSet rs) throws SQLException {
    LocalDateTime data = null;
    String dataStr = rs.getString("data_definicao");
    if (dataStr != null && !dataStr.isBlank()) {
      try {
        data = LocalDateTime.parse(dataStr, DTF);
      } catch (Exception ignored) {
        data = LocalDateTime.now();
      }
    }

    return new DefinicaoPipeline(
        rs.getLong("id"),
        rs.getString("nome"),
        TipoReducaoTextual.valueOf(rs.getString("reducao_textual")),
        TipoChunking.valueOf(rs.getString("chunking")),
        TipoRepresentacaoSemantica.valueOf(rs.getString("representacao_semantica")),
        TipoAlgoritmoTopicModeling.valueOf(rs.getString("algoritmo")),
        rs.getInt("k"),
        rs.getDouble("score_global"),
        rs.getDouble("coerencia_umass"),
        rs.getDouble("diversidade"),
        rs.getDouble("estabilidade"),
        rs.getDouble("utilidade_autoral"),
        rs.getString("justificativa_escolha"),
        data,
        rs.getInt("ativo") == 1
    );
  }
}
