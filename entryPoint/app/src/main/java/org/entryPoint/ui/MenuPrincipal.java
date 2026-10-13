package org.entryPoint.ui;

import java.sql.SQLException;
import java.util.Scanner;

import org.entryPoint.service.ServicoAutores;
import org.entryPoint.service.ServicoNormas;
import org.entryPoint.service.ServicoNormalizacaoCargo;
import org.entryPoint.service.ServicoRelacionamentoNormas;
import org.entryPoint.service.ServicoTopicModeling;
import org.entryPoint.service.preprocessamento.ServicoPreProcessamentoEmentas;

public class MenuPrincipal {
  private static final Scanner sc = new Scanner(System.in);
  private static final ServicoNormas servicoNormas = new ServicoNormas();
  private static final ServicoAutores servicoAutores = new ServicoAutores();
  private static final ServicoNormalizacaoCargo servicoNormalizacaoCargo = new ServicoNormalizacaoCargo();
  private static final ServicoPreProcessamentoEmentas servicoPreProcessamento = new ServicoPreProcessamentoEmentas();
  private static final ServicoRelacionamentoNormas servicoRelacionamentos = new ServicoRelacionamentoNormas();
  private static final ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
  private static final org.entryPoint.benchmark.ServicoBenchmark servicoBenchmark = new org.entryPoint.benchmark.ServicoBenchmark();

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
    System.out.println("7. Executar Topic Modeling nas normas (Baseline NMF K=12)");
    System.out.println("8. Executar Benchmark e Definir Melhor Pipeline (Reduções, Modelos e Perfis de Autores)");
    System.out.println("9. Visualizar Pipeline Ativo Oficial para os Próximos Passos");
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
            servicoTopicModeling.executarTopicModeling();
          } catch (SQLException e) {
            System.err.println("Erro ao executar Topic Modeling: " + e.getMessage());
          }
          pularLinha();

          break;

        case 8:
          try {
            System.out.println("Escolha o modo de execução do Benchmark:");
            System.out.println("1. Rápido (amostra de 1.500 normas para feedback ágil)");
            System.out.println("2. Completo (todas as 14k+ normas do banco)");
            System.out.print("Opção: ");
            String modoStr = sc.nextLine();
            var relatorio = "2".equals(modoStr.trim())
                ? servicoBenchmark.executarBenchmarkCompleto()
                : servicoBenchmark.executarBenchmarkRapido();

            System.out.print("\nDeseja DEFINIR E FIXAR esta melhor combinação como o Pipeline Oficial para os próximos passos? (S/n): ");
            String resp = sc.nextLine();
            if (!"n".equalsIgnoreCase(resp.trim()) && !"nao".equalsIgnoreCase(resp.trim())) {
              servicoBenchmark.definirEFixarMelhorPipeline(relatorio);
            }
          } catch (SQLException e) {
            System.err.println("Erro ao executar Benchmark: " + e.getMessage());
            e.printStackTrace();
          }
          pularLinha();

          break;

        case 9:
          try {
            var optPipeline = servicoBenchmark.getServicoPipeline().obterPipelineAtivo();
            if (optPipeline.isPresent()) {
              System.out.println(optPipeline.get().getFormatacaoVisual());
            } else {
              System.out.println("Nenhum pipeline ativo definido ainda.");
              System.out.println("Execute a opção 8 para rodar o benchmark e eleger o melhor pipeline.");
            }
          } catch (SQLException e) {
            System.err.println("Erro ao consultar pipeline ativo: " + e.getMessage());
          }
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
}
