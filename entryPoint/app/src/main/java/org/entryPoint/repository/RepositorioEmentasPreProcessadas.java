package org.entryPoint.repository;

import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.EmentaPreProcessada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RepositorioEmentasPreProcessadas {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioEmentasPreProcessadas() {
    this(new GerenciadorConexao());
  }

  public RepositorioEmentasPreProcessadas(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
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

    try (Connection conexao = gerenciadorConexao.conectar()) {
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
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    return 0;
  }

  public long contarEmbeddings() throws SQLException {
    String sql = "SELECT COUNT(*) FROM ementas_pre_processadas WHERE embeddings IS NOT NULL AND TRIM(embeddings) <> ''";
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    return 0;
  }

  public void salvarEmbeddingsEmLote(Map<Long, String> mapaEmbeddings) throws SQLException {
    if (mapaEmbeddings == null || mapaEmbeddings.isEmpty()) return;

    String sql = """
      INSERT INTO ementas_pre_processadas (id_norma, embeddings)
      VALUES (?, ?)
      ON CONFLICT(id_norma) DO UPDATE SET embeddings = excluded.embeddings
      """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try (PreparedStatement statement = conexao.prepareStatement(sql)) {
        for (Map.Entry<Long, String> entry : mapaEmbeddings.entrySet()) {
          statement.setLong(1, entry.getKey());
          statement.setString(2, entry.getValue());
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

  public List<org.entryPoint.model.NormaComEmenta> listarNormasParaGeracaoEmbedding(boolean apenasPendentes, int limite) throws SQLException {
    StringBuilder sql = new StringBuilder();
    if (apenasPendentes) {
      sql.append("""
        SELECT n.id, n.ementa, n.tipoEscrito
        FROM normas n
        LEFT JOIN ementas_pre_processadas e ON n.id = e.id_norma
        WHERE n.ementa IS NOT NULL AND TRIM(n.ementa) <> ''
          AND (e.embeddings IS NULL OR TRIM(e.embeddings) = '')
        ORDER BY n.id
      """);
    } else {
      sql.append("""
        SELECT n.id, n.ementa, n.tipoEscrito
        FROM normas n
        WHERE n.ementa IS NOT NULL AND TRIM(n.ementa) <> ''
        ORDER BY n.id
      """);
    }

    if (limite > 0) {
      sql.append(" LIMIT ").append(limite);
    }

    List<org.entryPoint.model.NormaComEmenta> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql.toString());
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        lista.add(new org.entryPoint.model.NormaComEmenta(
          resultSet.getLong("id"),
          resultSet.getString("ementa"),
          resultSet.getString("tipoEscrito")
        ));
      }
    }
    return lista;
  }

  public List<org.entryPoint.model.EmbeddingNorma> listarEmbeddingsParaClusterizacao() throws SQLException {
    String sql = """
      SELECT n.id, n.numero, n.ano, n.tipoEscrito, n.titulo, n.ementa, e.embeddings
      FROM normas n
      JOIN ementas_pre_processadas e ON n.id = e.id_norma
      WHERE e.embeddings IS NOT NULL AND TRIM(e.embeddings) <> ''
      ORDER BY n.id
    """;

    List<org.entryPoint.model.EmbeddingNorma> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        String jsonEmb = resultSet.getString("embeddings");
        float[] vetor = parseJsonFloatArray(jsonEmb);
        if (vetor != null && vetor.length > 0) {
          lista.add(new org.entryPoint.model.EmbeddingNorma(
            resultSet.getLong("id"),
            vetor,
            resultSet.getString("ementa"),
            resultSet.getString("titulo"),
            resultSet.getInt("numero"),
            resultSet.getInt("ano"),
            resultSet.getString("tipoEscrito")
          ));
        }
      }
    }
    return lista;
  }

  public static float[] parseJsonFloatArray(String json) {
    if (json == null || json.isBlank()) return null;
    String s = json.trim();
    if (s.startsWith("[")) s = s.substring(1);
    if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
    s = s.trim();
    if (s.isEmpty()) return new float[0];
    String[] tokens = s.split(",");
    float[] res = new float[tokens.length];
    for (int i = 0; i < tokens.length; i++) {
      try {
        res[i] = Float.parseFloat(tokens[i].trim());
      } catch (NumberFormatException e) {
        res[i] = 0.0f;
      }
    }
    return res;
  }

  public static String formatFloatArrayToJson(float[] vector) {
    if (vector == null) return "[]";
    StringBuilder sb = new StringBuilder(vector.length * 9 + 2);
    sb.append('[');
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(Float.toString(vector[i]));
    }
    sb.append(']');
    return sb.toString();
  }

  public List<EmentaParaTopico> listarEmentasParaTopicos() throws SQLException {
    String sql = """
      SELECT n.id, 
             COALESCE(e.stopwords_tfidf_topk, '') || ' ' || COALESCE(e.svd_lsa, '') AS termos
      FROM normas n
      JOIN ementas_pre_processadas e ON n.id = e.id_norma
      WHERE (e.stopwords_tfidf_topk IS NOT NULL AND TRIM(e.stopwords_tfidf_topk) != '')
         OR (e.svd_lsa IS NOT NULL AND TRIM(e.svd_lsa) != '')
    """;
    List<EmentaParaTopico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
      PreparedStatement statement = conexao.prepareStatement(sql);
      ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        lista.add(new EmentaParaTopico(resultSet.getLong("id"), resultSet.getString("termos")));
      }
    }
    return lista;
  }
}
