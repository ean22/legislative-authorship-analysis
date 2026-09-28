package org.entryPoint.repository;

import org.entryPoint.model.Norma;
import org.entryPoint.model.NormaComEmenta;
import org.entryPoint.model.NormaComIntegra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepositorioNormas {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioNormas() {
    this(new GerenciadorConexao());
  }

  public RepositorioNormas(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public void salvarNorma(Norma norma) {
    String sql = """
      INSERT OR REPLACE INTO normas (
        id,
        numero,
        ano,
        tipoEscrito,
        tipoSlug,
        titulo,
        ementa,
        dataOriginal,
        dataPublicacao,
        url,
        integra,
        cidade,
        estado
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql)) {

        statement.setLong(1, norma.getId());
        statement.setInt(2, norma.getNumero());
        statement.setInt(3, norma.getAno());
        statement.setString(4, norma.getTipoEscrito());
        statement.setString(5, norma.getTipoSlug());
        statement.setString(6, norma.getTitulo());
        statement.setString(7, norma.getEmenta());
        statement.setString(8, norma.getDataOriginal());
        statement.setString(9, norma.getDataPublicacao());
        statement.setString(10, norma.getUrl());
        statement.setString(11, norma.getIntegra());
        statement.setString(12, norma.getCidade());
        statement.setString(13, norma.getEstado());

        statement.executeUpdate();

    } catch (SQLException e) {
      System.err.println(
        "Erro ao salvar norma " + norma.getId() +
        ": " + e.getMessage() + "\n" +
        e
      );
    }
  }

  public void listarNormas() {
    String sql = """
      SELECT * FROM normas
      LIMIT 10;
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {

      System.out.println("Listando normas:");

      while (resultSet.next()) {
        System.out.println(
          "ID: " + resultSet.getLong("id") +
          ", Título: " + resultSet.getString("titulo") +
          ", Ementa: " + resultSet.getString("ementa") +
          ", Data Original: " + resultSet.getString("dataOriginal")
        );
      }

    } catch (SQLException e) {
      System.err.println(
        "Erro ao listar normas: " + e.getMessage() + "\n" + e
      );
    }
  }

  public List<NormaComIntegra> listarNormasComIntegra() throws SQLException {
    String sql = """
      SELECT id, integra
      FROM normas
      WHERE integra IS NOT NULL
        AND TRIM(integra) <> ''
      """;

    List<NormaComIntegra> normas = new ArrayList<>();

    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        normas.add(new NormaComIntegra(
          resultSet.getLong("id"),
          resultSet.getString("integra")
        ));
      }
    }

    return normas;
  }

  public List<NormaComEmenta> listarNormasComEmenta() throws SQLException {
    String sql = """
      SELECT id, ementa, tipoEscrito
      FROM normas
      WHERE ementa IS NOT NULL
        AND TRIM(ementa) <> ''
      ORDER BY id
      """;

    List<NormaComEmenta> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {

      while (resultSet.next()) {
        lista.add(new NormaComEmenta(
          resultSet.getLong("id"),
          resultSet.getString("ementa"),
          resultSet.getString("tipoEscrito")
        ));
      }
    }
    return lista;
  }

  public Set<Long> listarTodosIdsNormas() throws SQLException {
    String sql = "SELECT id FROM normas";
    Set<Long> ids = new HashSet<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        ids.add(resultSet.getLong("id"));
      }
    }
    return ids;
  }

  public Map<String, Long> mapearChavesNormasParaId() throws SQLException {
    String sql = "SELECT id, tipoSlug, ano, numero FROM normas WHERE tipoSlug IS NOT NULL AND ano IS NOT NULL AND numero IS NOT NULL";
    Map<String, Long> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        long id = resultSet.getLong("id");
        String tipoSlug = resultSet.getString("tipoSlug");
        int ano = resultSet.getInt("ano");
        int numero = resultSet.getInt("numero");
        String chave = (tipoSlug + ":" + ano + ":" + numero).toLowerCase();
        mapa.put(chave, id);
      }
    }
    return mapa;
  }

  public Map<Long, String> listarEmentasOriginais() throws SQLException {
    String sql = "SELECT id, ementa FROM normas WHERE ementa IS NOT NULL";
    Map<Long, String> mapa = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        mapa.put(resultSet.getLong("id"), resultSet.getString("ementa"));
      }
    }
    return mapa;
  }
}
