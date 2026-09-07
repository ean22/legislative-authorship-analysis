package org.entryPoint.repository;

import org.entryPoint.model.Law;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseRepository {

  private static final String DATABASE_PATH = "../../data/leis.db";

  private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH;

  public DatabaseRepository() {
    createDatabaseDirectory();
    createTables();
  }

  public void saveRawLaw(Law law) {

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

    try (Connection connection = connect();
      PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setLong(1, law.getId());
        statement.setInt(2, law.getNumero());
        statement.setInt(3, law.getAno());
        statement.setString(4, law.getTipoEscrito());
        statement.setString(5, law.getTipoSlug());
        statement.setString(6, law.getTitulo());
        statement.setString(7, law.getEmenta());
        statement.setString(8, law.getDataOriginal());
        statement.setString(9, law.getDataPublicacao());
        statement.setString(10, law.getUrl());
        statement.setString(11, law.getIntegra());
        statement.setString(12, law.getCidade());
        statement.setString(13, law.getEstado());

        statement.executeUpdate();

    } catch (SQLException e) {
      System.err.println(
        "Erro ao salvar norma " + law.getId() +
        ": " + e.getMessage() + "\n" +
        e
      );
    }
  }

  public void listLaws() {
    String sql = 
    """
      SELECT * FROM normas
      LIMIT 10;
    """;
    
    try {
      Connection connection = connect();
      PreparedStatement statement = connection.prepareStatement(sql);
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
  
  private void createDatabaseDirectory() {
    File databaseFile = new File(DATABASE_PATH);

    File parent = databaseFile.getParentFile();

    if (parent != null) {
      parent.mkdirs();
    }
  }

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(DATABASE_URL);
  }

  private void createTables() {

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

    try (Connection connection = connect()) {
      try (PreparedStatement statement = connection.prepareStatement(createNormas)) {
        statement.execute();
      }

      try (PreparedStatement statement = connection.prepareStatement(createAutores)) {
        statement.execute();
      }

      try (PreparedStatement statement = connection.prepareStatement(createNormaAutor)) {
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