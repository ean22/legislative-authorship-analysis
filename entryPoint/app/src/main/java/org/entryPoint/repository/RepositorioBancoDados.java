package org.entryPoint.repository;

import org.entryPoint.model.Norma;
import org.entryPoint.model.EmentaPreProcessada;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        cargo_autor_normalizado TEXT,
        ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
        PRIMARY KEY (id_norma, id_autor),
        FOREIGN KEY (id_norma) REFERENCES normas(id),
        FOREIGN KEY (id_autor) REFERENCES autores(id)
      )
    """;

    String createEmentasPreProcessadas = """
      CREATE TABLE IF NOT EXISTS ementas_pre_processadas (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        id_norma INTEGER UNIQUE,
        stopwords_tfidf_topk TEXT,
        tfidf_topk TEXT,
        resumo_extrativo_tfidf TEXT,
        textrank TEXT,
        tfidf_posicao_sentenca TEXT,
        tfidf_mmr TEXT,
        tfidf_similaridade_frases TEXT,
        svd_lsa TEXT,
        tfidf_feature_selection TEXT,
        chi_square_termos TEXT,
        mutual_information TEXT,
        clustering_sentencas TEXT,
        FOREIGN KEY (id_norma) REFERENCES normas(id)
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

      try (PreparedStatement statement = conexao.prepareStatement(createEmentasPreProcessadas)) {
        statement.execute();
      }

      try (var stmt = conexao.createStatement()) {
        stmt.execute("ALTER TABLE norma_autor ADD COLUMN cargo_autor_normalizado TEXT");
      } catch (SQLException ignored) {
        // Coluna já existe
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

  public List<String> listarCargosDistintos() throws SQLException {
    String sql = """
      SELECT DISTINCT cargo_autor
      FROM norma_autor
      WHERE cargo_autor IS NOT NULL
        AND TRIM(cargo_autor) <> ''
      ORDER BY cargo_autor
      """;

    List<String> cargos = new ArrayList<>();
    try (Connection conexao = conectar();
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
    try (Connection conexao = conectar();
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

    try (Connection conexao = conectar()) {
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

  public List<NormaComEmenta> listarNormasComEmenta() throws SQLException {
    String sql = """
      SELECT id, ementa, tipoEscrito
      FROM normas
      WHERE ementa IS NOT NULL
        AND TRIM(ementa) <> ''
      ORDER BY id
      """;

    List<NormaComEmenta> lista = new ArrayList<>();
    try (Connection conexao = conectar();
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

  public void salvarEmentasPreProcessadasEmLote(List<EmentaPreProcessada> lote) throws SQLException {
    String sql = """
      INSERT INTO ementas_pre_processadas (
        id_norma,
        stopwords_tfidf_topk,
        tfidf_topk,
        resumo_extrativo_tfidf,
        textrank,
        tfidf_posicao_sentenca,
        tfidf_mmr,
        tfidf_similaridade_frases,
        svd_lsa,
        tfidf_feature_selection,
        chi_square_termos,
        mutual_information,
        clustering_sentencas
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id_norma) DO UPDATE SET
        stopwords_tfidf_topk = excluded.stopwords_tfidf_topk,
        tfidf_topk = excluded.tfidf_topk,
        resumo_extrativo_tfidf = excluded.resumo_extrativo_tfidf,
        textrank = excluded.textrank,
        tfidf_posicao_sentenca = excluded.tfidf_posicao_sentenca,
        tfidf_mmr = excluded.tfidf_mmr,
        tfidf_similaridade_frases = excluded.tfidf_similaridade_frases,
        svd_lsa = excluded.svd_lsa,
        tfidf_feature_selection = excluded.tfidf_feature_selection,
        chi_square_termos = excluded.chi_square_termos,
        mutual_information = excluded.mutual_information,
        clustering_sentencas = excluded.clustering_sentencas
      """;

    try (Connection conexao = conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (EmentaPreProcessada item : lote) {
          statement.setLong(1, item.getIdNorma());
          statement.setString(2, item.getStopwordsTfidfTopk());
          statement.setString(3, item.getTfidfTopk());
          statement.setString(4, item.getResumoExtrativoTfidf());
          statement.setString(5, item.getTextrank());
          statement.setString(6, item.getTfidfPosicaoSentenca());
          statement.setString(7, item.getTfidfMmr());
          statement.setString(8, item.getTfidfSimilaridadeFrases());
          statement.setString(9, item.getSvdLsa());
          statement.setString(10, item.getTfidfFeatureSelection());
          statement.setString(11, item.getChiSquareTermos());
          statement.setString(12, item.getMutualInformation());
          statement.setString(13, item.getClusteringSentencas());
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

  public long contarEmentasPreProcessadas() throws SQLException {
    String sql = "SELECT COUNT(*) FROM ementas_pre_processadas";
    try (Connection conexao = conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    return 0;
  }

  public record NormaComIntegra(long id, String integra) {}
  public record NormaComEmenta(long id, String ementa, String tipoEscrito) {}

}