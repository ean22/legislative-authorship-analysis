package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.entryPoint.model.NormaTopico;
import org.entryPoint.model.PerfilAutor;
import org.entryPoint.repository.RepositorioBancoDados;

public class ServicoPerfilAutores {

  private final RepositorioBancoDados repositorio;

  public ServicoPerfilAutores() {
    this.repositorio = new RepositorioBancoDados();
  }

  public ServicoPerfilAutores(RepositorioBancoDados repositorio) {
    this.repositorio = repositorio;
  }

  /**
   * Gera o perfil temático de todos os autores que possuem normas classificadas no banco.
   */
  public List<PerfilAutor> gerarPerfisAutores(int minNormas) throws SQLException {
    Map<Long, NormaTopico> mapaNormasTopicos = repositorio.listarNormasTopicosComoMapa();
    if (mapaNormasTopicos.isEmpty()) {
      System.out.println("Nenhum tópico encontrado na tabela 'norma_topicos'. Execute o Topic Modeling primeiro.");
      return List.of();
    }

    Map<Long, String> nomesAutores = repositorio.listarNomesAutores();
    Map<Long, String> cargosAutores = repositorio.listarCargosNormalizadosAutores();
    Map<Long, List<Long>> autorParaNormas = repositorio.listarAutoresComNormas();

    List<PerfilAutor> perfis = new ArrayList<>();

    for (Map.Entry<Long, List<Long>> entry : autorParaNormas.entrySet()) {
      long idAutor = entry.getKey();
      List<Long> idNormas = entry.getValue();

      if (idNormas.size() < minNormas) {
        continue;
      }

      String nome = nomesAutores.getOrDefault(idAutor, "Autor #" + idAutor);
      String cargo = cargosAutores.getOrDefault(idAutor, "Não especificado");

      Map<Integer, Long> contagemPorTopico = new HashMap<>();
      Map<Integer, String> rotulosTopicos = new HashMap<>();
      long totalClassificadas = 0;

      for (Long idNorma : idNormas) {
        NormaTopico nt = mapaNormasTopicos.get(idNorma);
        if (nt != null) {
          contagemPorTopico.put(nt.idTopico(), contagemPorTopico.getOrDefault(nt.idTopico(), 0L) + 1L);
          rotulosTopicos.put(nt.idTopico(), nt.rotuloTopico());
          totalClassificadas++;
        }
      }

      if (totalClassificadas == 0) {
        continue;
      }

      // Distribuição percentual e cálculo de Entropia de Shannon
      Map<Integer, Double> distribuicaoPct = new LinkedHashMap<>();
      double entropia = 0.0;
      int topicoPredominanteId = -1;
      long maxContagem = -1;

      for (Map.Entry<Integer, Long> cEntry : contagemPorTopico.entrySet()) {
        int tId = cEntry.getKey();
        long cnt = cEntry.getValue();
        double p = (double) cnt / totalClassificadas;
        distribuicaoPct.put(tId, p * 100.0);

        if (p > 0) {
          entropia -= p * (Math.log(p) / Math.log(2));
        }

        if (cnt > maxContagem) {
          maxContagem = cnt;
          topicoPredominanteId = tId;
        }
      }

      // Normalizar entropia entre 0 e 1 dividindo por log2(K)
      double maxEntropiaPossivel = Math.log(Math.max(2, contagemPorTopico.size())) / Math.log(2);
      double entropiaNormalizada = maxEntropiaPossivel > 0 ? (entropia / maxEntropiaPossivel) : 0.0;

      String rotuloPredominante = rotulosTopicos.getOrDefault(topicoPredominanteId, "Geral");

      perfis.add(new PerfilAutor(
        idAutor,
        nome,
        cargo,
        idNormas.size(),
        totalClassificadas,
        contagemPorTopico,
        distribuicaoPct,
        entropiaNormalizada,
        rotuloPredominante
      ));
    }

    // Ordenar por total de normas decrescente
    perfis.sort((a, b) -> Long.compare(b.totalNormas(), a.totalNormas()));
    return perfis;
  }

  /**
   * Imprime no terminal o relatório formatado dos perfis dos autores.
   */
  public void exibirRelatorioPerfis(int minNormas, int limiteExibicao) throws SQLException {
    List<PerfilAutor> perfis = gerarPerfisAutores(minNormas);
    if (perfis.isEmpty()) {
      return;
    }

    System.out.println("\n" + "=".repeat(85));
    System.out.printf("=== RELATÓRIO DE PERFIL DE ATUAÇÃO TEMÁTICA DOS AUTORES (Min: %d normas) ===%n", minNormas);
    System.out.println("=".repeat(85));

    int count = 0;
    for (PerfilAutor perfil : perfis) {
      if (count++ >= limiteExibicao) break;

      System.out.printf("%n👤 Autor: %s [%s]%n", perfil.nomeAutor(), perfil.cargoNormalizado());
      System.out.printf("   Total Normas: %d | Classificadas: %d | Foco: %s (Entropia: %.2f - %s)%n",
        perfil.totalNormas(),
        perfil.totalNormasClassificadas(),
        perfil.topicoPredominante(),
        perfil.entropiaEspecializacao(),
        perfil.getClassificacaoAtuacao()
      );

      // Listar tópicos ordenados por relevância
      List<Map.Entry<Integer, Double>> ordenados = new ArrayList<>(perfil.distribuicaoPercentual().entrySet());
      ordenados.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

      for (Map.Entry<Integer, Double> entry : ordenados) {
        int tId = entry.getKey();
        double pct = entry.getValue();
        long qtd = perfil.contagemPorTopico().getOrDefault(tId, 0L);
        int barLength = (int) (pct / 4);
        String bar = "█".repeat(Math.max(1, barLength));
        System.out.printf("   %-25s %5.1f%% | Tópico #%02d (%d normas)%n", bar, pct, tId, qtd);
      }
    }
  }
}
