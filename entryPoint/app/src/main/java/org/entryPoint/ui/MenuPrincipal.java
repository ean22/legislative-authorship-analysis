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
    System.out.println("5. Pré-processar ementas (Redução Textual TextRank - Pipeline Vencedor)");
    System.out.println("6. Extrair relacionamentos entre normas (links na íntegra)");
    System.out.println("7. Topic Modeling e Auditoria Experimental (Pipeline, Clusters, Centroides, c-TF-IDF, NMF)");
    System.out.println("8. Executar Benchmark e Comparar Pipelines (Reduções, Modelos e Perfis de Autores)");
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
          controlarMenuTopicModeling();
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

  private static void controlarMenuTopicModeling() {
    int subOpcao = 0;
    do {
      System.out.println("=".repeat(85));
      System.out.println("   MÓDULO DE TOPIC MODELING E AUDITORIA EXPERIMENTAL");
      System.out.println("=".repeat(85));
      System.out.println("1. Executar Pipeline Completo Oficial (Cluster c-TF-IDF K=8 e Persistir no Banco)");
      System.out.println("2. Etapa de Agrupamento Customizado (Definir número de clusters K e Iterações)");
      System.out.println("3. Exibir Centroides de Cada Cluster (Vetor Médio, Coesão e Norma Medoide)");
      System.out.println("4. Exibir Resultado Detalhado do c-TF-IDF (Scores dos Termos por Cluster)");
      System.out.println("5. Executar e Auditar NMF (Baseline Comparativo: Matrizes W e H, Coerência e Diversidade)");
      System.out.println("6. Auditoria de Classificações no Banco (Volumetria, % e Confiança por Tópico)");
      System.out.println("7. Inspecionar Amostra de Normas por Tópico (Validação Humana de Ementas Reais)");
      System.out.println("0. Voltar ao Menu Principal");
      System.out.print("Escolha uma opção: ");

      try {
        String entrada = sc.nextLine().trim();
        if (entrada.isEmpty()) {
          continue;
        }
        subOpcao = Integer.parseInt(entrada);
      } catch (NumberFormatException e) {
        System.out.println("Entrada inválida. Por favor, insira um número.");
        pularLinha();
        continue;
      }

      switch (subOpcao) {
        case 1:
          try {
            servicoTopicModeling.executarTopicModeling();
          } catch (SQLException e) {
            System.err.println("Erro ao executar Topic Modeling: " + e.getMessage());
          }
          pularLinha();
          break;

        case 2:
          try {
            System.out.print("Digite o número de clusters desejado (K) [padrão: 8]: ");
            String kStr = sc.nextLine().trim();
            int k = kStr.isEmpty() ? 8 : Integer.parseInt(kStr);
            if (k <= 1 || k > 50) {
              System.out.println("K deve estar entre 2 e 50. Usando K=8.");
              k = 8;
            }

            System.out.print("Digite o número máximo de iterações do K-Means [padrão: 35]: ");
            String iterStr = sc.nextLine().trim();
            int iter = iterStr.isEmpty() ? 35 : Integer.parseInt(iterStr);

            System.out.print("Deseja salvar esta classificação customizada no banco de dados? (s/N): ");
            String salvarStr = sc.nextLine().trim();
            boolean salvar = "s".equalsIgnoreCase(salvarStr) || "sim".equalsIgnoreCase(salvarStr);

            servicoTopicModeling.executarTopicModeling(k, iter, salvar);
          } catch (NumberFormatException e) {
            System.err.println("Valor numérico inválido informado.");
          } catch (SQLException e) {
            System.err.println("Erro ao executar agrupamento: " + e.getMessage());
          }
          pularLinha();
          break;

        case 3:
          try {
            servicoTopicModeling.exibirRelatorioCentroides();
          } catch (SQLException e) {
            System.err.println("Erro ao exibir centroides: " + e.getMessage());
          }
          pularLinha();
          break;

        case 4:
          try {
            System.out.print("Quantos termos por tópico deseja visualizar? [padrão: 8]: ");
            String tStr = sc.nextLine().trim();
            int topTermos = tStr.isEmpty() ? 8 : Integer.parseInt(tStr);
            servicoTopicModeling.exibirRelatorioCTfIdf(topTermos);
          } catch (NumberFormatException e) {
            System.err.println("Valor inválido. Exibindo top 8 termos.");
            try {
              servicoTopicModeling.exibirRelatorioCTfIdf(8);
            } catch (SQLException ex) {
              System.err.println("Erro: " + ex.getMessage());
            }
          } catch (SQLException e) {
            System.err.println("Erro ao exibir resultado c-TF-IDF: " + e.getMessage());
          }
          pularLinha();
          break;

        case 5:
          try {
            System.out.print("Digite o número de tópicos (K) para o NMF [padrão: 8]: ");
            String kStr = sc.nextLine().trim();
            int k = kStr.isEmpty() ? 8 : Integer.parseInt(kStr);

            System.out.print("Digite o número máximo de iterações do NMF [padrão: 30]: ");
            String iterStr = sc.nextLine().trim();
            int iter = iterStr.isEmpty() ? 30 : Integer.parseInt(iterStr);

            servicoTopicModeling.executarAuditoriaNmf(k, iter);
          } catch (NumberFormatException e) {
            System.err.println("Valor inválido. Executando NMF com K=8.");
            try {
              servicoTopicModeling.executarAuditoriaNmf(8, 30);
            } catch (SQLException ex) {
              System.err.println("Erro: " + ex.getMessage());
            }
          } catch (SQLException e) {
            System.err.println("Erro ao executar auditoria NMF: " + e.getMessage());
          }
          pularLinha();
          break;

        case 6:
          try {
            servicoTopicModeling.auditarClassificacoesBanco();
          } catch (SQLException e) {
            System.err.println("Erro ao auditar banco: " + e.getMessage());
          }
          pularLinha();
          break;

        case 7:
          try {
            System.out.print("Digite o ID do tópico a inspecionar (ex: 1 a 8): ");
            String idStr = sc.nextLine().trim();
            int idTopico = Integer.parseInt(idStr);

            System.out.print("Quantas normas de exemplo deseja listar? [padrão: 5]: ");
            String limStr = sc.nextLine().trim();
            int limite = limStr.isEmpty() ? 5 : Integer.parseInt(limStr);

            servicoTopicModeling.inspecionarAmostraNormasPorTopico(idTopico, limite);
          } catch (NumberFormatException e) {
            System.err.println("ID ou limite inválido.");
          } catch (SQLException e) {
            System.err.println("Erro ao inspecionar normas: " + e.getMessage());
          }
          pularLinha();
          break;

        case 0:
          System.out.println("Retornando ao menu principal...");
          pularLinha();
          break;

        default:
          exibirOpcaoInvalida();
          break;
      }
    } while (subOpcao != 0);
  }
}
