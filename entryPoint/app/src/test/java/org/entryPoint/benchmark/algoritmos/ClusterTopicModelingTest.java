package org.entryPoint.benchmark.algoritmos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;
import org.junit.jupiter.api.Test;

public class ClusterTopicModelingTest {

  @Test
  public void testFiltroClustersVaziosQuandoKSolicitadoEhMaiorQueGruposReais() {
    // Cenário: 4 documentos com apenas 2 termos distintos em 2 grupos ortogonais
    // Solicitamos K = 6. O algoritmo deve descartar os clusters vazios e manter apenas os válidos (<= 4).
    List<Long> docIds = List.of(101L, 102L, 103L, 104L);
    List<String> vocabulario = List.of("credito", "saude");
    Map<String, Integer> vocabIndex = Map.of("credito", 0, "saude", 1);
    Map<String, Integer> df = Map.of("credito", 2, "saude", 2);

    List<Map<Integer, Double>> tfidfEsparsa = new ArrayList<>();
    tfidfEsparsa.add(Map.of(0, 1.0)); // doc 1: credito
    tfidfEsparsa.add(Map.of(0, 1.0)); // doc 2: credito
    tfidfEsparsa.add(Map.of(1, 1.0)); // doc 3: saude
    tfidfEsparsa.add(Map.of(1, 1.0)); // doc 4: saude

    // Matriz densa 2D: docs 1 e 2 em [1.0, 0.0], docs 3 e 4 em [0.0, 1.0]
    double[][] matrizDensa = new double[][] {
        {1.0, 0.0},
        {1.0, 0.0},
        {0.0, 1.0},
        {0.0, 1.0}
    };

    CorpusProcessado corpus = new CorpusProcessado(
        docIds,
        List.of(List.of("credito"), List.of("credito"), List.of("saude"), List.of("saude")),
        vocabulario,
        vocabIndex,
        df,
        tfidfEsparsa,
        matrizDensa,
        4,
        Map.of()
    );

    ClusterTopicModeling model = new ClusterTopicModeling();
    int kDesejado = 6;
    var resultado = model.executar(corpus, kDesejado, 20, 42L);

    assertNotNull(resultado);
    List<TopicoTematico> topicos = resultado.topicos();

    // 1. O número de tópicos gerados deve ser menor que K solicitado devido ao filtro de vazios
    assertTrue(topicos.size() < kDesejado, "Clusters vazios devem ser filtrados");
    assertTrue(topicos.size() <= 4, "No máximo 4 clusters podem conter documentos");
    assertTrue(topicos.size() >= 1, "Pelo menos um cluster ativo deve ser retornado");

    // 2. O total de clusters filtrados deve ser exatamente a diferença (k - kValidos)
    assertEquals(kDesejado - topicos.size(), model.getUltimosClustersFiltradosTotal());
    assertEquals(kDesejado, model.getUltimosClustersOriginaisK());

    // 3. Verificar que as matrizes de saída W e H estão com dimensões compactadas para os clusters válidos
    assertEquals(4, resultado.matrizDocTopico().length);
    assertEquals(topicos.size(), resultado.matrizDocTopico()[0].length);
    assertEquals(topicos.size(), resultado.matrizTopicoTermo().length);
    assertEquals(2, resultado.matrizTopicoTermo()[0].length);

    // 4. Nenhum tópico deve ter termos vazios nem ser um cluster vazio artificial
    for (TopicoTematico t : topicos) {
      assertNotNull(t.nome());
      assertFalse(t.nome().isBlank());
      assertFalse(t.termosPrincipais().isEmpty());
    }

    // 5. Verificar que os clusters atribuídos estão no intervalo [0, topicos.size() - 1]
    int[] clusters = model.getUltimosClusters();
    for (int c : clusters) {
      assertTrue(c >= 0 && c < topicos.size());
    }
  }

  @Test
  public void testCorpusVazioOuKInvalidoRetornaResultadoVazio() {
    CorpusProcessado corpusVazio = new CorpusProcessado(
        List.of(), List.of(), List.of(), Map.of(), Map.of(), List.of(), new double[0][0], 0, Map.of()
    );

    ClusterTopicModeling model = new ClusterTopicModeling();
    var resultado = model.executar(corpusVazio, 5, 10, 42L);

    assertNotNull(resultado);
    assertTrue(resultado.topicos().isEmpty());
    assertEquals(0, resultado.matrizDocTopico().length);
  }
}
