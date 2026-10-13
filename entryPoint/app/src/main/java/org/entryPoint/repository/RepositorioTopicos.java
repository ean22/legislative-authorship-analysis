package org.entryPoint.repository;

import org.entryPoint.model.TopicoTematico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepositorioTopicos {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioTopicos() {
    this(new GerenciadorConexao());
  }

  public RepositorioTopicos(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public void salvarTopicosDescobertosEmLote(List<TopicoTematico> topicos) throws SQLException {
    String sql = """
      INSERT OR REPLACE INTO topicos (
        id,
        nome,
        termos_principais
      )
      VALUES (?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (TopicoTematico item : topicos) {
          statement.setInt(1, item.id());
          statement.setString(2, item.nome());
          statement.setString(3, String.join(", ", item.termosPrincipais()));
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

  public List<TopicoTematico> listarTopicosSalvos() throws SQLException {
    String sql = """
      SELECT id, COALESCE(nome, 'Tópico #' || id) AS nome, termos_principais
      FROM topicos
      ORDER BY id
    """;
    List<TopicoTematico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        String tp = rs.getString("termos_principais");
        List<String> termosList = tp != null ? List.of(tp.split(",\\s*")) : List.of();
        lista.add(new TopicoTematico(
          rs.getInt("id"),
          rs.getString("nome"),
          termosList
        ));
      }
    }
    return lista;
  }
}
