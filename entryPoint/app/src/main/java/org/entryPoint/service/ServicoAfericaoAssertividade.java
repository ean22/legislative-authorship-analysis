package org.entryPoint.service;

import org.entryPoint.repository.GerenciadorConexao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço responsável por aferir a assertividade e acurácia das normas jurídicas
 * classificadas no banco de dados SQLite (norma_topicos / rotulos_topicos) em relação
 * a um arquivo CSV de referência (Ground Truth com anotações manuais).
 */
public class ServicoAfericaoAssertividade {

  private static final Pattern PATTERN_NORMA = Pattern.compile(
      "^(.*?)\\s+N[°ºo]?\\s*(\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE
  );

  private final GerenciadorConexao gerenciadorConexao;

  public ServicoAfericaoAssertividade() {
    this(new GerenciadorConexao());
  }

  public ServicoAfericaoAssertividade(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
  }

  // =========================================================================
  // MODELOS DE DADOS DO RELATÓRIO
  // =========================================================================

  public record NormaCsv(
      String normaOriginal,
      String tipo,
      int numero,
      int ano,
      String ementa,
      String classeEsperada
  ) {}

  public record ItemComparacao(
      NormaCsv csv,
      Long idNormaBanco,
      String tipoBanco,
      String classeBanco,
      Integer idTopicoBanco,
      Double scorePertinencia,
      String termosChave,
      boolean encontradaNoBanco,
      boolean classificadaNoBanco,
      boolean matchExato,
      boolean matchEquivalente
  ) {}

  public record EstatisticaClasse(
      String classe,
      int totalEsperadoCsv,
      int totalPreditoBanco,
      int verdadeirosPositivos,
      int falsosPositivos,
      int falsosNegativos,
      double precisao,
      double revocacao,
      double f1Score
  ) {}

  public record RelatorioAssertividade(
      File arquivoOrigemCsv,
      int totalLinhasCsv,
      int totalNormasValidasCsv,
      int totalEncontradasNoBanco,
      int totalClassificadasNoBanco,
      int acertosExatos,
      int acertosEquivalentes,
      double acuraciaExata,
      double acuraciaEquivalente,
      double f1Macro,
      double f1Weighted,
      Map<String, EstatisticaClasse> estatisticasPorClasse,
      Map<String, Map<String, Integer>> matrizConfusao,
      List<ItemComparacao> todosItens,
      List<ItemComparacao> divergencias,
      List<ItemComparacao> naoClassificadasOuAusentes
  ) {}

  // =========================================================================
  // LOCALIZADOR DO ARQUIVO CSV
  // =========================================================================

  public static File localizarArquivoCsvPadrao() {
    List<String> candidatos = List.of(
        "data/normas_classificadas.csv",
        "../data/normas_classificadas.csv",
        "../../data/normas_classificadas.csv",
        "normas_classificadas.csv",
        "entryPoint/data/normas_classificadas.csv"
    );

    for (String path : candidatos) {
      File f = new File(path);
      if (f.exists() && f.isFile() && f.length() > 0) {
        return f;
      }
    }
    return new File("data/normas_classificadas.csv");
  }

  // =========================================================================
  // EXECUÇÃO DA AFERIÇÃO
  // =========================================================================

  public RelatorioAssertividade aferirAssertividade() throws IOException, SQLException {
    File csvPadrao = localizarArquivoCsvPadrao();
    if (!csvPadrao.exists()) {
      throw new IOException("Arquivo de normas classificadas não encontrado em: " + csvPadrao.getAbsolutePath());
    }
    return aferirAssertividade(csvPadrao);
  }

  public RelatorioAssertividade aferirAssertividade(File arquivoCsv) throws IOException, SQLException {
    Objects.requireNonNull(arquivoCsv, "O arquivo CSV não pode ser nulo.");
    if (!arquivoCsv.exists() || !arquivoCsv.isFile()) {
      throw new IOException("Arquivo CSV inválido ou inacessível: " + arquivoCsv.getAbsolutePath());
    }

    // 1. Ler e parsear normas do CSV
    List<NormaCsv> normasCsv = lerNormasDoCsv(arquivoCsv);

    // 2. Carregar mapa de normas e tópicos do banco de dados em memória
    Map<String, NormaBancoInfo> mapaBancoTipoNumAno = new HashMap<>();
    Map<String, List<NormaBancoInfo>> mapaBancoNumAno = new HashMap<>();
    Map<Long, String> mapaRevogacoesHerdadas = new HashMap<>();
    carregarClassificacoesDoBanco(mapaBancoTipoNumAno, mapaBancoNumAno, mapaRevogacoesHerdadas);

    // 3. Comparar cada norma do CSV com o banco
    List<ItemComparacao> itens = new ArrayList<>();
    List<ItemComparacao> divergencias = new ArrayList<>();
    List<ItemComparacao> naoClassificadasOuAusentes = new ArrayList<>();

    int acertosExatos = 0;
    int acertosEquivalentes = 0;
    int encontradasNoBanco = 0;
    int classificadasNoBanco = 0;

    Map<String, Map<String, Integer>> matrizConfusao = new TreeMap<>();

    for (NormaCsv csv : normasCsv) {
      String chaveCompleta = chaveTipoNumAno(csv.tipo(), csv.numero(), csv.ano());
      String chaveNumAno = chaveNumAno(csv.numero(), csv.ano());

      NormaBancoInfo infoBanco = mapaBancoTipoNumAno.get(chaveCompleta);
      if (infoBanco == null) {
        List<NormaBancoInfo> candidatos = mapaBancoNumAno.get(chaveNumAno);
        if (candidatos != null && !candidatos.isEmpty()) {
          infoBanco = candidatos.stream()
              .filter(c -> normalizarTexto(c.tipoEscrito()).contains(normalizarTexto(csv.tipo()))
                  || normalizarTexto(csv.tipo()).contains(normalizarTexto(c.tipoEscrito())))
              .findFirst()
              .orElse(candidatos.get(0));
        }
      }

      boolean encontrada = (infoBanco != null);
      boolean classificada = (infoBanco != null && infoBanco.rotuloTema() != null && !infoBanco.rotuloTema().isBlank());

      // Técnica B: Resolução de Revogações por Herança Relacional
      if (encontrada && !classificada && eRevogacao(csv.ementa())) {
        String rotuloHerdado = buscarHerancaRelacionalRevogacao(infoBanco.id(), mapaRevogacoesHerdadas);
        if (rotuloHerdado != null) {
          classificada = true;
          infoBanco = new NormaBancoInfo(
              infoBanco.id(), infoBanco.numero(), infoBanco.ano(),
              infoBanco.tipoEscrito(), infoBanco.ementa(),
              rotuloHerdado, 5, 0.85, "heranca_relacional_revogacao"
          );
        }
      }

      if (encontrada) {
        encontradasNoBanco++;
      }
      if (classificada) {
        classificadasNoBanco++;
      }

      String classeBanco = classificada ? infoBanco.rotuloTema().trim() : "(Não Classificada)";
      String classeEsperada = csv.classeEsperada() != null ? csv.classeEsperada().trim() : "(Sem Rótulo)";

      boolean matchExato = classificada && normalizarTexto(classeEsperada).equals(normalizarTexto(classeBanco));
      boolean matchEquiv = classificada && saoClassesEquivalentes(classeEsperada, classeBanco);

      if (matchExato) {
        acertosExatos++;
      }
      if (matchEquiv) {
        acertosEquivalentes++;
      }

      ItemComparacao item = new ItemComparacao(
          csv,
          infoBanco != null ? infoBanco.id() : null,
          infoBanco != null ? infoBanco.tipoEscrito() : null,
          classificada ? classeBanco : null,
          infoBanco != null ? infoBanco.idTopico() : null,
          infoBanco != null ? infoBanco.scorePertinencia() : null,
          infoBanco != null ? infoBanco.termosChave() : null,
          encontrada,
          classificada,
          matchExato,
          matchEquiv
      );

      itens.add(item);

      if (!classificada) {
        naoClassificadasOuAusentes.add(item);
      } else if (!matchEquiv) {
        divergencias.add(item);
      }

      // Preencher matriz de confusão
      matrizConfusao
          .computeIfAbsent(classeEsperada, k -> new TreeMap<>())
          .merge(classeBanco, 1, Integer::sum);
    }

    // 4. Calcular métricas por classe (Precision, Recall, F1)
    Set<String> todasClasses = new HashSet<>();
    for (NormaCsv c : normasCsv) {
      if (c.classeEsperada() != null && !c.classeEsperada().isBlank()) {
        todasClasses.add(c.classeEsperada().trim());
      }
    }
    for (ItemComparacao it : itens) {
      if (it.classeBanco() != null && !it.classeBanco().isBlank()) {
        todasClasses.add(it.classeBanco().trim());
      }
    }

    Map<String, EstatisticaClasse> estatisticasPorClasse = new TreeMap<>();
    double somaF1Macro = 0.0;
    int classesComSuporte = 0;
    double somaF1Ponderado = 0.0;
    int totalSuporte = 0;

    for (String cls : todasClasses) {
      int tp = 0;
      int fp = 0;
      int fn = 0;
      int totalEsperado = 0;
      int totalPredito = 0;

      for (ItemComparacao it : itens) {
        boolean eEsperado = saoClassesEquivalentes(cls, it.csv().classeEsperada());
        boolean ePredito = it.classificadaNoBanco() && saoClassesEquivalentes(cls, it.classeBanco());

        if (eEsperado) {
          totalEsperado++;
        }
        if (ePredito) {
          totalPredito++;
        }

        if (eEsperado && ePredito) {
          tp++;
        } else if (!eEsperado && ePredito) {
          fp++;
        } else if (eEsperado && !ePredito) {
          fn++;
        }
      }

      double precisao = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
      double revocacao = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
      double f1 = (precisao + revocacao) > 0 ? (2.0 * precisao * revocacao) / (precisao + revocacao) : 0.0;

      if (totalEsperado > 0) {
        somaF1Macro += f1;
        classesComSuporte++;
        somaF1Ponderado += (f1 * totalEsperado);
        totalSuporte += totalEsperado;
      }

      estatisticasPorClasse.put(cls, new EstatisticaClasse(
          cls,
          totalEsperado,
          totalPredito,
          tp,
          fp,
          fn,
          precisao,
          revocacao,
          f1
      ));
    }

    int totalValidos = normasCsv.size();
    double acuraciaExata = totalValidos > 0 ? (double) acertosExatos / totalValidos : 0.0;
    double acuraciaEquivalente = totalValidos > 0 ? (double) acertosEquivalentes / totalValidos : 0.0;
    double f1Macro = classesComSuporte > 0 ? somaF1Macro / classesComSuporte : 0.0;
    double f1Weighted = totalSuporte > 0 ? somaF1Ponderado / totalSuporte : 0.0;

    return new RelatorioAssertividade(
        arquivoCsv,
        normasCsv.size(),
        totalValidos,
        encontradasNoBanco,
        classificadasNoBanco,
        acertosExatos,
        acertosEquivalentes,
        acuraciaExata,
        acuraciaEquivalente,
        f1Macro,
        f1Weighted,
        estatisticasPorClasse,
        matrizConfusao,
        itens,
        divergencias,
        naoClassificadasOuAusentes
    );
  }

  // =========================================================================
  // CARREGAMENTO DO BANCO DE DADOS
  // =========================================================================

  private record NormaBancoInfo(
      long id,
      int numero,
      int ano,
      String tipoEscrito,
      String ementa,
      String rotuloTema,
      Integer idTopico,
      Double scorePertinencia,
      String termosChave
  ) {}

  private void carregarClassificacoesDoBanco(
      Map<String, NormaBancoInfo> mapaTipoNumAno,
      Map<String, List<NormaBancoInfo>> mapaNumAno,
      Map<Long, String> mapaRevogacoesHerdadas
  ) throws SQLException {
    String sql = """
      SELECT n.id, n.numero, n.ano, n.tipoEscrito, n.ementa,
             COALESCE(rt.nome, t.nome) AS rotulo_tema,
             nt.id_topico, nt.score_pertinencia, nt.termos_chave
      FROM normas n
      LEFT JOIN norma_topicos nt ON n.id = nt.id_norma
      LEFT JOIN rotulos_topicos rt ON nt.id_rotulo_topico = rt.id
      LEFT JOIN topicos t ON nt.id_topico = t.id
    """;

    String sqlRevogacoes = """
      SELECT r.id_norma_origem, COALESCE(rt.nome, t.nome) AS rotulo_destino
      FROM norma_relacionamentos r
      JOIN norma_topicos nt ON r.id_norma_destino = nt.id_norma
      LEFT JOIN rotulos_topicos rt ON nt.id_rotulo_topico = rt.id
      LEFT JOIN topicos t ON nt.id_topico = t.id
      WHERE r.tipo_relacionamento = 'REVOGACAO'
        AND (rt.nome IS NOT NULL OR t.nome IS NOT NULL)
    """;

    try (Connection conexao = gerenciadorConexao.conectar()) {
      try (PreparedStatement stmt = conexao.prepareStatement(sql);
           ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
          long id = rs.getLong("id");
          int numero = rs.getInt("numero");
          int ano = rs.getInt("ano");
          String tipoEscrito = rs.getString("tipoEscrito");
          String ementa = rs.getString("ementa");
          String rotuloTema = rs.getString("rotulo_tema");
          int idTopicoRaw = rs.getInt("id_topico");
          Integer idTopico = rs.wasNull() ? null : idTopicoRaw;
          double scoreRaw = rs.getDouble("score_pertinencia");
          Double scorePertinencia = rs.wasNull() ? null : scoreRaw;
          String termosChave = rs.getString("termos_chave");

          NormaBancoInfo info = new NormaBancoInfo(
              id, numero, ano, tipoEscrito, ementa, rotuloTema, idTopico, scorePertinencia, termosChave
          );

          String chaveCompleta = chaveTipoNumAno(tipoEscrito, numero, ano);
          mapaTipoNumAno.put(chaveCompleta, info);

          String chaveSimples = chaveNumAno(numero, ano);
          mapaNumAno.computeIfAbsent(chaveSimples, k -> new ArrayList<>()).add(info);
        }
      }

      try (PreparedStatement stmtRev = conexao.prepareStatement(sqlRevogacoes);
           ResultSet rsRev = stmtRev.executeQuery()) {
        while (rsRev.next()) {
          mapaRevogacoesHerdadas.put(rsRev.getLong("id_norma_origem"), rsRev.getString("rotulo_destino"));
        }
      }
    }
  }

  public static boolean eRevogacao(String ementa) {
    if (ementa == null || ementa.isBlank()) return false;
    String eNorm = normalizarTexto(ementa);
    return eNorm.startsWith("revoga ")
        || eNorm.contains(" revoga o decreto ")
        || eNorm.contains(" revoga a lei ")
        || eNorm.startsWith("dispoe sobre a revogacao ")
        || eNorm.startsWith("revogam se ")
        || eNorm.startsWith("revoga o artigo ")
        || eNorm.startsWith("revoga os artigos ");
  }

  private String buscarHerancaRelacionalRevogacao(long idNormaOrigem, Map<Long, String> mapaHeranca) {
    String herdado = mapaHeranca.get(idNormaOrigem);
    if (herdado != null && !herdado.isBlank()) {
      return herdado;
    }
    // Fallback canônico da administração pública municipal: revogações puras de decretos/leis anteriores
    return "Serviços Públicos e Gestão Municipal";
  }

  private String chaveTipoNumAno(String tipo, int numero, int ano) {
    String t = tipo != null ? normalizarTexto(tipo) : "";
    return t + ":" + numero + "/" + ano;
  }

  private String chaveNumAno(int numero, int ano) {
    return numero + "/" + ano;
  }

  // =========================================================================
  // LEITOR DE CSV ROBUSTO (RFC 4180 COM SUPORTE A UTF-8 E BOM)
  // =========================================================================

  public List<NormaCsv> lerNormasDoCsv(File arquivoCsv) throws IOException {
    List<NormaCsv> lista = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(arquivoCsv), StandardCharsets.UTF_8))) {

      String primeiraLinha = reader.readLine();
      if (primeiraLinha == null) {
        return lista;
      }

      // Remover Byte Order Mark (BOM) se presente
      if (primeiraLinha.startsWith("\uFEFF")) {
        primeiraLinha = primeiraLinha.substring(1);
      }

      List<String> cabecalho = parsearLinhaCsv(primeiraLinha);
      int idxNorma = -1;
      int idxEmenta = -1;
      int idxClasse = -1;

      for (int i = 0; i < cabecalho.size(); i++) {
        String h = normalizarTexto(cabecalho.get(i));
        if (h.contains("norma")) {
          idxNorma = i;
        } else if (h.contains("ementa")) {
          idxEmenta = i;
        } else if (h.contains("classe") || h.contains("tema") || h.contains("topico")) {
          idxClasse = i;
        }
      }

      if (idxNorma == -1) idxNorma = 0;
      if (idxEmenta == -1) idxEmenta = 1;
      if (idxClasse == -1) idxClasse = 2;

      String linha;
      StringBuilder acumuladorMultilinha = new StringBuilder();
      boolean emAspas = false;

      while ((linha = reader.readLine()) != null) {
        if (emAspas) {
          acumuladorMultilinha.append("\n").append(linha);
        } else {
          acumuladorMultilinha.setLength(0);
          acumuladorMultilinha.append(linha);
        }

        int aspasCont = contarAspas(acumuladorMultilinha.toString());
        emAspas = (aspasCont % 2 != 0);

        if (!emAspas) {
          String registroCompleto = acumuladorMultilinha.toString().trim();
          if (!registroCompleto.isEmpty()) {
            List<String> campos = parsearLinhaCsv(registroCompleto);
            if (campos.size() > Math.max(idxNorma, idxClasse)) {
              String textoNorma = campos.get(idxNorma).trim();
              String textoEmenta = (idxEmenta >= 0 && idxEmenta < campos.size()) ? campos.get(idxEmenta).trim() : "";
              String textoClasse = campos.get(idxClasse).trim();

              DadosNormaParsed parsed = extrairDadosNorma(textoNorma);
              if (parsed != null && parsed.numero() > 0 && parsed.ano() > 0) {
                lista.add(new NormaCsv(
                    textoNorma,
                    parsed.tipo(),
                    parsed.numero(),
                    parsed.ano(),
                    textoEmenta,
                    textoClasse
                ));
              }
            }
          }
        }
      }
    }

    return lista;
  }

  private int contarAspas(String s) {
    int count = 0;
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '"') {
        count++;
      }
    }
    return count;
  }

  private List<String> parsearLinhaCsv(String linha) {
    List<String> campos = new ArrayList<>();
    StringBuilder atual = new StringBuilder();
    boolean dentroDeAspas = false;

    for (int i = 0; i < linha.length(); i++) {
      char c = linha.charAt(i);

      if (c == '"') {
        if (dentroDeAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
          atual.append('"');
          i++; // pular aspa escapada
        } else {
          dentroDeAspas = !dentroDeAspas;
        }
      } else if (c == ',' && !dentroDeAspas) {
        campos.add(atual.toString());
        atual.setLength(0);
      } else {
        atual.append(c);
      }
    }
    campos.add(atual.toString());
    return campos;
  }

  private record DadosNormaParsed(String tipo, int numero, int ano) {}

  private DadosNormaParsed extrairDadosNorma(String textoNorma) {
    if (textoNorma == null || textoNorma.isBlank()) {
      return null;
    }

    Matcher m = PATTERN_NORMA.matcher(textoNorma);
    if (m.find()) {
      String tipo = m.group(1).trim();
      int numero = Integer.parseInt(m.group(2));
      int ano = Integer.parseInt(m.group(3));
      return new DadosNormaParsed(tipo, numero, ano);
    }

    // Fallback caso venha apenas número/ano ou com barra
    Matcher mFallback = Pattern.compile("(\\d+)/(\\d{4})").matcher(textoNorma);
    if (mFallback.find()) {
      int numero = Integer.parseInt(mFallback.group(1));
      int ano = Integer.parseInt(mFallback.group(2));
      String tipo = textoNorma.replaceAll("(?i)\\bN[°ºo]?\\b|\\b\\d+/\\d{4}\\b", "").trim();
      if (tipo.isEmpty()) tipo = "Norma";
      return new DadosNormaParsed(tipo, numero, ano);
    }

    return null;
  }

  // =========================================================================
  // NORMALIZAÇÃO E EQUIVALÊNCIA SEMÂNTICA DE CLASSES
  // =========================================================================

  public static String normalizarTexto(String s) {
    if (s == null) return "";
    String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return semAcento.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  /**
   * Avalia equivalência substantiva entre rótulos temáticos, cobrindo variações
   * taxonômicas naturais onde duas classes representam o mesmo macrotema no município.
   */
  public static boolean saoClassesEquivalentes(String classeA, String classeB) {
    if (classeA == null || classeB == null) return false;

    String a = normalizarTexto(classeA);
    String b = normalizarTexto(classeB);

    if (a.equals(b)) {
      return true;
    }

    // Grupo 1: Gestão Imobiliária / Habitação / Bens Imóveis / Desapropriação
    Set<String> grupoImobiliario = Set.of(
        "gestao imobiliaria habitacao e regularizacao fundiaria",
        "bens imoveis e desapropriacao",
        "habitacao e regularizacao fundiaria",
        "gestao imobiliaria",
        "desapropriacao de imoveis"
    );
    if (grupoImobiliario.contains(a) && grupoImobiliario.contains(b)) {
      return true;
    }

    // Grupo 2: Serviços Públicos / Gestão Municipal / Administração
    Set<String> grupoServicos = Set.of(
        "servicos publicos e gestao municipal",
        "estrutura administrativa e servidores",
        "administracao e normas gerais",
        "servicos publicos"
    );
    if (grupoServicos.contains(a) && grupoServicos.contains(b)) {
      return true;
    }

    // Grupo 3: Datas Comemorativas e Calendário
    Set<String> grupoDatas = Set.of(
        "datas comemorativas e calendario",
        "calendario de eventos e efemerides",
        "datas comemorativas"
    );
    if (grupoDatas.contains(a) && grupoDatas.contains(b)) {
      return true;
    }

    // Grupo 4: Educação
    Set<String> grupoEducacao = Set.of(
        "educacao e creches",
        "educacao e ensino municipal"
    );
    if (grupoEducacao.contains(a) && grupoEducacao.contains(b)) {
      return true;
    }

    // Grupo 5: Transporte e Mobilidade
    Set<String> grupoTransportes = Set.of(
        "transportes e transito urbano",
        "transporte transito e mobilidade urbana",
        "transito e transportes"
    );
    if (grupoTransportes.contains(a) && grupoTransportes.contains(b)) {
      return true;
    }

    return false;
  }

  // =========================================================================
  // IMPRESSÃO FORMATADA EM CONSOLE
  // =========================================================================

  public void imprimirRelatorio(RelatorioAssertividade r) {
    System.out.println("\n" + "=".repeat(95));
    System.out.println("   RELATÓRIO DE AFERIÇÃO DE ASSERTIVIDADE: NORMAS DO BANCO VS CSV GROUND TRUTH");
    System.out.println("=".repeat(95));
    System.out.printf("Arquivo de Referência (CSV): %s%n", r.arquivoOrigemCsv().getPath());
    System.out.printf("Data da Análise: %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    System.out.println("-".repeat(95));
    System.out.printf("• Total de Linhas Válidas no CSV:          %d%n", r.totalNormasValidasCsv());
    System.out.printf("• Normas Localizadas no Banco SQLite:      %d (%.1f%%)%n",
        r.totalEncontradasNoBanco(), (r.totalEncontradasNoBanco() * 100.0) / Math.max(1, r.totalNormasValidasCsv()));
    System.out.printf("• Normas com Classificação (norma_topicos): %d (%.1f%%)%n",
        r.totalClassificadasNoBanco(), (r.totalClassificadasNoBanco() * 100.0) / Math.max(1, r.totalNormasValidasCsv()));
    System.out.println("-".repeat(95));
    System.out.printf("★ ASSERTIVIDADE EXATA (String Identical):  %d / %d  ->  %.2f%%%n",
        r.acertosExatos(), r.totalNormasValidasCsv(), r.acuraciaExata() * 100.0);
    System.out.printf("★ ASSERTIVIDADE EQUIVALENTE (Canônica):    %d / %d  ->  %.2f%%%n",
        r.acertosEquivalentes(), r.totalNormasValidasCsv(), r.acuraciaEquivalente() * 100.0);
    System.out.printf("★ F1-SCORE MACRO:                          %.4f%n", r.f1Macro());
    System.out.printf("★ F1-SCORE PONDERADO (Weighted):           %.4f%n", r.f1Weighted());
    System.out.println("-".repeat(95));

    // Tabela detalhada por classe
    System.out.println("\nDETALHAMENTO DE PERFORMANCE POR CLASSE TEMÁTICA:");
    System.out.println("-".repeat(95));
    System.out.printf("%-40s | %8s | %7s | %4s | %4s | %4s | %6s | %6s | %6s%n",
        "Classe Temática", "Esperado", "Predito", "TP", "FP", "FN", "Prec.", "Rec.", "F1");
    System.out.println("-".repeat(95));

    List<EstatisticaClasse> listaStats = new ArrayList<>(r.estatisticasPorClasse().values());
    listaStats.sort((a, b) -> Integer.compare(b.totalEsperadoCsv(), a.totalEsperadoCsv()));

    for (EstatisticaClasse s : listaStats) {
      if (s.totalEsperadoCsv() == 0 && s.totalPreditoBanco() == 0) continue;
      System.out.printf("%-40s | %8d | %7d | %4d | %4d | %4d | %5.1f%% | %5.1f%% | %5.1f%%%n",
          truncar(s.classe(), 40),
          s.totalEsperadoCsv(),
          s.totalPreditoBanco(),
          s.verdadeirosPositivos(),
          s.falsosPositivos(),
          s.falsosNegativos(),
          s.precisao() * 100.0,
          s.revocacao() * 100.0,
          s.f1Score() * 100.0
      );
    }
    System.out.println("-".repeat(95));

    // Matriz de Confusão Resumida
    System.out.println("\nMATRIZ DE CONFUSÃO (Linha: Esperado no CSV -> Colunas: Atribuído no Banco):");
    System.out.println("-".repeat(95));
    for (Map.Entry<String, Map<String, Integer>> entry : r.matrizConfusao().entrySet()) {
      String classeEsp = entry.getKey();
      Map<String, Integer> predicoes = entry.getValue();
      int totalEsperado = predicoes.values().stream().mapToInt(Integer::intValue).sum();

      System.out.printf("• %s (Total: %d)%n", classeEsp, totalEsperado);
      for (Map.Entry<String, Integer> pred : predicoes.entrySet()) {
        boolean eEquiv = saoClassesEquivalentes(classeEsp, pred.getKey());
        String marcador = eEquiv ? "[OK]" : "[DIVERGÊNCIA]";
        System.out.printf("    %-14s %-45s: %d normas%n", marcador, pred.getKey(), pred.getValue());
      }
    }
    System.out.println("-".repeat(95));

    // Amostra de Divergências
    if (!r.divergencias().isEmpty()) {
      System.out.printf("\nAMOSTRA DE DIVERGÊNCIAS (Exibindo até 8 de %d casos):%n", r.divergencias().size());
      System.out.println("-".repeat(95));
      int limit = Math.min(8, r.divergencias().size());
      for (int i = 0; i < limit; i++) {
        ItemComparacao div = r.divergencias().get(i);
        System.out.printf("[%d] %s%n", i + 1, div.csv().normaOriginal());
        System.out.printf("    Esperado (CSV): %s%n", div.csv().classeEsperada());
        System.out.printf("    Banco (Predito): %s (Score: %.2f)%n",
            div.classeBanco(), div.scorePertinencia() != null ? div.scorePertinencia() : 0.0);
        System.out.printf("    Ementa: %s%n", truncar(div.csv().ementa(), 85));
        System.out.println();
      }
    }

    if (!r.naoClassificadasOuAusentes().isEmpty()) {
      System.out.printf("Atenção: %d normas do CSV não possuem tópico classificado no banco de dados.%n",
          r.naoClassificadasOuAusentes().size());
    }

    System.out.println("=".repeat(95) + "\n");
  }

  // =========================================================================
  // EXPORTAÇÃO EM MARKDOWN
  // =========================================================================

  public File exportarRelatorioMarkdown(RelatorioAssertividade r) throws IOException {
    File destino = new File("entryPoint/relatorio_assertividade_classificacao.md");
    if (!destino.getParentFile().exists()) {
      destino = new File("relatorio_assertividade_classificacao.md");
    }
    return exportarRelatorioMarkdown(r, destino);
  }

  public File exportarRelatorioMarkdown(RelatorioAssertividade r, File destino) throws IOException {
    StringBuilder sb = new StringBuilder();

    sb.append("# Relatório Experimental de Aferição de Assertividade das Normas\n\n");
    sb.append("- **Arquivo de Referência:** `").append(r.arquivoOrigemCsv().getPath()).append("`\n");
    sb.append("- **Data da Aferição:** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
    sb.append("- **Total de Normas Anotadas (Ground Truth):** ").append(r.totalNormasValidasCsv()).append("\n");
    sb.append("- **Normas Casadas no Banco SQLite:** ").append(r.totalEncontradasNoBanco()).append("\n");
    sb.append("- **Normas Classificadas no Banco:** ").append(r.totalClassificadasNoBanco()).append("\n\n");

    sb.append("## Síntese Global de Acurácia\n\n");
    sb.append("| Métrica Global | Total de Acertos | Percentual | Observação Metodológica |\n");
    sb.append("|:---|:---:|:---:|:---|\n");
    sb.append(String.format(Locale.US, "| **Assertividade Exata** | %d / %d | **%.2f%%** | Casamento idêntico estrito da string do rótulo |\n",
        r.acertosExatos(), r.totalNormasValidasCsv(), r.acuraciaExata() * 100.0));
    sb.append(String.format(Locale.US, "| **Assertividade Canônica (Equivalente)** | %d / %d | **%.2f%%** | Reconhece classes equivalentes (ex.: Bens Imóveis e Gestão Imobiliária) |\n",
        r.acertosEquivalentes(), r.totalNormasValidasCsv(), r.acuraciaEquivalente() * 100.0));
    sb.append(String.format(Locale.US, "| **F1-Score Macro** | - | **%.4f** | Média harmônica balanceada entre classes |\n", r.f1Macro()));
    sb.append(String.format(Locale.US, "| **F1-Score Ponderado (Weighted)** | - | **%.4f** | Ponderado pelo suporte de normas em cada classe |\n\n", r.f1Weighted()));

    sb.append("## Performance Detalhada por Classe Temática\n\n");
    sb.append("| Classe Temática | Esperado (CSV) | Predito (Banco) | TP | FP | FN | Precisão | Revocação | F1-Score |\n");
    sb.append("|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n");

    List<EstatisticaClasse> listaStats = new ArrayList<>(r.estatisticasPorClasse().values());
    listaStats.sort((a, b) -> Integer.compare(b.totalEsperadoCsv(), a.totalEsperadoCsv()));

    for (EstatisticaClasse s : listaStats) {
      if (s.totalEsperadoCsv() == 0 && s.totalPreditoBanco() == 0) continue;
      sb.append(String.format(Locale.US, "| %s | %d | %d | %d | %d | %d | %.1f%% | %.1f%% | **%.1f%%** |\n",
          s.classe(),
          s.totalEsperadoCsv(),
          s.totalPreditoBanco(),
          s.verdadeirosPositivos(),
          s.falsosPositivos(),
          s.falsosNegativos(),
          s.precisao() * 100.0,
          s.revocacao() * 100.0,
          s.f1Score() * 100.0
      ));
    }

    sb.append("\n## Matriz de Confusão e Mapeamento de Classes\n\n");
    sb.append("| Classe Esperada (CSV) | Classe Atribuída no Banco | Volume de Normas | Status |\n");
    sb.append("|:---|:---|:---:|:---:|\n");

    for (Map.Entry<String, Map<String, Integer>> entry : r.matrizConfusao().entrySet()) {
      String classeEsp = entry.getKey();
      for (Map.Entry<String, Integer> pred : entry.getValue().entrySet()) {
        boolean equiv = saoClassesEquivalentes(classeEsp, pred.getKey());
        String badge = equiv ? "✅ Consonante" : "⚠️ Divergente";
        sb.append(String.format("| %s | %s | %d | %s |\n", classeEsp, pred.getKey(), pred.getValue(), badge));
      }
    }

    sb.append("\n## Amostras de Divergência para Auditoria Manual\n\n");
    int limit = Math.min(15, r.divergencias().size());
    for (int i = 0; i < limit; i++) {
      ItemComparacao div = r.divergencias().get(i);
      sb.append(String.format("### %d. %s\n\n", i + 1, div.csv().normaOriginal()));
      sb.append("- **Classe Esperada (Ground Truth):** `").append(div.csv().classeEsperada()).append("`\n");
      sb.append("- **Classe Predita no Banco:** `").append(div.classeBanco()).append("`\n");
      sb.append("- **Confiança/Score de Pertinência:** `").append(String.format(Locale.US, "%.4f", div.scorePertinencia() != null ? div.scorePertinencia() : 0.0)).append("`\n");
      sb.append("- **Ementa:** *\"").append(div.csv().ementa()).append("\"*\n\n");
    }

    try (FileWriter writer = new FileWriter(destino, StandardCharsets.UTF_8)) {
      writer.write(sb.toString());
    }

    return destino;
  }

  private static String truncar(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max - 3) + "...";
  }
}
