package org.entryPoint.service;

import org.entryPoint.model.perfil.ComparativoPerfisAutores;
import org.entryPoint.model.perfil.DistribuicaoTopico;
import org.entryPoint.model.perfil.GrafoColaboracaoPolitica;
import org.entryPoint.model.perfil.ParceriaPolitica;
import org.entryPoint.model.perfil.PerfilAtuacaoLegislativa;
import org.entryPoint.model.perfil.PerfilColaboracaoPolitica;
import org.entryPoint.model.perfil.PerfilPoliticoAutor;
import org.entryPoint.model.perfil.PerfilTematicoLegislativo;
import org.entryPoint.model.perfil.PerfilTemporalEvolucao;
import org.entryPoint.repository.GerenciadorConexao;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioPerfilPolitico;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço responsável por orquestrar a geração dos Perfis Políticos Legislativos dos autores
 * e a construção dos Grafos de Relacionamento e Colaboração Política.
 */
public class ServicoPerfilPolitico {
  private final RepositorioPerfilPolitico repositorioPerfil;

  public ServicoPerfilPolitico() {
    this(new RepositorioPerfilPolitico());
  }

  public ServicoPerfilPolitico(RepositorioPerfilPolitico repositorioPerfil) {
    this.repositorioPerfil = repositorioPerfil;
  }

  public ServicoPerfilPolitico(RepositorioBancoDados repositorioBanco) {
    this(repositorioBanco.getRepositorioPerfilPolitico());
  }

  /**
   * Constrói o perfil político multidimensional completo de um autor a partir de seu ID.
   */
  public Optional<PerfilPoliticoAutor> gerarPerfilAutor(long idAutor) throws SQLException {
    var optInfo = repositorioPerfil.buscarInfoBasicaAutor(idAutor);
    if (optInfo.isEmpty()) {
      return Optional.empty();
    }

    var info = optInfo.get();

    // 1. DIMENSÃO TEMÁTICA
    List<DistribuicaoTopico> distTopicos = repositorioPerfil.buscarDistribuicaoTematica(idAutor);
    int totalNormasComTopico = distTopicos.stream().mapToInt(DistribuicaoTopico::quantidadeNormas).sum();
    String topicoDominante = distTopicos.isEmpty() ? "Não Classificado" : distTopicos.get(0).nomeTopico();
    double pctDominante = distTopicos.isEmpty() ? 0.0 : distTopicos.get(0).percentual();

    // Cálculo do HHI (Herfindahl-Hirschman Index) de Especialização: sum((p_i / 100)^2)
    double hhi = 0.0;
    double entropia = 0.0;
    int k = distTopicos.size();

    for (DistribuicaoTopico dt : distTopicos) {
      double p = dt.percentual() / 100.0;
      if (p > 0.0) {
        hhi += (p * p);
        entropia -= (p * (Math.log(p) / Math.log(2.0)));
      }
    }

    double entropiaNormalizada = (k > 1) ? (entropia / (Math.log(k) / Math.log(2.0))) : 0.0;
    List<String> termosRecorrentes = repositorioPerfil.buscarTermosRecorrentesAutor(idAutor, 8);

    PerfilTematicoLegislativo perfilTematico = new PerfilTematicoLegislativo(
        totalNormasComTopico,
        distTopicos,
        topicoDominante,
        pctDominante,
        hhi,
        entropiaNormalizada,
        termosRecorrentes
    );

    // 2. DIMENSÃO DE ATUAÇÃO LEGISLATIVA
    var dadoAtuacao = repositorioPerfil.buscarAtuacaoLegislativa(idAutor);
    double taxaCoautoria = dadoAtuacao.totalNormas() > 0
        ? (dadoAtuacao.normasColetivas() * 100.0 / dadoAtuacao.totalNormas())
        : 0.0;

    PerfilAtuacaoLegislativa perfilAtuacao = new PerfilAtuacaoLegislativa(
        dadoAtuacao.totalNormas(),
        dadoAtuacao.tiposNormas(),
        dadoAtuacao.normasIndividuais(),
        dadoAtuacao.normasColetivas(),
        taxaCoautoria,
        dadoAtuacao.mediaAutoresPorNorma(),
        dadoAtuacao.cargosExercidos(),
        dadoAtuacao.cargoMaisFrequente()
    );

    // 3. DIMENSÃO DE COLABORAÇÃO POLÍTICA
    int totalParceiros = repositorioPerfil.contarParceirosDistintos(idAutor);
    List<ParceriaPolitica> principaisParcerias = repositorioPerfil.buscarPrincipaisParcerias(idAutor, dadoAtuacao.totalNormas(), 10);
    Map<String, Integer> parceriasPorCargo = repositorioPerfil.buscarParceriasPorCargo(idAutor);
    double intensidadeMedia = principaisParcerias.isEmpty() ? 0.0
        : principaisParcerias.stream().mapToInt(ParceriaPolitica::normasCompartilhadas).average().orElse(0.0);

    PerfilColaboracaoPolitica perfilColaboracao = new PerfilColaboracaoPolitica(
        totalParceiros,
        principaisParcerias,
        parceriasPorCargo,
        intensidadeMedia
    );

    // 4. DIMENSÃO TEMPORAL E DE EVOLUÇÃO
    var dadoEvolucao = repositorioPerfil.buscarEvolucaoTemporal(idAutor);
    Map<Integer, Integer> prodAno = dadoEvolucao.producaoPorAno();

    int primeiroAno = prodAno.isEmpty() ? 0 : Collections.min(prodAno.keySet());
    int ultimoAno = prodAno.isEmpty() ? 0 : Collections.max(prodAno.keySet());
    int totalAnos = prodAno.size();

    int anoPico = 0;
    int volumePico = 0;
    for (var entry : prodAno.entrySet()) {
      if (entry.getValue() > volumePico) {
        volumePico = entry.getValue();
        anoPico = entry.getKey();
      }
    }

    double mediaAnual = totalAnos > 0
        ? prodAno.values().stream().mapToInt(Integer::intValue).average().orElse(0.0)
        : 0.0;

    Map<Integer, String> temaDominantePorAno = new LinkedHashMap<>();
    for (var entry : dadoEvolucao.temasPorAno().entrySet()) {
      int ano = entry.getKey();
      var temasDoAno = entry.getValue();
      String temaMaisFrequente = temasDoAno.entrySet().stream()
          .max(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey)
          .orElse("N/D");
      temaDominantePorAno.put(ano, temaMaisFrequente);
    }

    PerfilTemporalEvolucao perfilTemporal = new PerfilTemporalEvolucao(
        primeiroAno,
        ultimoAno,
        totalAnos,
        prodAno,
        anoPico,
        volumePico,
        mediaAnual,
        dadoEvolucao.temasPorAno(),
        temaDominantePorAno
    );

    return Optional.of(new PerfilPoliticoAutor(
        info.id(),
        info.nome(),
        info.cargoPrincipal() != null ? info.cargoPrincipal() : dadoAtuacao.cargoMaisFrequente(),
        perfilTematico,
        perfilAtuacao,
        perfilColaboracao,
        perfilTemporal
    ));
  }

  /**
   * Constrói o perfil político buscando o autor por nome.
   */
  public Optional<PerfilPoliticoAutor> gerarPerfilAutor(String nomeAutor) throws SQLException {
    var optId = repositorioPerfil.buscarIdAutorPorNome(nomeAutor);
    if (optId.isEmpty()) {
      return Optional.empty();
    }
    return gerarPerfilAutor(optId.get());
  }

  /**
   * Constrói os perfis dos N autores com maior volume de normas no acervo legislativo.
   */
  public List<PerfilPoliticoAutor> gerarPerfisPrincipaisAutores(int limite) throws SQLException {
    var maisProlificos = repositorioPerfil.listarAutoresMaisProlificos(limite);
    List<PerfilPoliticoAutor> lista = new ArrayList<>();
    for (var a : maisProlificos) {
      gerarPerfilAutor(a.id()).ifPresent(lista::add);
    }
    return lista;
  }

  /**
   * Constrói a Rede/Grafo Global de Colaboração Política e Coautoria.
   */
  public GrafoColaboracaoPolitica construirGrafoColaboracao(int limiteAutores, int pesoMinimoAresta) throws SQLException {
    var maisProlificos = repositorioPerfil.listarAutoresMaisProlificos(limiteAutores > 0 ? limiteAutores : 35);
    Map<Long, GrafoColaboracaoPolitica.NoAutor> mapaNos = new LinkedHashMap<>();

    for (var p : maisProlificos) {
      List<DistribuicaoTopico> dist = repositorioPerfil.buscarDistribuicaoTematica(p.id());
      String temaDominante = dist.isEmpty() ? "Geral" : dist.get(0).nomeTopico();
      int grau = repositorioPerfil.contarParceirosDistintos(p.id());

      mapaNos.put(p.id(), new GrafoColaboracaoPolitica.NoAutor(
          p.id(),
          p.nome(),
          p.cargo(),
          p.totalNormas(),
          temaDominante,
          grau
      ));
    }

    var arestasBrutas = repositorioPerfil.buscarParesColaboracaoGlobais(pesoMinimoAresta, 300);
    List<GrafoColaboracaoPolitica.ArestaColaboracao> arestas = new ArrayList<>();

    for (var ab : arestasBrutas) {
      // Manter arestas cujos autores estão no conjunto relevante
      if (mapaNos.containsKey(ab.id1()) && mapaNos.containsKey(ab.id2())) {
        List<String> topicosComum = repositorioPerfil.obterTopicosCompartilhados(ab.id1(), ab.id2(), 2);
        arestas.add(new GrafoColaboracaoPolitica.ArestaColaboracao(
            ab.id1(), ab.nome1(),
            ab.id2(), ab.nome2(),
            ab.peso(),
            topicosComum
        ));
      }
    }

    return new GrafoColaboracaoPolitica(new ArrayList<>(mapaNos.values()), arestas);
  }

  /**
   * Constrói o Grafo de Colaboração centrado em um autor específico (Ego Network).
   */
  public Optional<GrafoColaboracaoPolitica> construirGrafoColaboracaoAutor(long idAutor, int pesoMinimoAresta) throws SQLException {
    var optInfo = repositorioPerfil.buscarInfoBasicaAutor(idAutor);
    if (optInfo.isEmpty()) {
      return Optional.empty();
    }

    var info = optInfo.get();
    Map<Long, GrafoColaboracaoPolitica.NoAutor> nos = new LinkedHashMap<>();

    // Adiciona o autor central
    List<DistribuicaoTopico> distCentro = repositorioPerfil.buscarDistribuicaoTematica(idAutor);
    String temaCentro = distCentro.isEmpty() ? "Geral" : distCentro.get(0).nomeTopico();
    int grauCentro = repositorioPerfil.contarParceirosDistintos(idAutor);
    nos.put(idAutor, new GrafoColaboracaoPolitica.NoAutor(idAutor, info.nome(), info.cargoPrincipal(), 0, temaCentro, grauCentro));

    var arestasBrutas = repositorioPerfil.buscarParesColaboracaoDoAutor(idAutor, pesoMinimoAresta);
    List<GrafoColaboracaoPolitica.ArestaColaboracao> arestas = new ArrayList<>();

    for (var ab : arestasBrutas) {
      long idParceiro = ab.id2();
      if (!nos.containsKey(idParceiro)) {
        var optParceiro = repositorioPerfil.buscarInfoBasicaAutor(idParceiro);
        String cargo = optParceiro.map(RepositorioPerfilPolitico.InfoBasicaAutor::cargoPrincipal).orElse("N/D");
        List<DistribuicaoTopico> distP = repositorioPerfil.buscarDistribuicaoTematica(idParceiro);
        String temaP = distP.isEmpty() ? "Geral" : distP.get(0).nomeTopico();
        int grauP = repositorioPerfil.contarParceirosDistintos(idParceiro);

        nos.put(idParceiro, new GrafoColaboracaoPolitica.NoAutor(idParceiro, ab.nome2(), cargo, 0, temaP, grauP));
      }

      List<String> topicosComum = repositorioPerfil.obterTopicosCompartilhados(idAutor, idParceiro, 2);
      arestas.add(new GrafoColaboracaoPolitica.ArestaColaboracao(
          idAutor, info.nome(),
          idParceiro, ab.nome2(),
          ab.peso(),
          topicosComum
      ));
    }

    return Optional.of(new GrafoColaboracaoPolitica(new ArrayList<>(nos.values()), arestas));
  }

  /**
   * Compara o perfil político de dois autores distintos, calculando afinidade temática.
   */
  public Optional<ComparativoPerfisAutores> compararAutores(long idAutor1, long idAutor2) throws SQLException {
    var optP1 = gerarPerfilAutor(idAutor1);
    var optP2 = gerarPerfilAutor(idAutor2);

    if (optP1.isEmpty() || optP2.isEmpty()) {
      return Optional.empty();
    }

    PerfilPoliticoAutor p1 = optP1.get();
    PerfilPoliticoAutor p2 = optP2.get();

    // Cálculo da Similaridade Cosseno Temática
    Map<Integer, Double> vetor1 = new HashMap<>();
    Map<Integer, Double> vetor2 = new HashMap<>();

    for (DistribuicaoTopico dt : p1.perfilTematico().distribuicaoTopicos()) {
      vetor1.put(dt.idTopico(), dt.percentual());
    }
    for (DistribuicaoTopico dt : p2.perfilTematico().distribuicaoTopicos()) {
      vetor2.put(dt.idTopico(), dt.percentual());
    }

    double produtoEscalar = 0.0;
    double norma1 = 0.0;
    double norma2 = 0.0;

    for (var entry : vetor1.entrySet()) {
      double v1 = entry.getValue();
      norma1 += (v1 * v1);
      double v2 = vetor2.getOrDefault(entry.getKey(), 0.0);
      produtoEscalar += (v1 * v2);
    }
    for (double v2 : vetor2.values()) {
      norma2 += (v2 * v2);
    }

    double simCosseno = (norma1 > 0 && norma2 > 0)
        ? (produtoEscalar / (Math.sqrt(norma1) * Math.sqrt(norma2)))
        : 0.0;

    int normasCompartilhadas = repositorioPerfil.contarNormasCompartilhadas(idAutor1, idAutor2);

    String diagnostico;
    if (simCosseno >= 0.85) {
      diagnostico = "Forte Convergência Temática (Pautas e Prioridades Legislativas Quase Idênticas)";
    } else if (simCosseno >= 0.50) {
      diagnostico = "Convergência Moderada (Interseção em Temas Centrais, mas com Agendas Distintas)";
    } else {
      diagnostico = "Divergência de Pautas (Atuação em Esferas de Políticas Públicas Complementares/Distintas)";
    }

    return Optional.of(new ComparativoPerfisAutores(
        p1, p2, simCosseno, normasCompartilhadas, diagnostico
    ));
  }

  /**
   * Imprime no terminal o relatório formatado do autor.
   */
  public void exibirPerfilAutor(long idAutor) throws SQLException {
    var optPerfil = gerarPerfilAutor(idAutor);
    if (optPerfil.isEmpty()) {
      System.out.printf("Autor com ID #%d não localizado no banco de dados.%n", idAutor);
      return;
    }
    System.out.println(optPerfil.get().gerarRelatorioVisual());
  }

  public void exibirPerfilAutor(String nomeAutor) throws SQLException {
    var optPerfil = gerarPerfilAutor(nomeAutor);
    if (optPerfil.isEmpty()) {
      System.out.printf("Autor com nome correspondente a \"%s\" não localizado.%n", nomeAutor);
      return;
    }
    System.out.println(optPerfil.get().gerarRelatorioVisual());
  }

  /**
   * Imprime a lista dos principais autores com volume de normas e cargo.
   */
  public void listarTopAutoresFormatado(int limite) throws SQLException {
    var lista = repositorioPerfil.listarAutoresMaisProlificos(limite > 0 ? limite : 15);
    System.out.println("\n" + "=".repeat(95));
    System.out.printf("TOP %d AUTORES COM MAIOR PRODUÇÃO NORMATIVA NO ACERVO LEGISLATIVO%n", lista.size());
    System.out.println("=".repeat(95));
    System.out.printf("%-5s | %-34s | %-40s | %-12s%n", "ID", "Nome do Autor", "Cargo Principal", "Normas");
    System.out.println("-".repeat(95));

    for (var a : lista) {
      System.out.printf("#%-4d | %-34s | %-40s | %5d normas%n",
          a.id(), a.nome(),
          a.cargo() != null ? a.cargo() : "AUTOR / VEREADOR",
          a.totalNormas());
    }
    System.out.println("=".repeat(95));
  }

  /**
   * Exporta o grafo em formato Graphviz (.dot) em arquivo.
   */
  public File exportarGrafoDot(GrafoColaboracaoPolitica grafo, String caminhoArquivo, int pesoMinimo) throws IOException {
    File f = new File(caminhoArquivo);
    if (f.getParentFile() != null) {
      f.getParentFile().mkdirs();
    }
    try (FileWriter writer = new FileWriter(f, StandardCharsets.UTF_8)) {
      writer.write(grafo.exportarDot("RedeLegislativa", pesoMinimo));
    }
    return f;
  }

  /**
   * Exporta o grafo em formato JSON estruturado (.json) para bibliotecas de visualização web (D3/Vis.js).
   */
  public File exportarGrafoJson(GrafoColaboracaoPolitica grafo, String caminhoArquivo, int pesoMinimo) throws IOException {
    File f = new File(caminhoArquivo);
    if (f.getParentFile() != null) {
      f.getParentFile().mkdirs();
    }
    try (FileWriter writer = new FileWriter(f, StandardCharsets.UTF_8)) {
      writer.write(grafo.exportarJsonD3(pesoMinimo));
    }
    return f;
  }

  /**
   * Gera uma página web completa e autocontida (HTML + CSS + JS) para visualização offline interativa do grafo.
   */
  public File gerarPaginaWebInterativa(File destino) throws SQLException, IOException {
    if (destino.getParentFile() != null) {
      destino.getParentFile().mkdirs();
    }

    var grafo = construirGrafoColaboracao(40, 15);
    String jsonGrafo = grafo.exportarJsonD3(15);
    var topAutores = gerarPerfisPrincipaisAutores(40);

    StringBuilder jsonAutores = new StringBuilder("[");
    for (int i = 0; i < topAutores.size(); i++) {
      var a = topAutores.get(i);
      String nome = a.nomeAutor() != null ? a.nomeAutor().replace("\"", "\\\"") : "";
      String cargo = a.cargoPrincipal() != null ? a.cargoPrincipal().replace("\"", "\\\"") : "";
      jsonAutores.append(String.format("{\"id\":%d,\"name\":\"%s\",\"cargo\":\"%s\",\"totalNormas\":%d}",
          a.idAutor(), nome, cargo, a.perfilAtuacao().totalNormas()));
      if (i < topAutores.size() - 1) jsonAutores.append(",");
    }
    jsonAutores.append("]");

    String html = org.entryPoint.service.web.PaginaWebGrafoTemplate.gerarHtml(jsonGrafo, jsonAutores.toString(), false);
    try (FileWriter writer = new FileWriter(destino, StandardCharsets.UTF_8)) {
      writer.write(html);
    }
    return destino;
  }
}
