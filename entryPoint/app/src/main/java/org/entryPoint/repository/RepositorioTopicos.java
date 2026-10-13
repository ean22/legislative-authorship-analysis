package org.entryPoint.repository;

import org.entryPoint.model.TopicoTematico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepositorioTopicos {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioTopicos() {
    this(new GerenciadorConexao());
  }

  public RepositorioTopicos(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public void salvarTopicosDescobertosEmLote(List<TopicoTematico> topicos) throws SQLException {
    String sql = """
      INSERT OR REPLACE INTO topicos (
        id,
        nome,
        termos_principais
      )
      VALUES (?, ?, ?)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      conexao.setAutoCommit(false);
      try {
        try (PreparedStatement deleteStmt = conexao.prepareStatement("DELETE FROM topicos")) {
          deleteStmt.executeUpdate();
        }
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
          for (TopicoTematico item : topicos) {
            statement.setInt(1, item.id());
            statement.setString(2, item.nome());
            statement.setString(3, String.join(", ", item.termosPrincipais()));
            statement.addBatch();
          }
          statement.executeBatch();
        }
        conexao.commit();
      } catch (SQLException e) {
        conexao.rollback();
        throw e;
      } finally {
        conexao.setAutoCommit(true);
      }
    }
  }

  public List<TopicoTematico> listarTopicosSalvos() throws SQLException {
    String sql = """
      SELECT id, COALESCE(nome, 'Tópico #' || id) AS nome, termos_principais
      FROM topicos
      ORDER BY id
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
          rs.getString("nome"),
          termosList
        ));
      }
    }
    return lista;
  }

  public record EstatisticaTopico(
      int idTopico,
      String nome,
      int totalNormas,
      double percentual,
      double scoreMedio,
      double scoreMin,
      double scoreMax
  ) {}

  public record NormaAmostraTopico(
      long idNorma,
      String tipo,
      int numero,
      int ano,
      String ementa,
      double score,
      String termosChave
  ) {}

  public List<EstatisticaTopico> consultarEstatisticasTopicos() throws SQLException {
    String sqlTotal = "SELECT COUNT(*) FROM norma_topicos";
    int grandTotal = 1;
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement st = conexao.prepareStatement(sqlTotal);
         ResultSet rs = st.executeQuery()) {
      if (rs.next()) {
        grandTotal = Math.max(1, rs.getInt(1));
      }
    }

    String sql = """
      SELECT t.id, 
             COALESCE(t.nome, 'Tópico #' || t.id) AS nome,
             COUNT(nt.id_norma) AS total_normas,
             COALESCE(AVG(nt.score_pertinencia), 0.0) AS score_medio,
             COALESCE(MIN(nt.score_pertinencia), 0.0) AS score_min,
             COALESCE(MAX(nt.score_pertinencia), 0.0) AS score_max
      FROM topicos t
      LEFT JOIN norma_topicos nt ON t.id = nt.id_topico
      GROUP BY t.id, t.nome
      ORDER BY t.id
    """;

    List<EstatisticaTopico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement statement = conexao.prepareStatement(sql);
         ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        int cnt = rs.getInt("total_normas");
        double pct = (cnt * 100.0) / grandTotal;
        lista.add(new EstatisticaTopico(
            rs.getInt("id"),
            rs.getString("nome"),
            cnt,
            pct,
            rs.getDouble("score_medio"),
            rs.getDouble("score_min"),
            rs.getDouble("score_max")
        ));
      }
    }
    return lista;
  }

  public List<NormaAmostraTopico> listarAmostraNormasPorTopico(int idTopico, int limite) throws SQLException {
    String sql = """
      SELECT n.id, n.tipoEscrito, n.numero, n.ano, n.ementa,
             nt.score_pertinencia, nt.termos_chave
      FROM norma_topicos nt
      JOIN normas n ON nt.id_norma = n.id
      WHERE nt.id_topico = ?
      ORDER BY nt.score_pertinencia DESC
      LIMIT ?
    """;

    List<NormaAmostraTopico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement statement = conexao.prepareStatement(sql)) {
      statement.setInt(1, idTopico);
      statement.setInt(2, limite > 0 ? limite : 5);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          lista.add(new NormaAmostraTopico(
              rs.getLong("id"),
              rs.getString("tipoEscrito"),
              rs.getInt("numero"),
              rs.getInt("ano"),
              rs.getString("ementa"),
              rs.getDouble("score_pertinencia"),
              rs.getString("termos_chave")
          ));
        }
      }
    }
    return lista;
  }
}
