package org.entryPoint.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RepositorioAutores {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioAutores() {
    this(new GerenciadorConexao());
  }

  public RepositorioAutores(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public long salvarAutor(String nome) throws SQLException {
    String nomeNormalizado = normalizarNome(nome);

    String busca = """
      SELECT id
      FROM autores
      WHERE nome = ?
      """;

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement consultar = conexao.prepareStatement(busca)) {
      consultar.setString(1, nomeNormalizado);

      try (ResultSet resultSet = consultar.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong("id");
        }
      }

      String inserirSql = """
        INSERT INTO autores (nome)
        VALUES (?)
        """;

      try (PreparedStatement inserir = conexao.prepareStatement(inserirSql)) {
        inserir.setString(1, nomeNormalizado);
        inserir.executeUpdate();
      }

      consultar.setString(1, nomeNormalizado);
      try (ResultSet resultSet = consultar.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong("id");
        }
      }
    }

    throw new SQLException("Não foi possível obter o ID do autor: " + nome);
  }

  public void salvarNormaAutor(
    long idNorma,
    long idAutor,
    String cargo
  ) throws SQLException {
    String sql = """
      INSERT INTO norma_autor (
        id_norma,
        id_autor,
        cargo_autor
      )
      VALUES (?, ?, ?)
      ON CONFLICT(id_norma, id_autor) DO UPDATE SET
        cargo_autor = excluded.cargo_autor
      """;

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql)) {
      statement.setLong(1, idNorma);
      statement.setLong(2, idAutor);
      statement.setString(3, cargo.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT));
      statement.executeUpdate();
    }
  }

  public List<String> listarCargosDistintos() throws SQLException {
    String sql = """
      SELECT DISTINCT cargo_autor
      FROM norma_autor
      WHERE cargo_autor IS NOT NULL
        AND TRIM(cargo_autor) <> ''
      ORDER BY cargo_autor
      """;

    List<String> cargos = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        cargos.add(resultSet.getString(1));
      }
    }
    return cargos;
  }

  public List<String> listarCargosPendentesNormalizacao() throws SQLException {
    String sql = """
      SELECT DISTINCT cargo_autor
      FROM norma_autor
      WHERE cargo_autor IS NOT NULL
        AND TRIM(cargo_autor) <> ''
        AND (cargo_autor_normalizado IS NULL OR TRIM(cargo_autor_normalizado) = '')
      ORDER BY cargo_autor
      """;

    List<String> cargos = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        cargos.add(resultSet.getString(1));
      }
    }
    return cargos;
  }

  public int atualizarCargosNormaAutor(Map<String, String> mapaDePara) throws SQLException {
    String sql = """
      UPDATE norma_autor
      SET cargo_autor_normalizado = ?
      WHERE cargo_autor = ?
      """;

    int totalLinhasModificadas = 0;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (Map.Entry<String, String> entry : mapaDePara.entrySet()) {
          String original = entry.getKey();
          String normalizado = entry.getValue();

          if (original == null || normalizado == null) {
            continue;
          }

          statement.setString(1, normalizado);
          statement.setString(2, original);
          statement.addBatch();
        }

        int[] resultados = statement.executeBatch();
        for (int contagem : resultados) {
          if (contagem > 0) {
            totalLinhasModificadas += contagem;
          }
        }

        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      } finally {
        conexao.setAutoCommit(true);
      }
    }

    return totalLinhasModificadas;
  }

  public Map<Long, List<Long>> listarAutoresComNormas() throws SQLException {
    String sql = "SELECT id_autor, id_norma FROM norma_autor WHERE ativo = 1 ORDER BY id_autor";
    Map<Long, List<Long>> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long idAutor = resultSet.getLong("id_autor");
        long idNorma = resultSet.getLong("id_norma");
        mapa.computeIfAbsent(idAutor, k -> new ArrayList<>()).add(idNorma);
      }
    }
    return mapa;
  }

  public Map<Long, String> listarNomesAutores() throws SQLException {
    String sql = "SELECT id, nome FROM autores";
    Map<Long, String> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        mapa.put(resultSet.getLong("id"), resultSet.getString("nome"));
      }
    }
    return mapa;
  }

  public Map<Long, String> listarCargosNormalizadosAutores() throws SQLException {
    String sql = "SELECT DISTINCT id_autor, cargo_autor_normalizado FROM norma_autor WHERE cargo_autor_normalizado IS NOT NULL";
    Map<Long, String> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        mapa.put(resultSet.getLong("id_autor"), resultSet.getString("cargo_autor_normalizado"));
      }
    }
    return mapa;
  }

  private String normalizarNome(String nome) {
    return nome
      .trim()
      .replaceAll("\\s+", " ")
      .toUpperCase(Locale.ROOT);
  }
}
