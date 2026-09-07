package org.entryPoint.repository;

import org.entryPoint.model.Norma;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RepositorioBancoDados {

  private final String CAMINHO_BANCO_DADOS = "../../data/leis.db";

  private final String URL_BANCO_DADOS = "jdbc:sqlite:" + CAMINHO_BANCO_DADOS;

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
    
    try {
      Connection conexao = conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery();
      
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
  
  private void criarDiretorioBancoDados() {
    File arquivoBancoDados = new File(CAMINHO_BANCO_DADOS);

    File diretorioPai = arquivoBancoDados.getParentFile();

    if (diretorioPai != null) {
      diretorioPai.mkdirs();
    }
  }

  private Connection conectar() throws SQLException {
    return DriverManager.getConnection(URL_BANCO_DADOS);
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
        nome TEXT 
      )
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
      try (PreparedStatement statement = conexao.prepareStatement(createNormas)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(createAutores)) {
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


}