package org.entryPoint.service.embeddings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.entryPoint.model.EmbeddingNorma;
import org.entryPoint.model.NormaComEmenta;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;

/**
 * Serviço responsável pela geração de embeddings das ementas legislativas
 * e sua persistência na coluna 'embeddings' da tabela 'ementas_pre_processadas'.
 */
public class ServicoEmbeddings {

  private final RepositorioEmentasPreProcessadas repositorioEmentas;

  public ServicoEmbeddings() {
    this.repositorioEmentas = new RepositorioEmentasPreProcessadas();
  }

  public ServicoEmbeddings(RepositorioEmentasPreProcessadas repositorioEmentas) {
    this.repositorioEmentas = repositorioEmentas;
  }

  public ServicoEmbeddings(RepositorioBancoDados repositorio) {
    this.repositorioEmentas = repositorio.getRepositorioEmentas();
  }

  /**
   * Identifica o gerador padrão disponível (Gemini se configurado, ou Local offline).
   */
  public GeradorEmbeddings obterGeradorPadrao() {
    GeradorEmbeddingsGemini gemini = new GeradorEmbeddingsGemini();
    if (gemini.isDisponivel()) {
      return gemini;
    }
    return new GeradorEmbeddingsLocal();
  }

  /**
   * Gera e salva embeddings para as ementas da base de dados.
   *
   * @param gerador         Instância do gerador (Gemini ou Local)
   * @param apenasPendentes Se true, processa apenas normas que ainda não possuem embeddings
   * @param limite          Limite de normas a processar (<= 0 para todas)
   * @return Quantidade de embeddings gerados e salvos com sucesso
   */
  public int gerarESalvarEmbeddings(GeradorEmbeddings gerador, boolean apenasPendentes, int limite) throws Exception {
    long inicioTempo = System.currentTimeMillis();

    System.out.println("\n" + "=".repeat(80));
    System.out.println("=== GERAÇÃO E ARMAZENAMENTO DE EMBEDDINGS DE EMENTAS ===");
    System.out.println("=".repeat(80));
    System.out.printf("Gerador selecionado: %s%n", gerador.getNome());
    System.out.printf("Dimensões do vetor:  %d%n", gerador.getDimensao());
    System.out.printf("Modo:                %s%n", apenasPendentes ? "Apenas pendentes (sem embedding)" : "Todas as normas");

    List<NormaComEmenta> normas = repositorioEmentas.listarNormasParaGeracaoEmbedding(apenasPendentes, limite);
    if (normas.isEmpty()) {
      System.out.println("Nenhuma norma pendente de embedding para processamento.");
      return 0;
    }

    System.out.printf("Total de ementas a processar: %d%n", normas.size());

    int tamanhoLoteBanco = 250;
    int tamanhoLoteGerador = (gerador instanceof GeradorEmbeddingsGemini) ? 100 : 500;

    int totalProcessadas = 0;
    Map<Long, String> loteParaPersistir = new LinkedHashMap<>();

    for (int i = 0; i < normas.size(); i += tamanhoLoteGerador) {
      int fim = Math.min(i + tamanhoLoteGerador, normas.size());
      List<NormaComEmenta> subLista = normas.subList(i, fim);

      List<String> textos = subLista.stream()
          .map(n -> n.ementa() != null ? n.ementa() : "")
          .toList();

      List<float[]> vetores = gerador.gerarEmbeddingsEmLote(textos);

      for (int j = 0; j < subLista.size(); j++) {
        long idNorma = subLista.get(j).id();
        float[] vetor = vetores.get(j);
        String jsonVetor = RepositorioEmentasPreProcessadas.formatFloatArrayToJson(vetor);
        loteParaPersistir.put(idNorma, jsonVetor);
      }

      totalProcessadas += subLista.size();

      if (loteParaPersistir.size() >= tamanhoLoteBanco || fim >= normas.size()) {
        repositorioEmentas.salvarEmbeddingsEmLote(loteParaPersistir);
        loteParaPersistir.clear();
        double pct = (totalProcessadas * 100.0) / normas.size();
        System.out.printf("Progresso: %d/%d (%.1f%%) embeddings persistidos em data/leis.db...%n",
            totalProcessadas, normas.size(), pct);
      }
    }

    long duracaoMs = System.currentTimeMillis() - inicioTempo;
    long totalNoBanco = repositorioEmentas.contarEmbeddings();

    System.out.println("=".repeat(80));
    System.out.printf("Embeddings salvos com sucesso!%n");
    System.out.printf("Tempo total:           %d ms (%.2f s)%n", duracaoMs, duracaoMs / 1000.0);
    System.out.printf("Normas processadas:    %d%n", totalProcessadas);
    System.out.printf("Total atual no banco:  %d embeddings%n", totalNoBanco);
    System.out.println("=".repeat(80));

    return totalProcessadas;
  }

  public long contarEmbeddingsExistentes() throws SQLException {
    return repositorioEmentas.contarEmbeddings();
  }

  public List<EmbeddingNorma> carregarEmbeddings() throws SQLException {
    return repositorioEmentas.listarEmbeddingsParaClusterizacao();
  }
}
