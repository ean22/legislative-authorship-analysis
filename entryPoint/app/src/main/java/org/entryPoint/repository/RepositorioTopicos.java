package org.entryPoint.repository;

import org.entryPoint.model.NormaTopico;
import org.entryPoint.model.TopicoTematico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositorioTopicos {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioTopicos() {
    this(new GerenciadorConexao());
  }

  public RepositorioTopicos(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public int obterOuCriarIdRotuloTopico(String nomeRotulo) throws SQLException {
    String sqlSelect = "SELECT id FROM rotulos_topicos WHERE nome = ?";
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sqlSelect)) {
      statement.setString(1, nomeRotulo);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("id");
        }
      }
    }

    String sqlInsert = "INSERT OR IGNORE INTO rotulos_topicos (nome) VALUES (?)";
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sqlInsert)) {
      statement.setString(1, nomeRotulo);
      statement.executeUpdate();
    }

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sqlSelect)) {
      statement.setString(1, nomeRotulo);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("id");
        }
      }
    }
    return 1;
  }

  public Map<String, Integer> mapearRotulosParaIds() throws SQLException {
    String sql = "SELECT id, nome FROM rotulos_topicos";
    Map<String, Integer> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        mapa.put(rs.getString("nome"), rs.getInt("id"));
      }
    }
    return mapa;
  }

  public void salvarTopicosDescobertosEmLote(List<TopicoTematico> topicos) throws SQLException {
    String sql = """
      INSERT OR REPLACE INTO topicos (
        id,
        id_rotulo_topico,
        termos_principais,
        total_normas,
        percentual_base
      )
      VALUES (?, ?, ?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (TopicoTematico item : topicos) {
          statement.setInt(1, item.id());
          statement.setInt(2, item.idRotuloTopico());
          statement.setString(3, String.join(", ", item.termosPrincipais()));
          statement.setLong(4, item.totalNormas());
          statement.setDouble(5, item.percentualBase());
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

  public void salvarTopicosNormasEmLote(List<NormaTopico> lote) throws SQLException {
    String sql = """
      INSERT OR REPLACE INTO norma_topicos (
        id_norma,
        id_topico,
        id_rotulo_topico,
        score_pertinencia,
        termos_chave
      )
      VALUES (?, ?, ?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (NormaTopico item : lote) {
          statement.setLong(1, item.idNorma());
          statement.setInt(2, item.idTopico());
          statement.setInt(3, item.idRotuloTopico());
          statement.setDouble(4, item.scorePertinencia());
          statement.setString(5, item.termosChave());
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

  public List<NormaTopico> listarNormasTopicos() throws SQLException {
    String sql = """
      SELECT nt.id_norma, nt.id_topico, nt.id_rotulo_topico, rt.nome AS rotulo_topico, nt.score_pertinencia, nt.termos_chave
      FROM norma_topicos nt
      JOIN rotulos_topicos rt ON nt.id_rotulo_topico = rt.id
    """;
    List<NormaTopico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        lista.add(new NormaTopico(
          resultSet.getLong("id_norma"),
          resultSet.getInt("id_topico"),
          resultSet.getInt("id_rotulo_topico"),
          resultSet.getString("rotulo_topico"),
          resultSet.getDouble("score_pertinencia"),
          resultSet.getString("termos_chave")
        ));
      }
    }
    return lista;
  }

  public Map<Long, NormaTopico> listarNormasTopicosComoMapa() throws SQLException {
    String sql = """
      SELECT nt.id_norma, nt.id_topico, nt.id_rotulo_topico, rt.nome AS rotulo_topico, nt.score_pertinencia, nt.termos_chave
      FROM norma_topicos nt
      JOIN rotulos_topicos rt ON nt.id_rotulo_topico = rt.id
    """;
    Map<Long, NormaTopico> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        mapa.put(resultSet.getLong("id_norma"), new NormaTopico(
          resultSet.getLong("id_norma"),
          resultSet.getInt("id_topico"),
          resultSet.getInt("id_rotulo_topico"),
          resultSet.getString("rotulo_topico"),
          resultSet.getDouble("score_pertinencia"),
          resultSet.getString("termos_chave")
        ));
      }
    }
    return mapa;
  }

  public List<TopicoTematico> listarTopicosSalvos() throws SQLException {
    String sql = """
      SELECT t.id, t.id_rotulo_topico, rt.nome AS rotulo, t.termos_principais, t.total_normas, t.percentual_base
      FROM topicos t
      JOIN rotulos_topicos rt ON t.id_rotulo_topico = rt.id
      ORDER BY t.id
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
          rs.getInt("id_rotulo_topico"),
          rs.getString("rotulo"),
          termosList,
          rs.getLong("total_normas"),
          rs.getDouble("percentual_base")
        ));
      }
    }
    return lista;
  }
}
