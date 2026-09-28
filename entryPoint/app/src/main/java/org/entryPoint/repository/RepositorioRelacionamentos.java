package org.entryPoint.repository;

import org.entryPoint.model.RelacionamentoNorma;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class RepositorioRelacionamentos {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioRelacionamentos() {
    this(new GerenciadorConexao());
  }

  public RepositorioRelacionamentos(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public void salvarRelacionamentosEmLote(List<RelacionamentoNorma> lote) throws SQLException {
    String sql = """
      INSERT INTO norma_relacionamentos (
        id_norma_origem,
        id_norma_destino,
        tipo_relacionamento,
        url_alvo,
        tipo_slug_alvo,
        ano_alvo,
        numero_alvo,
        trecho_contexto
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (RelacionamentoNorma item : lote) {
          statement.setLong(1, item.idNormaOrigem());
          if (item.idNormaDestino() != null) {
            statement.setLong(2, item.idNormaDestino());
          } else {
            statement.setNull(2, Types.INTEGER);
          }
          statement.setString(3, item.tipoRelacionamento());
          statement.setString(4, item.urlAlvo());
          statement.setString(5, item.tipoSlugAlvo());
          if (item.anoAlvo() != null) {
            statement.setInt(6, item.anoAlvo());
          } else {
            statement.setNull(6, Types.INTEGER);
          }
          if (item.numeroAlvo() != null) {
            statement.setInt(7, item.numeroAlvo());
          } else {
            statement.setNull(7, Types.INTEGER);
          }
          statement.setString(8, item.trechoContexto());
          statement.addBatch();
        }
        statement.executeBatch();
        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      } finally {
        conexao.setAutoCommit(true);
      }
    }
  }

  public long contarRelacionamentos() throws SQLException {
    String sql = "SELECT COUNT(*) FROM norma_relacionamentos";
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    return 0;
  }

  public void limparRelacionamentos() throws SQLException {
    String sql = "DELETE FROM norma_relacionamentos";
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }
}
