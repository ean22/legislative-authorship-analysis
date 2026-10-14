package org.entryPoint.repository;

import org.entryPoint.model.perfil.DistribuicaoTopico;
import org.entryPoint.model.perfil.ParceriaPolitica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório especializado para consultas analíticas sobre o perfil político dos autores,
 * cruzando dados de autoria, tipologia de normas, tópicos descobertos e rede de colaboração.
 */
public class RepositorioPerfilPolitico {
  private final GerenciadorConexao gerenciadorConexao;

  public RepositorioPerfilPolitico() {
    this(new GerenciadorConexao());
  }

  public RepositorioPerfilPolitico(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  public record InfoBasicaAutor(long id, String nome, String cargoPrincipal) {}

  public record ResumoAutorProlifico(long id, String nome, String cargo, int totalNormas) {}

  public record DadoAtuacaoBruto(
      int totalNormas,
      Map<String, Integer> tiposNormas,
      int normasIndividuais,
      int normasColetivas,
      double mediaAutoresPorNorma,
      Map<String, Integer> cargosExercidos,
      String cargoMaisFrequente
  ) {}

  public record DadoEvolucaoBruto(
      Map<Integer, Integer> producaoPorAno,
      Map<Integer, Map<String, Integer>> temasPorAno
  ) {}

  public record ArestaBruta(
      long id1,
      String nome1,
      long id2,
      String nome2,
      int peso
  ) {}

  /**
   * Localiza informações básicas do autor (nome e cargo mais exercido).
   */
  public Optional<InfoBasicaAutor> buscarInfoBasicaAutor(long idAutor) throws SQLException {
    String sql = """
      SELECT a.id, a.nome,
             (SELECT na2.cargo_autor_normalizado
              FROM norma_autor na2
              WHERE na2.id_autor = a.id AND na2.cargo_autor_normalizado IS NOT NULL
              GROUP BY na2.cargo_autor_normalizado
              ORDER BY COUNT(*) DESC
              LIMIT 1) AS cargo_principal
      FROM autores a
      WHERE a.id = ?
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new InfoBasicaAutor(
              rs.getLong("id"),
              rs.getString("nome"),
              rs.getString("cargo_principal")
          ));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Localiza ID do autor por nome (busca exata ou parcial sem acentos/case-insensitive).
   */
  public Optional<Long> buscarIdAutorPorNome(String nome) throws SQLException {
    if (nome == null || nome.isBlank()) return Optional.empty();
    String sql = "SELECT id FROM autores WHERE UPPER(nome) = UPPER(?) LIMIT 1";

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setString(1, nome.trim());
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("id"));
        }
      }
    }

    // Busca parcial caso não encontre exato
    String sqlLike = "SELECT id FROM autores WHERE UPPER(nome) LIKE UPPER(?) ORDER BY LENGTH(nome) ASC LIMIT 1";
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlLike)) {
      stmt.setString(1, "%" + nome.trim() + "%");
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("id"));
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Consulta a distribuição temática de um autor cruzando norma_autor com norma_topicos.
   */
  public List<DistribuicaoTopico> buscarDistribuicaoTematica(long idAutor) throws SQLException {
    String sql = """
      SELECT nt.id_topico AS id,
             COALESCE(t.nome, (SELECT r.nome FROM rotulos_topicos r WHERE r.id = nt.id_rotulo_topico), 'Tópico #' || nt.id_topico) AS nome,
             COUNT(*) AS qtd,
             AVG(nt.score_pertinencia) AS score_medio
      FROM norma_autor na
      JOIN norma_topicos nt ON nt.id_norma = na.id_norma
      LEFT JOIN topicos t ON t.id = nt.id_topico
      WHERE na.id_autor = ?
      GROUP BY nt.id_topico
      ORDER BY qtd DESC
    """;

    List<DistribuicaoTopico> lista = new ArrayList<>();
    int totalNormasComTopico = 0;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        List<int[]> contagens = new ArrayList<>();
        List<Object[]> dados = new ArrayList<>();

        while (rs.next()) {
          int id = rs.getInt("id");
          String nome = rs.getString("nome");
          int qtd = rs.getInt("qtd");
          double score = rs.getDouble("score_medio");
          totalNormasComTopico += qtd;
          dados.add(new Object[]{id, nome, qtd, score});
        }

        for (Object[] d : dados) {
          int id = (int) d[0];
          String nome = (String) d[1];
          int qtd = (int) d[2];
          double score = (double) d[3];
          double pct = totalNormasComTopico > 0 ? (qtd * 100.0 / totalNormasComTopico) : 0.0;
          lista.add(new DistribuicaoTopico(id, nome, qtd, pct, score));
        }
      }
    }
    return lista;
  }

  /**
   * Coleta os termos-chave mais frequentes das normas associadas a este autor.
   */
  public List<String> buscarTermosRecorrentesAutor(long idAutor, int limite) throws SQLException {
    String sql = """
      SELECT nt.termos_chave
      FROM norma_autor na
      JOIN norma_topicos nt ON nt.id_norma = na.id_norma
      WHERE na.id_autor = ? AND nt.termos_chave IS NOT NULL AND TRIM(nt.termos_chave) <> ''
      LIMIT 600
    """;

    Map<String, Integer> freqTermos = new HashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String termosStr = rs.getString("termos_chave");
          if (termosStr != null) {
            String[] tokens = termosStr.split(",\\s*");
            for (String t : tokens) {
              String limpo = t.trim().toLowerCase();
              if (limpo.length() > 2) {
                freqTermos.put(limpo, freqTermos.getOrDefault(limpo, 0) + 1);
              }
            }
          }
        }
      }
    }

    return freqTermos.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(limite > 0 ? limite : 8)
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * Consulta dados de atuação legislativa (tipos de normas, autoria individual vs coletiva, cargos).
   */
  public DadoAtuacaoBruto buscarAtuacaoLegislativa(long idAutor) throws SQLException {
    Map<String, Integer> tiposNormas = new LinkedHashMap<>();
    int totalNormas = 0;

    String sqlTipos = """
      SELECT COALESCE(n.tipoEscrito, 'Outros') AS tipo, COUNT(DISTINCT na.id_norma) AS qtd
      FROM norma_autor na
      JOIN normas n ON n.id = na.id_norma
      WHERE na.id_autor = ?
      GROUP BY tipo
      ORDER BY qtd DESC
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlTipos)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String tipo = rs.getString("tipo");
          int qtd = rs.getInt("qtd");
          tiposNormas.put(tipo, qtd);
          totalNormas += qtd;
        }
      }
    }

    // Contagem de normas individuais vs coletivas e média de signatários
    String sqlAutoria = """
      SELECT 
        CASE WHEN sub.total_autores = 1 THEN 1 ELSE 0 END AS individual,
        COUNT(*) AS qtd,
        SUM(sub.total_autores) AS soma_autores
      FROM (
        SELECT na1.id_norma, COUNT(na2.id_autor) AS total_autores
        FROM norma_autor na1
        JOIN norma_autor na2 ON na1.id_norma = na2.id_norma
        WHERE na1.id_autor = ?
        GROUP BY na1.id_norma
      ) sub
      GROUP BY individual
    """;

    int individuais = 0;
    int coletivas = 0;
    long somaTotalAutores = 0;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlAutoria)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int individual = rs.getInt("individual");
          int qtd = rs.getInt("qtd");
          long soma = rs.getLong("soma_autores");
          if (individual == 1) {
            individuais = qtd;
          } else {
            coletivas = qtd;
          }
          somaTotalAutores += soma;
        }
      }
    }

    double mediaAutores = totalNormas > 0 ? ((double) somaTotalAutores / totalNormas) : 1.0;

    // Cargos exercidos pelo autor
    Map<String, Integer> cargos = new LinkedHashMap<>();
    String sqlCargos = """
      SELECT COALESCE(cargo_autor_normalizado, cargo_autor, 'NÃO INFORMADO') AS cargo, COUNT(*) AS qtd
      FROM norma_autor
      WHERE id_autor = ?
      GROUP BY cargo
      ORDER BY qtd DESC
    """;

    String cargoMaisFrequente = "N/A";
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlCargos)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        boolean primeiro = true;
        while (rs.next()) {
          String c = rs.getString("cargo");
          int q = rs.getInt("qtd");
          cargos.put(c, q);
          if (primeiro) {
            cargoMaisFrequente = c;
            primeiro = false;
          }
        }
      }
    }

    return new DadoAtuacaoBruto(
        totalNormas, tiposNormas, individuais, coletivas, mediaAutores, cargos, cargoMaisFrequente
    );
  }

  /**
   * Consulta os principais parceiros de coautoria do autor.
   */
  public List<ParceriaPolitica> buscarPrincipaisParcerias(long idAutor, int totalNormasAutor, int limite) throws SQLException {
    String sql = """
      SELECT a2.id, a2.nome, na2.cargo_autor_normalizado,
             COUNT(DISTINCT na1.id_norma) AS qtd_compartilhada
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na1.id_autor <> na2.id_autor
      JOIN autores a2 ON a2.id = na2.id_autor
      WHERE na1.id_autor = ?
      GROUP BY a2.id, a2.nome, na2.cargo_autor_normalizado
      ORDER BY qtd_compartilhada DESC
      LIMIT ?
    """;

    List<ParceriaPolitica> parcerias = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      stmt.setInt(2, limite > 0 ? limite : 8);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          long idParceiro = rs.getLong("id");
          String nomeParceiro = rs.getString("nome");
          String cargoParceiro = rs.getString("cargo_autor_normalizado");
          int qtd = rs.getInt("qtd_compartilhada");
          double pct = totalNormasAutor > 0 ? (qtd * 100.0 / totalNormasAutor) : 0.0;

          List<String> topicosEmComum = buscarTopicosCompartilhados(conexao, idAutor, idParceiro, 3);
          parcerias.add(new ParceriaPolitica(idParceiro, nomeParceiro, cargoParceiro, qtd, pct, topicosEmComum));
        }
      }
    }
    return parcerias;
  }

  private List<String> buscarTopicosCompartilhados(Connection conexao, long idAutor1, long idAutor2, int limite) throws SQLException {
    String sql = """
      SELECT COALESCE(t.nome, (SELECT r.nome FROM rotulos_topicos r WHERE r.id = nt.id_rotulo_topico), 'Tópico #' || nt.id_topico) AS nome,
             COUNT(*) AS qtd
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na2.id_autor = ?
      JOIN norma_topicos nt ON nt.id_norma = na1.id_norma
      LEFT JOIN topicos t ON t.id = nt.id_topico
      WHERE na1.id_autor = ?
      GROUP BY nt.id_topico
      ORDER BY qtd DESC
      LIMIT ?
    """;

    List<String> topicos = new ArrayList<>();
    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor2);
      stmt.setLong(2, idAutor1);
      stmt.setInt(3, limite);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          topicos.add(rs.getString("nome"));
        }
      }
    }
    return topicos;
  }

  /**
   * Consulta a agregação de parcerias políticas por cargo do parceiro.
   */
  public Map<String, Integer> buscarParceriasPorCargo(long idAutor) throws SQLException {
    String sql = """
      SELECT COALESCE(na2.cargo_autor_normalizado, 'OUTROS CARGOS') AS cargo,
             COUNT(DISTINCT na1.id_norma) AS qtd
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na1.id_autor <> na2.id_autor
      WHERE na1.id_autor = ?
      GROUP BY cargo
      ORDER BY qtd DESC
    """;

    Map<String, Integer> mapa = new LinkedHashMap<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          mapa.put(rs.getString("cargo"), rs.getInt("qtd"));
        }
      }
    }
    return mapa;
  }

  /**
   * Conta quantos parceiros distintos o autor tem no banco.
   */
  public int contarParceirosDistintos(long idAutor) throws SQLException {
    String sql = """
      SELECT COUNT(DISTINCT na2.id_autor)
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na1.id_autor <> na2.id_autor
      WHERE na1.id_autor = ?
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    return 0;
  }

  /**
   * Consulta a evolução temporal da produção e temas ano a ano.
   */
  public DadoEvolucaoBruto buscarEvolucaoTemporal(long idAutor) throws SQLException {
    Map<Integer, Integer> producaoPorAno = new LinkedHashMap<>();
    String sqlProducao = """
      SELECT n.ano, COUNT(DISTINCT na.id_norma) AS qtd
      FROM norma_autor na
      JOIN normas n ON n.id = na.id_norma
      WHERE na.id_autor = ? AND n.ano IS NOT NULL AND n.ano > 1900
      GROUP BY n.ano
      ORDER BY n.ano ASC
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlProducao)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          producaoPorAno.put(rs.getInt("ano"), rs.getInt("qtd"));
        }
      }
    }

    Map<Integer, Map<String, Integer>> temasPorAno = new LinkedHashMap<>();
    String sqlTemasAno = """
      SELECT n.ano,
             COALESCE(t.nome, (SELECT r.nome FROM rotulos_topicos r WHERE r.id = nt.id_rotulo_topico), 'Tópico #' || nt.id_topico) AS nome,
             COUNT(*) AS qtd
      FROM norma_autor na
      JOIN normas n ON n.id = na.id_norma
      JOIN norma_topicos nt ON nt.id_norma = na.id_norma
      LEFT JOIN topicos t ON t.id = nt.id_topico
      WHERE na.id_autor = ? AND n.ano IS NOT NULL AND n.ano > 1900
      GROUP BY n.ano, nt.id_topico
      ORDER BY n.ano ASC, qtd DESC
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sqlTemasAno)) {
      stmt.setLong(1, idAutor);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int ano = rs.getInt("ano");
          String tema = rs.getString("nome");
          int qtd = rs.getInt("qtd");
          temasPorAno.computeIfAbsent(ano, k -> new LinkedHashMap<>()).put(tema, qtd);
        }
      }
    }

    return new DadoEvolucaoBruto(producaoPorAno, temasPorAno);
  }

  /**
   * Lista os autores mais prolíficos do acervo legislativo.
   */
  public List<ResumoAutorProlifico> listarAutoresMaisProlificos(int limite) throws SQLException {
    String sql = """
      SELECT a.id, a.nome,
             (SELECT na2.cargo_autor_normalizado
              FROM norma_autor na2
              WHERE na2.id_autor = a.id AND na2.cargo_autor_normalizado IS NOT NULL
              GROUP BY na2.cargo_autor_normalizado
              ORDER BY COUNT(*) DESC LIMIT 1) AS cargo,
             COUNT(DISTINCT na.id_norma) AS total_normas
      FROM autores a
      JOIN norma_autor na ON na.id_autor = a.id
      GROUP BY a.id, a.nome
      ORDER BY total_normas DESC
      LIMIT ?
    """;

    List<ResumoAutorProlifico> lista = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setInt(1, limite > 0 ? limite : 15);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          lista.add(new ResumoAutorProlifico(
              rs.getLong("id"),
              rs.getString("nome"),
              rs.getString("cargo"),
              rs.getInt("total_normas")
          ));
        }
      }
    }
    return lista;
  }

  /**
   * Consulta laços de coautoria globais para montagem do Grafo de Colaboração.
   */
  public List<ArestaBruta> buscarParesColaboracaoGlobais(int pesoMinimo, int limite) throws SQLException {
    String sql = """
      SELECT 
        na1.id_autor AS autor1, a1.nome AS nome1,
        na2.id_autor AS autor2, a2.nome AS nome2,
        COUNT(DISTINCT na1.id_norma) AS peso
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na1.id_autor < na2.id_autor
      JOIN autores a1 ON a1.id = na1.id_autor
      JOIN autores a2 ON a2.id = na2.id_autor
      GROUP BY na1.id_autor, na2.id_autor
      HAVING peso >= ?
      ORDER BY peso DESC
      LIMIT ?
    """;

    List<ArestaBruta> arestas = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setInt(1, Math.max(1, pesoMinimo));
      stmt.setInt(2, limite > 0 ? limite : 250);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          arestas.add(new ArestaBruta(
              rs.getLong("autor1"),
              rs.getString("nome1"),
              rs.getLong("autor2"),
              rs.getString("nome2"),
              rs.getInt("peso")
          ));
        }
      }
    }
    return arestas;
  }

  /**
   * Consulta laços de coautoria focados em um autor específico.
   */
  public List<ArestaBruta> buscarParesColaboracaoDoAutor(long idAutor, int pesoMinimo) throws SQLException {
    String sql = """
      SELECT 
        na1.id_autor AS autor1, a1.nome AS nome1,
        na2.id_autor AS autor2, a2.nome AS nome2,
        COUNT(DISTINCT na1.id_norma) AS peso
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na1.id_autor <> na2.id_autor
      JOIN autores a1 ON a1.id = na1.id_autor
      JOIN autores a2 ON a2.id = na2.id_autor
      WHERE na1.id_autor = ?
      GROUP BY na2.id_autor
      HAVING peso >= ?
      ORDER BY peso DESC
    """;

    List<ArestaBruta> arestas = new ArrayList<>();
    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor);
      stmt.setInt(2, Math.max(1, pesoMinimo));
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          arestas.add(new ArestaBruta(
              rs.getLong("autor1"),
              rs.getString("nome1"),
              rs.getLong("autor2"),
              rs.getString("nome2"),
              rs.getInt("peso")
          ));
        }
      }
    }
    return arestas;
  }

  /**
   * Conta quantas normas foram assinadas conjuntamente por dois autores.
   */
  public int contarNormasCompartilhadas(long idAutor1, long idAutor2) throws SQLException {
    String sql = """
      SELECT COUNT(DISTINCT na1.id_norma)
      FROM norma_autor na1
      JOIN norma_autor na2 ON na1.id_norma = na2.id_norma AND na2.id_autor = ?
      WHERE na1.id_autor = ?
    """;

    try (Connection conexao = gerenciadorConexao.conectar();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setLong(1, idAutor2);
      stmt.setLong(2, idAutor1);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    }
    return 0;
  }

  public List<String> obterTopicosCompartilhados(long idAutor1, long idAutor2, int limite) throws SQLException {
    try (Connection conexao = gerenciadorConexao.conectar()) {
      return buscarTopicosCompartilhados(conexao, idAutor1, idAutor2, limite);
    }
  }
}
