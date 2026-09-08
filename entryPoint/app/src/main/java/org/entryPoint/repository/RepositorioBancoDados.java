package org.entryPoint.repository;

import org.entryPoint.model.Norma;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RepositorioBancoDados {
  private final String PATH_BANCO = "../../data/leis.db";
  private final String URL_BANCO_DADOS = "jdbc:sqlite:" + PATH_BANCO;

  public RepositorioBancoDados() {
    criarDiretorioBancoDados();
    criarTabelas();
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

    try (Connection conexao = conectar();
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
    String sql = 
    """
      SELECT * FROM normas
      LIMIT 10;
    """;
    
    try (Connection conexao = conectar();
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

    try (Connection conexao = conectar();
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

  public long salvarAutor(String nome) throws SQLException {
    String nomeNormalizado = normalizarNome(nome);

    String busca = """
      SELECT id
      FROM autores
      WHERE nome = ?
      """;

    try (Connection conexao = conectar();
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

    try (Connection conexao = conectar();
      PreparedStatement statement = conexao.prepareStatement(sql)) {
      statement.setLong(1, idNorma);
      statement.setLong(2, idAutor);
      statement.setString(3, cargo.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT));
      statement.executeUpdate();
    }
  }
  
  private void criarDiretorioBancoDados() {
    File arquivoBancoDados = new File(PATH_BANCO);

    File diretorioPai = arquivoBancoDados.getParentFile();

    if (diretorioPai != null) {
      diretorioPai.mkdirs();
    }
  }

  private Connection conectar() throws SQLException {
    Connection conexao = DriverManager.getConnection(URL_BANCO_DADOS);
    try (var statement = conexao.createStatement()) {
      statement.execute("PRAGMA busy_timeout = 5000");
    }
    return conexao;
  }

  private void criarTabelas() {

    String createNormas = """
      CREATE TABLE IF NOT EXISTS normas (
        id INTEGER PRIMARY KEY,
        numero INTEGER,
        ano INTEGER,
        tipoEscrito TEXT,
        tipoSlug TEXT,
        titulo TEXT,
        ementa TEXT,
        dataOriginal TEXT,
        dataPublicacao TEXT,
        url TEXT,
        integra TEXT,
        cidade TEXT,
        estado TEXT
      )
    """;

    String createAutores = """
      CREATE TABLE IF NOT EXISTS autores (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nome TEXT UNIQUE
      )
      """;

    String criarIndiceNomeAutor = """
      CREATE UNIQUE INDEX IF NOT EXISTS indice_autores_nome
      ON autores (nome)
      """;

    String createNormaAutor = """
      CREATE TABLE IF NOT EXISTS norma_autor (
        id_norma INTEGER,
        id_autor INTEGER,
        cargo_autor TEXT,
        PRIMARY KEY (id_norma, id_autor),
        FOREIGN KEY (id_norma) REFERENCES normas(id),
        FOREIGN KEY (id_autor) REFERENCES autores(id)
      )
    """;

    try (Connection conexao = conectar()) {
      try (var statement = conexao.createStatement()) {
        statement.execute("PRAGMA journal_mode = WAL");
      }

      try (PreparedStatement statement = conexao.prepareStatement(createNormas)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(createAutores)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(criarIndiceNomeAutor)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(createNormaAutor)) {
        statement.execute();
      }

      
    } catch (SQLException e) {
      throw new RuntimeException(
        "Erro ao criar tabela normas",
        e
      );
    }
  }

  private String normalizarNome(String nome) {
    return nome
      .trim()
      .replaceAll("\\s+", " ")
      .toUpperCase(Locale.ROOT);
  }

  public record NormaComIntegra(long id, String integra) {}


}