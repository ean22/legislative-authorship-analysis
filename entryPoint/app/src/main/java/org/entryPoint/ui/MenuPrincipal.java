package org.entryPoint.ui;

import java.sql.SQLException;
import java.util.Scanner;

import java.io.File;
import org.entryPoint.model.perfil.PerfilPoliticoAutor;
import org.entryPoint.service.ServicoAutores;
import org.entryPoint.service.ServicoNormas;
import org.entryPoint.service.ServicoNormalizacaoCargo;
import org.entryPoint.service.ServicoPerfilPolitico;
import org.entryPoint.service.ServicoRelacionamentoNormas;
import org.entryPoint.service.ServicoTopicModeling;
import org.entryPoint.service.preprocessamento.ServicoPreProcessamentoEmentas;
import org.entryPoint.service.web.ServidorWebGrafos;

public class MenuPrincipal {
  private static final Scanner sc = new Scanner(System.in);
  private static final ServicoNormas servicoNormas = new ServicoNormas();
  private static final ServicoAutores servicoAutores = new ServicoAutores();
  private static final ServicoNormalizacaoCargo servicoNormalizacaoCargo = new ServicoNormalizacaoCargo();
  private static final ServicoPreProcessamentoEmentas servicoPreProcessamento = new ServicoPreProcessamentoEmentas();
  private static final ServicoRelacionamentoNormas servicoRelacionamentos = new ServicoRelacionamentoNormas();
  private static final ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
  private static final ServicoPerfilPolitico servicoPerfilPolitico = new ServicoPerfilPolitico();
  private static final ServidorWebGrafos servidorWeb = new ServidorWebGrafos(servicoPerfilPolitico);
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
    System.out.println("8. Perfis Políticos e Grafos de Colaboração (Temas, Atuação, Parcerias, Trajetória, Redes)");
    System.out.println("9. Executar Benchmark e Comparar Pipelines (Reduções, Modelos e Perfis de Autores)");
    System.out.println("10. Visualizar Pipeline Ativo Oficial para os Próximos Passos");
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
          controlarMenuPerfisPoliticos();
          pularLinha();

          break;

        case 9:
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

        case 10:
          try {
            var optPipeline = servicoBenchmark.getServicoPipeline().obterPipelineAtivo();
            if (optPipeline.isPresent()) {
              System.out.println(optPipeline.get().getFormatacaoVisual());
            } else {
              System.out.println("Nenhum pipeline ativo definido ainda.");
              System.out.println("Execute a opção 9 para rodar o benchmark e eleger o melhor pipeline.");
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
      System.out.println("8. Informações da Estratégia de Rotulação de Tópicos (LLM Gemini + Fallback Regras Léxicas)");
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

        case 8:
          System.out.println("\n" + "=".repeat(85));
          System.out.println("   ESTRATÉGIA UNIFICADA DE ROTULAÇÃO DE TÓPICOS");
          System.out.println("=".repeat(85));
          System.out.println("O sistema utiliza um fluxo único e resiliente:");
          System.out.println("  1. Primário: LLM Google Gemini (raciocínio semântico contextual em lote)");
          System.out.println("  2. Fallback Automático: Regras Léxicas e Posição (ativado se GEMINI_API_KEY ausente ou erro)");
          pularLinha();
          boolean geminiOk = new org.entryPoint.service.agent.AgenteGemini().isConfigurado();
          System.out.printf("Status da Chave GEMINI_API_KEY: %s%n",
              geminiOk ? "[CONFIGURADA] LLM Google Gemini ativo para rotulação semântica."
                       : "[NÃO CONFIGURADA] O sistema usará automaticamente o fallback de Regras Léxicas.");
          System.out.println("Sempre que o Topic Modeling for executado, o prompt avisa qual estratégia foi usada e o motivo.");
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

  private static void controlarMenuPerfisPoliticos() {
    int subOpcao = 0;
    do {
      System.out.println("=".repeat(85));
      System.out.println("   MÓDULO DE PERFIS POLÍTICOS E GRAFOS DE COLABORAÇÃO LEGISLATIVA");
      System.out.println("=".repeat(85));
      System.out.println("1. Consultar Perfil Político Completo de um Autor (Por ID ou Nome)");
      System.out.println("2. Listar Top Autores com Maior Produção Legislativa");
      System.out.println("3. Exibir Grafo de Colaboração Política no Console (Hubs e Laços Mais Fortes)");
      System.out.println("4. Exibir Grafo Centrado em um Autor Específico (Ego-Network)");
      System.out.println("5. Exportar Grafo de Colaboração em Formato DOT (Graphviz)");
      System.out.println("6. Exportar Grafo de Colaboração em Formato JSON (D3.js / Vis.js)");
      System.out.println("7. Comparar Perfis Políticos de Dois Autores (Similaridade Temática Cosseno)");
      System.out.println("8. Abrir Interface Web Interativa no Navegador (Servidor Local http://localhost:8080)");
      System.out.println("9. Gerar Arquivo HTML Interativo Independente (Visualização Offline)");
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
            System.out.print("Digite o ID ou Nome do Autor (ex: 'FERNANDO HADDAD' ou 1731): ");
            String termo = sc.nextLine().trim();
            if (termo.isEmpty()) break;

            if (termo.matches("\\d+")) {
              servicoPerfilPolitico.exibirPerfilAutor(Long.parseLong(termo));
            } else {
              servicoPerfilPolitico.exibirPerfilAutor(termo);
            }
          } catch (SQLException e) {
            System.err.println("Erro ao consultar perfil do autor: " + e.getMessage());
          }
          pularLinha();
          break;

        case 2:
          try {
            System.out.print("Quantos autores deseja listar? [padrão: 15]: ");
            String limStr = sc.nextLine().trim();
            int limite = limStr.isEmpty() ? 15 : Integer.parseInt(limStr);
            servicoPerfilPolitico.listarTopAutoresFormatado(limite);
          } catch (NumberFormatException e) {
            try {
              servicoPerfilPolitico.listarTopAutoresFormatado(15);
            } catch (SQLException ex) {
              System.err.println("Erro: " + ex.getMessage());
            }
          } catch (SQLException e) {
            System.err.println("Erro ao listar autores: " + e.getMessage());
          }
          pularLinha();
          break;

        case 3:
          try {
            System.out.print("Quantos atores principais incluir no grafo? [padrão: 25]: ");
            String atoresStr = sc.nextLine().trim();
            int limiteAtores = atoresStr.isEmpty() ? 25 : Integer.parseInt(atoresStr);

            System.out.print("Peso mínimo de normas compartilhadas por aresta? [padrão: 20]: ");
            String pesoStr = sc.nextLine().trim();
            int pesoMin = pesoStr.isEmpty() ? 20 : Integer.parseInt(pesoStr);

            var grafo = servicoPerfilPolitico.construirGrafoColaboracao(limiteAtores, pesoMin);
            System.out.println(grafo.gerarVisualizacaoConsole(20));
          } catch (NumberFormatException e) {
            System.err.println("Parâmetro numérico inválido informado.");
          } catch (SQLException e) {
            System.err.println("Erro ao construir grafo: " + e.getMessage());
          }
          pularLinha();
          break;

        case 4:
          try {
            System.out.print("Digite o ID do autor central para o grafo ego-network: ");
            String idStr = sc.nextLine().trim();
            long idAutor = Long.parseLong(idStr);

            System.out.print("Peso mínimo de normas compartilhadas [padrão: 10]: ");
            String pesoStr = sc.nextLine().trim();
            int pesoMin = pesoStr.isEmpty() ? 10 : Integer.parseInt(pesoStr);

            var optGrafo = servicoPerfilPolitico.construirGrafoColaboracaoAutor(idAutor, pesoMin);
            if (optGrafo.isPresent()) {
              System.out.println(optGrafo.get().gerarVisualizacaoConsole(15));
            } else {
              System.out.println("Autor não localizado.");
            }
          } catch (NumberFormatException e) {
            System.err.println("ID ou peso inválido.");
          } catch (SQLException e) {
            System.err.println("Erro ao gerar ego-network: " + e.getMessage());
          }
          pularLinha();
          break;

        case 5:
          try {
            System.out.print("Informe o caminho do arquivo .dot [padrão: rede_colaboracao.dot]: ");
            String path = sc.nextLine().trim();
            if (path.isEmpty()) path = "rede_colaboracao.dot";

            var grafo = servicoPerfilPolitico.construirGrafoColaboracao(35, 15);
            File f = servicoPerfilPolitico.exportarGrafoDot(grafo, path, 15);
            System.out.println("Grafo exportado com sucesso em: " + f.getAbsolutePath());
            System.out.println("Dica: você pode gerar PNG com o comando: dot -Tpng " + path + " -o rede.png");
          } catch (Exception e) {
            System.err.println("Erro ao exportar DOT: " + e.getMessage());
          }
          pularLinha();
          break;

        case 6:
          try {
            System.out.print("Informe o caminho do arquivo .json [padrão: rede_colaboracao.json]: ");
            String path = sc.nextLine().trim();
            if (path.isEmpty()) path = "rede_colaboracao.json";

            var grafo = servicoPerfilPolitico.construirGrafoColaboracao(35, 15);
            File f = servicoPerfilPolitico.exportarGrafoJson(grafo, path, 15);
            System.out.println("Arquivo JSON exportado com sucesso em: " + f.getAbsolutePath());
          } catch (Exception e) {
            System.err.println("Erro ao exportar JSON: " + e.getMessage());
          }
          pularLinha();
          break;

        case 7:
          try {
            System.out.print("Digite o ID ou Nome do Autor 1 (ex: 'FERNANDO HADDAD'): ");
            String a1 = sc.nextLine().trim();
            System.out.print("Digite o ID ou Nome do Autor 2 (ex: 'RICARDO NUNES'): ");
            String a2 = sc.nextLine().trim();

            long id1 = a1.matches("\\d+") ? Long.parseLong(a1) : servicoPerfilPolitico.gerarPerfilAutor(a1).map(PerfilPoliticoAutor::idAutor).orElse(-1L);
            long id2 = a2.matches("\\d+") ? Long.parseLong(a2) : servicoPerfilPolitico.gerarPerfilAutor(a2).map(PerfilPoliticoAutor::idAutor).orElse(-1L);

            if (id1 <= 0 || id2 <= 0) {
              System.out.println("Não foi possível localizar um ou ambos os autores informados.");
              break;
            }

            var optComp = servicoPerfilPolitico.compararAutores(id1, id2);
            if (optComp.isPresent()) {
              System.out.println(optComp.get().gerarRelatorioComparativo());
            }
          } catch (Exception e) {
            System.err.println("Erro ao comparar perfis: " + e.getMessage());
          }
          pularLinha();
          break;

        case 8:
          try {
            if (!servidorWeb.isAtivo()) {
              int porta = servidorWeb.iniciar(8080);
              System.out.printf("🌐 Servidor Web Interativo INICIADO com sucesso na porta %d!%n", porta);
              System.out.printf("👉 Acesse no seu navegador: %s%n", servidorWeb.getUrlBase());
            } else {
              System.out.printf("🌐 Servidor Web já está em execução em: %s%n", servidorWeb.getUrlBase());
            }
            System.out.println("Abrindo navegador padrão...");
            servidorWeb.abrirNavegadorPadrao();
          } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor web: " + e.getMessage());
          }
          pularLinha();
          break;

        case 9:
          try {
            System.out.print("Informe o caminho do arquivo HTML [padrão: rede_interativa.html]: ");
            String path = sc.nextLine().trim();
            if (path.isEmpty()) path = "rede_interativa.html";
            File f = servicoPerfilPolitico.gerarPaginaWebInterativa(new File(path));
            System.out.println("✨ Arquivo HTML interativo gerado com sucesso!");
            System.out.println("📂 Localização: " + f.getAbsolutePath());
            System.out.println("👉 Você pode abrir este arquivo diretamente em qualquer navegador sem precisar de servidor!");
          } catch (Exception e) {
            System.err.println("Erro ao gerar página HTML: " + e.getMessage());
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
