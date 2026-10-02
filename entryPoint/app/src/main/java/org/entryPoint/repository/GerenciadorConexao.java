package org.entryPoint.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GerenciadorConexao {
  private static final String PATH_BANCO = "../../data/leis.db";
  private static final String URL_BANCO_DADOS = "jdbc:sqlite:" + PATH_BANCO;
  private static volatile boolean inicializado = false;

  public GerenciadorConexao() {
    inicializar();
  }

  public synchronized void inicializar() {
    if (!inicializado) {
      criarDiretorioBancoDados();
      criarTabelas();
      inicializado = true;
    }
  }

  public Connection conectar() throws SQLException {
    Connection conexao = DriverManager.getConnection(URL_BANCO_DADOS);
    try (var statement = conexao.createStatement()) {
      statement.execute("PRAGMA busy_timeout = 5000");
    }
    return conexao;
  }

  private void criarDiretorioBancoDados() {
    File arquivoBancoDados = new File(PATH_BANCO);
    File diretorioPai = arquivoBancoDados.getParentFile();
    if (diretorioPai != null) {
      diretorioPai.mkdirs();
    }
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
        embeddings TEXT,
        FOREIGN KEY (id_norma) REFERENCES normas(id)
      )
    """;

    String createRelacionamentosNormas = """
      CREATE TABLE IF NOT EXISTS norma_relacionamentos (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        id_norma_origem INTEGER NOT NULL,
        id_norma_destino INTEGER,
        tipo_relacionamento TEXT NOT NULL,
        url_alvo TEXT,
        tipo_slug_alvo TEXT,
        ano_alvo INTEGER,
        numero_alvo INTEGER,
        trecho_contexto TEXT,
        FOREIGN KEY (id_norma_origem) REFERENCES normas(id),
        FOREIGN KEY (id_norma_destino) REFERENCES normas(id)
      )
    """;

    String criarIndiceRelOrigem = """
      CREATE INDEX IF NOT EXISTS idx_norma_rel_origem
      ON norma_relacionamentos (id_norma_origem)
    """;

    String criarIndiceRelDestino = """
      CREATE INDEX IF NOT EXISTS idx_norma_rel_destino
      ON norma_relacionamentos (id_norma_destino)
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

      try (PreparedStatement statement = conexao.prepareStatement(createRelacionamentosNormas)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(criarIndiceRelOrigem)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(criarIndiceRelDestino)) {
        statement.execute();
      }

      String createRotulosTopicos = """
        CREATE TABLE IF NOT EXISTS rotulos_topicos (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          nome TEXT UNIQUE NOT NULL
        )
      """;

      String createTopicos = """
        CREATE TABLE IF NOT EXISTS topicos (
          id INTEGER PRIMARY KEY,
          id_rotulo_topico INTEGER NOT NULL,
          termos_principais TEXT,
          total_normas INTEGER,
          percentual_base REAL,
          FOREIGN KEY (id_rotulo_topico) REFERENCES rotulos_topicos(id)
        )
      """;

      try (PreparedStatement statement = conexao.prepareStatement(createRotulosTopicos)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(createTopicos)) {
        statement.execute();
      }

      // Migração/criação da tabela norma_topicos com id_rotulo_topico
      try (var stmt = conexao.createStatement()) {
        var rs = stmt.executeQuery("PRAGMA table_info(norma_topicos)");
        boolean temRotuloTexto = false;
        while (rs.next()) {
          if ("rotulo_topico".equalsIgnoreCase(rs.getString("name"))) {
            temRotuloTexto = true;
            break;
          }
        }
        if (temRotuloTexto) {
          stmt.execute("DROP TABLE norma_topicos");
        }
      } catch (SQLException ignored) {}

      String createNormaTopicos = """
        CREATE TABLE IF NOT EXISTS norma_topicos (
          id_norma INTEGER PRIMARY KEY,
          id_topico INTEGER NOT NULL,
          id_rotulo_topico INTEGER NOT NULL,
          score_pertinencia REAL,
          termos_chave TEXT,
          FOREIGN KEY (id_norma) REFERENCES normas(id),
          FOREIGN KEY (id_rotulo_topico) REFERENCES rotulos_topicos(id)
        )
      """;

      String criarIndiceNormaTopico = """
        CREATE INDEX IF NOT EXISTS idx_norma_topicos_topico
        ON norma_topicos (id_topico)
      """;

      String criarIndiceNormaRotulo = """
        CREATE INDEX IF NOT EXISTS idx_norma_topicos_rotulo
        ON norma_topicos (id_rotulo_topico)
      """;

      try (PreparedStatement statement = conexao.prepareStatement(createNormaTopicos)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(criarIndiceNormaTopico)) {
        statement.execute();
      }

      try (PreparedStatement statement = conexao.prepareStatement(criarIndiceNormaRotulo)) {
        statement.execute();
      }

      try (var stmt = conexao.createStatement()) {
        stmt.execute("ALTER TABLE norma_autor ADD COLUMN cargo_autor_normalizado TEXT");
      } catch (SQLException ignored) {
        // Coluna já existe
      }

      try (var stmt = conexao.createStatement()) {
        stmt.execute("ALTER TABLE norma_autor ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1");
      } catch (SQLException ignored) {
        // Coluna já existe
      }

      try (var stmt = conexao.createStatement()) {
        stmt.execute("ALTER TABLE ementas_pre_processadas ADD COLUMN embeddings TEXT");
      } catch (SQLException ignored) {
        // Coluna já existe
      }

    } catch (SQLException e) {
      throw new RuntimeException("Erro ao criar tabelas", e);
    }
  }
}
