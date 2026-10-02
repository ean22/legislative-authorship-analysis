package org.entryPoint.ui;

import java.sql.SQLException;
import java.util.Scanner;

import org.entryPoint.service.ServicoAutores;
import org.entryPoint.service.ServicoAvaliacaoClassificacao;
import org.entryPoint.service.ServicoNormas;
import org.entryPoint.service.ServicoNormalizacaoCargo;
import org.entryPoint.service.ServicoPerfilAutores;
import org.entryPoint.service.ServicoRelacionamentoNormas;
import org.entryPoint.service.ServicoTopicModeling;
import org.entryPoint.service.ServicoClusterizacaoEmbeddings;
import org.entryPoint.service.embeddings.GeradorEmbeddings;
import org.entryPoint.service.embeddings.GeradorEmbeddingsGemini;
import org.entryPoint.service.embeddings.GeradorEmbeddingsLocal;
import org.entryPoint.service.embeddings.ServicoEmbeddings;
import org.entryPoint.service.preprocessamento.ServicoPreProcessamentoEmentas;

public class MenuPrincipal {
  private static final Scanner sc = new Scanner(System.in);
  private static ServicoNormas servicoNormas = new ServicoNormas();
  private static ServicoAutores servicoAutores = new ServicoAutores();
  private static ServicoNormalizacaoCargo servicoNormalizacaoCargo = new ServicoNormalizacaoCargo();
  private static ServicoPreProcessamentoEmentas servicoPreProcessamento = new ServicoPreProcessamentoEmentas();
  private static ServicoRelacionamentoNormas servicoRelacionamentos = new ServicoRelacionamentoNormas();
  private static ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
  private static ServicoPerfilAutores servicoPerfilAutores = new ServicoPerfilAutores();
  private static ServicoAvaliacaoClassificacao servicoAvaliacao = new ServicoAvaliacaoClassificacao();
  private static ServicoEmbeddings servicoEmbeddings = new ServicoEmbeddings();
  private static ServicoClusterizacaoEmbeddings servicoClusterizacao = new ServicoClusterizacaoEmbeddings();

  private static ServicoTopicModeling.ResultadoTopicModeling ultimoResultadoTopicModeling = null;

  public static void exibirSaudacao() {
    System.out.println("Bem-vindo ao sistema de análise de autoria legislativa!");
    pularLinha();
  }

  public static void iniciar() {
    controlarMenu();
  }
  
  public static void exibirMenu() {
    System.out.println("Escolha uma opção:");
    System.out.println("1. Baixar normas");
    System.out.println("2. Testar leitura do banco");
    System.out.println("3. Extrair autores");
    System.out.println("4. Normalizar cargos (Google Gemini)");
    System.out.println("5. Pré-processar ementas (redução textual)");
    System.out.println("6. Extrair relacionamentos entre normas (links na íntegra)");
    System.out.println("7. Executar Topic Modeling nas normas (NMF)");
    System.out.println("8. Exibir perfil temático dos autores");
    System.out.println("9. Avaliar métricas de classificação e desempenho");
    System.out.println("10. Gerar embeddings das ementas (armazena em data/leis.db)");
    System.out.println("11. Clusterizar embeddings (K-Means) e exibir normas representativas");
    System.out.println("0. Sair");
  }

  public static void sair() {
    System.out.println("Saindo do sistema...");
    pularLinha();
  }

  public static void exibirOpcaoInvalida() {
    System.out.println("Opção inválida. Tente novamente.");
    pularLinha();
  }

  public static void exibirBaixandoNormas() {
    System.out.println("Baixando normas...");
    pularLinha();
  }

  public static void exibirListandoNormas() {
    System.out.println("Listando normas...");
    pularLinha();
  }

  public static void solicitarDataInicial() {
    System.out.print("Digite a data inicial (formato: YYYY-MM-DD): ");
  }

  public static void solicitarDataFinal() {
    System.out.print("Digite a data final (formato: YYYY-MM-DD): ");
  }

  private static void pularLinha() {
    System.out.println();
  }

  private static void controlarMenu() {
    int opcao = 0;

    do {
      exibirMenu();

    try { 
      String entrada = sc.nextLine();
      opcao = Integer.parseInt(entrada);
    } catch (NumberFormatException e) { 
      System.out.println("Entrada inválida. Por favor, insira um número."); 
      pularLinha();
      continue; 
    }

      switch (opcao) {
        case 1:
          exibirBaixandoNormas();

          solicitarDataInicial();
          String dataInicial = sc.next();
          pularLinha();

          solicitarDataFinal();
          String dataFinal = sc.next();
          pularLinha();

          servicoNormas.definirIntervaloDatas(dataInicial, dataFinal);
          servicoNormas.buscarNormas();

          break;

        case 2:
          exibirListandoNormas();
          servicoNormas.listarNormas();
          pularLinha();

          break;
        
        case 3:
          try {
            servicoAutores.extrairAutores();
          } catch (SQLException e) {
            e.printStackTrace();
          }
          pularLinha();

          break;

        case 4:
          servicoNormalizacaoCargo.normalizarCargos();
          pularLinha();

          break;

        case 5:
          servicoPreProcessamento.processarTodasEmentas();
          pularLinha();

          break;

        case 6:
          try {
            servicoRelacionamentos.extrairEPopularRelacionamentos();
          } catch (SQLException e) {
            e.printStackTrace();
          }
          pularLinha();

          break;

        case 7:
          try {
            ultimoResultadoTopicModeling = servicoTopicModeling.executarTopicModeling();
          } catch (SQLException e) {
            System.err.println("Erro ao executar Topic Modeling: " + e.getMessage());
          }
          pularLinha();

          break;

        case 8:
          try {
            servicoPerfilAutores.exibirRelatorioPerfis(50, 10);
          } catch (SQLException e) {
            System.err.println("Erro ao exibir perfis dos autores: " + e.getMessage());
          }
          pularLinha();

          break;

        case 9:
          try {
            long tempoMs = ultimoResultadoTopicModeling != null ? ultimoResultadoTopicModeling.tempoMs() : 17500L;
            long tokens = ultimoResultadoTopicModeling != null ? ultimoResultadoTopicModeling.totalTokens() : 68000L;
            long memoriaMb = ultimoResultadoTopicModeling != null ? ultimoResultadoTopicModeling.memoriaMb() : 38L;
            var metricas = servicoAvaliacao.avaliarDesempenho(tempoMs, tokens, memoriaMb);
            servicoAvaliacao.exibirRelatorioAvaliacao(metricas);
          } catch (SQLException e) {
            System.err.println("Erro ao avaliar métricas de classificação: " + e.getMessage());
          }
          pularLinha();

          break;

        case 10:
          executarGeracaoEmbeddings();
          pularLinha();

          break;

        case 11:
          executarClusterizacaoEmbeddings();
          pularLinha();

          break;

        case 0:
          sair();
          
          break;

        default:
          exibirOpcaoInvalida();

          break;
      }

    } while (opcao != 0);

    sc.close();
  }

  private static void executarGeracaoEmbeddings() {
    System.out.println("\n--- GERAÇÃO DE EMBEDDINGS DE EMENTAS ---");
    System.out.println("Escolha o provedor de embeddings:");
    System.out.println("1. Google Gemini (text-embedding-004) [Requer GEMINI_API_KEY]");
    System.out.println("2. Local Semantic Embeddings (128D, 100% offline e rápido)");
    System.out.print("Opção [Padrão: 2]: ");
    String optStr = sc.nextLine().trim();
    int opt = 2;
    try {
      if (!optStr.isBlank()) {
        opt = Integer.parseInt(optStr);
      }
    } catch (NumberFormatException ignored) {}

    GeradorEmbeddings gerador;
    if (opt == 1) {
      gerador = new GeradorEmbeddingsGemini();
      if (!gerador.isDisponivel()) {
        System.err.println("AVISO: GEMINI_API_KEY não foi encontrada no ambiente. Utilizando gerador local como fallback.");
        gerador = new GeradorEmbeddingsLocal();
      }
    } else {
      gerador = new GeradorEmbeddingsLocal();
    }

    System.out.print("Deseja gerar apenas para normas pendentes (sem embedding)? (S/n) [Padrão: S]: ");
    String pendentesStr = sc.nextLine().trim();
    boolean apenasPendentes = !pendentesStr.equalsIgnoreCase("n");

    System.out.print("Definir limite de normas a processar? (0 para todas) [Padrão: 0]: ");
    String limiteStr = sc.nextLine().trim();
    int limite = 0;
    try {
      if (!limiteStr.isBlank()) {
        limite = Integer.parseInt(limiteStr);
      }
    } catch (NumberFormatException ignored) {}

    try {
      servicoEmbeddings.gerarESalvarEmbeddings(gerador, apenasPendentes, limite);
    } catch (Exception e) {
      System.err.println("Erro durante a geração de embeddings: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void executarClusterizacaoEmbeddings() {
    System.out.println("\n--- CLUSTERIZAÇÃO DE EMBEDDINGS (K-MEANS) ---");
    System.out.print("Digite a quantidade de clusters desejada (k) [Padrão: 10]: ");
    String kStr = sc.nextLine().trim();
    int k = 10;
    try {
      if (!kStr.isBlank()) {
        k = Integer.parseInt(kStr);
      }
    } catch (NumberFormatException ignored) {}

    if (k <= 0) {
      System.out.println("Quantidade de clusters inválida. Usando k=10.");
      k = 10;
    }

    try {
      var resultado = servicoClusterizacao.clusterizar(k);
      servicoClusterizacao.exibirRelatorioClusterizacao(resultado);
    } catch (SQLException e) {
      System.err.println("Erro ao executar clusterização: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
