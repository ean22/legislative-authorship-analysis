package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.service.agent.AgenteGemini;

public class ServicoNormalizacaoCargo {

  private final RepositorioBancoDados repositorio = new RepositorioBancoDados();
  private final AgenteGemini agenteGemini = new AgenteGemini();

  public void normalizarCargos() {
    System.out.println("""
      \n======================================================
         NORMALIZAÇÃO DE CARGOS COM AGENTE GEMINI (FREE TIER)
      ======================================================
      """);

    if (!agenteGemini.isConfigurado()) {
      System.err.println("""
        [ERRO] A variável de ambiente GEMINI_API_KEY não está configurada.
        
        Para utilizar o serviço, defina sua chave gratuita do Google AI Studio:
        export GEMINI_API_KEY="sua_chave_aqui"
        
        Em seguida, execute a aplicação novamente.
        """);
      return;
    }

    try {
      System.out.println("[1/4] Verificando cargos pendentes na tabela norma_autor...");
      List<String> cargosPendentes = repositorio.listarCargosPendentesNormalizacao();

      if (cargosPendentes.isEmpty()) {
        System.out.println("Todos os cargos da tabela norma_autor já foram normalizados!");
        return;
      }

      System.out.printf("Encontrados %d cargos distintos pendentes de normalização.%n", cargosPendentes.size());
      System.out.println("\n[2/4] Enviando cargos para o " + agenteGemini.getNome() + "...");

      long inicio = System.currentTimeMillis();
      
      // Normalização com persistência progressiva a cada lote
      Map<String, String> mapaNormalizacao = agenteGemini.normalizarCargos(cargosPendentes, loteNormalizado -> {
        int linhas = repositorio.atualizarCargosNormaAutor(loteNormalizado);
        System.out.printf("  ↳ %d linhas atualizadas no banco com este lote.%n", linhas);
      });
      
      long duracao = System.currentTimeMillis() - inicio;

      List<Map.Entry<String, String>> alteracoes = new ArrayList<>();
      Set<String> cargosNormalizadosUnicos = new HashSet<>();

      for (Map.Entry<String, String> entry : mapaNormalizacao.entrySet()) {
        String orig = entry.getKey();
        String norm = entry.getValue();
        cargosNormalizadosUnicos.add(norm);
        if (orig != null && norm != null && !orig.equals(norm)) {
          alteracoes.add(entry);
        }
      }

      System.out.println("\n[3/4] Amostra de normalizações realizadas pelo Gemini:");
      System.out.println("----------------------------------------------------------------------------------------------------------");
      System.out.printf("%-50s -> %-50s%n", "ORIGINAL", "NORMALIZADO (cargo_autor_normalizado)");
      System.out.println("----------------------------------------------------------------------------------------------------------");

      int limiteAmostra = Math.min(15, alteracoes.size());
      for (int i = 0; i < limiteAmostra; i++) {
        Map.Entry<String, String> par = alteracoes.get(i);
        System.out.printf("%-50s -> %-50s%n",
          abreviar(par.getKey(), 48),
          abreviar(par.getValue(), 48)
        );
      }

      System.out.println("----------------------------------------------------------------------------------------------------------");
      System.out.printf("Total de padrões modificados: %d de %d pendentes.%n", alteracoes.size(), cargosPendentes.size());
      System.out.printf("Total de cargos únicos consolidados: %d (de %d originais).%n",
        cargosNormalizadosUnicos.size(), cargosPendentes.size());

      System.out.printf("""
        \n======================================================
           NORMALIZAÇÃO CONCLUÍDA COM SUCESSO!
        ======================================================
        - Agente: %s
        - Tempo total: %.2f segundos
        - Padrões normalizados: %d
        - Coluna gravada: cargo_autor_normalizado (cargo_autor original preservado)
        - Economia de cotas: Processado respeitando limites de 5 RPM / 250k TPM / 20 RPD
        ======================================================\n
        """,
        agenteGemini.getNome(),
        duracao / 1000.0,
        alteracoes.size()
      );

    } catch (SQLException e) {
      System.err.println("Erro no banco de dados durante a normalização: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Erro durante o processamento do Agente Gemini: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private String abreviar(String texto, int maxLen) {
    if (texto == null) return "";
    if (texto.length() <= maxLen) return texto;
    return texto.substring(0, maxLen - 3) + "...";
  }
}
