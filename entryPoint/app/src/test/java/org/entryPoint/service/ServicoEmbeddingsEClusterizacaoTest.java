package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.entryPoint.model.ClusterInfo;
import org.entryPoint.model.EmbeddingNorma;
import org.entryPoint.model.NormaRepresentativa;
import org.entryPoint.model.ResultadoClusterizacao;
import org.entryPoint.repository.RepositorioEmentasPreProcessadas;
import org.entryPoint.service.embeddings.GeradorEmbeddingsLocal;
import org.entryPoint.service.embeddings.ServicoEmbeddings;
import org.junit.jupiter.api.Test;

public class ServicoEmbeddingsEClusterizacaoTest {

  @Test
  public void testGeradorEmbeddingsLocal() {
    GeradorEmbeddingsLocal gerador = new GeradorEmbeddingsLocal(128);
    assertEquals(128, gerador.getDimensao());
    assertTrue(gerador.isDisponivel());

    String ementa1 = "Dispõe sobre a denominação da Praça da República e dá outras providências.";
    String ementa2 = "Abre crédito suplementar ao orçamento da Secretaria Municipal de Educação.";

    float[] v1 = gerador.gerarEmbedding(ementa1);
    float[] v2 = gerador.gerarEmbedding(ementa2);

    assertNotNull(v1);
    assertNotNull(v2);
    assertEquals(128, v1.length);
    assertEquals(128, v2.length);

    // Verificar normalização L2 (norma próxima de 1.0)
    double norma1 = 0.0;
    for (float f : v1) norma1 += f * f;
    assertTrue(Math.abs(Math.sqrt(norma1) - 1.0) < 1e-4);

    // Vetores de textos diferentes não devem ser idênticos
    double produtoEscalar = 0.0;
    for (int i = 0; i < 128; i++) produtoEscalar += v1[i] * v2[i];
    assertTrue(produtoEscalar < 0.99, "Textos semanticamente diferentes não devem ter similaridade 1.0");
  }

  @Test
  public void testSerializacaoFloatArrayJson() {
    float[] original = new float[] {0.123f, -0.456f, 0.789f, 0.0f};
    String json = RepositorioEmentasPreProcessadas.formatFloatArrayToJson(original);
    assertNotNull(json);
    assertTrue(json.startsWith("["));
    assertTrue(json.endsWith("]"));

    float[] recuperado = RepositorioEmentasPreProcessadas.parseJsonFloatArray(json);
    assertNotNull(recuperado);
    assertEquals(original.length, recuperado.length);
    for (int i = 0; i < original.length; i++) {
      assertEquals(original[i], recuperado[i], 1e-5f);
    }
  }

  @Test
  public void testFluxoCompletoEmbeddingsEClusterizacao() throws Exception {
    ServicoEmbeddings servicoEmbeddings = new ServicoEmbeddings();
    ServicoClusterizacaoEmbeddings servicoClusterizacao = new ServicoClusterizacaoEmbeddings();

    // 1. Gerar e salvar embeddings com o gerador local para uma amostra
    GeradorEmbeddingsLocal geradorLocal = new GeradorEmbeddingsLocal(128);
    int processados = servicoEmbeddings.gerarESalvarEmbeddings(geradorLocal, false, 50);
    assertTrue(processados > 0, "Deveria processar ementas para teste");

    // 2. Verificar se foram persistidos no banco
    long totalNoBanco = servicoEmbeddings.contarEmbeddingsExistentes();
    assertTrue(totalNoBanco >= processados, "Embeddings devem estar salvos em data/leis.db");

    // 3. Executar clusterização K-Means (k = 3)
    int k = 3;
    ResultadoClusterizacao resultado = servicoClusterizacao.clusterizar(k);

    assertNotNull(resultado);
    assertEquals(k, resultado.k());
    assertEquals(k, resultado.clusters().size());
    assertTrue(resultado.totalNormasClusterizadas() > 0);
    assertTrue(resultado.inerciaTotal() >= 0.0);

    // 4. Validar que cada cluster possui sua norma mais representativa (Medoid)
    for (ClusterInfo cluster : resultado.clusters()) {
      assertTrue(cluster.totalNormas() >= 0);
      if (cluster.totalNormas() > 0) {
        NormaRepresentativa medoid = cluster.normaMaisRepresentativa();
        assertNotNull(medoid, "Cluster com normas deve possuir norma mais representativa");
        assertTrue(medoid.idNorma() > 0);
        assertNotNull(medoid.ementa());
        assertTrue(medoid.similaridadeComCentroide() >= -1.0 && medoid.similaridadeComCentroide() <= 1.0);

        // Testar método utilitário obterNormaMaisRepresentativa
        NormaRepresentativa medoidBuscado = servicoClusterizacao.obterNormaMaisRepresentativa(cluster.idCluster(), resultado);
        assertEquals(medoid.idNorma(), medoidBuscado.idNorma());
      }
    }
  }
}
