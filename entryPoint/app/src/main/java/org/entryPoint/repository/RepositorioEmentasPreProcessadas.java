package org.entryPoint.repository;

import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.EmentaPreProcessada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        texto_reduzido,
        textrank,
        stopwords_tfidf_topk,
        tfidf_topk,
        resumo_extrativo_tfidf,
        tfidf_posicao_sentenca,
        tfidf_mmr,
        tfidf_similaridade_frases,
        svd_lsa,
        tfidf_feature_selection,
        chi_square_termos,
        mutual_information,
        clustering_sentencas
      )
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id_norma) DO UPDATE SET
        texto_reduzido = excluded.texto_reduzido,
        textrank = excluded.textrank,
        stopwords_tfidf_topk = excluded.stopwords_tfidf_topk,
        tfidf_topk = excluded.tfidf_topk,
        resumo_extrativo_tfidf = excluded.resumo_extrativo_tfidf,
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
          String red = item.getTextoReduzido() != null ? item.getTextoReduzido() : item.getTextrank();
          statement.setLong(1, item.getIdNorma());
          statement.setString(2, red);
          statement.setString(3, item.getTextrank() != null ? item.getTextrank() : red);
          statement.setString(4, item.getStopwordsTfidfTopk());
          statement.setString(5, item.getTfidfTopk());
          statement.setString(6, item.getResumoExtrativoTfidf());
          statement.setString(7, item.getTfidfPosicaoSentenca());
          statement.setString(8, item.getTfidfMmr());
          statement.setString(9, item.getTfidfSimilaridadeFrases());
          statement.setString(10, item.getSvdLsa());
          statement.setString(11, item.getTfidfFeatureSelection());
          statement.setString(12, item.getChiSquareTermos());
          statement.setString(13, item.getMutualInformation());
          statement.setString(14, item.getClusteringSentencas());
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

  public List<EmentaParaTopico> listarEmentasParaTopicos() throws SQLException {
    // Utiliza a redução textual do pipeline vencedor (TextRank / texto_reduzido) com fallback para ementa original
    String sql = """
      SELECT n.id, 
             COALESCE(e.texto_reduzido, e.textrank, n.ementa, '') AS termos
      FROM normas n
      LEFT JOIN ementas_pre_processadas e ON n.id = e.id_norma
      WHERE (e.texto_reduzido IS NOT NULL AND TRIM(e.texto_reduzido) != '')
         OR (e.textrank IS NOT NULL AND TRIM(e.textrank) != '')
         OR (n.ementa IS NOT NULL AND TRIM(n.ementa) != '')
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

  public List<EmentaParaTopico> listarEmentasPorEstrategia(
      org.entryPoint.benchmark.model.TipoReducaoTextual tipo,
      int limite
  ) throws SQLException {
    String coluna = tipo.getColunaSql();
    String sql;

    if (tipo.isVemDaTabelaNormas()) {
      sql = "SELECT n.id, COALESCE(" + coluna + ", '') AS termos FROM normas n " +
            "WHERE n.ementa IS NOT NULL AND TRIM(n.ementa) != '' ORDER BY n.id";
    } else {
      sql = "SELECT n.id, COALESCE(" + coluna + ", '') AS termos FROM normas n " +
            "JOIN ementas_pre_processadas e ON n.id = e.id_norma " +
            "WHERE " + coluna + " IS NOT NULL AND TRIM(" + coluna + ") != '' ORDER BY n.id";
    }

    if (limite > 0) {
      sql += " LIMIT " + limite;
    }

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
