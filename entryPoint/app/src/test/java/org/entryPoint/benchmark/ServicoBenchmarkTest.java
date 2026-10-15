package org.entryPoint.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import org.entryPoint.benchmark.model.ConfiguracaoExperimento;
import org.entryPoint.benchmark.model.DefinicaoPipeline;
import org.entryPoint.benchmark.model.RelatorioBenchmark;
import org.entryPoint.benchmark.model.ResultadoExperimento;
import org.entryPoint.benchmark.model.TipoAlgoritmoTopicModeling;
import org.entryPoint.benchmark.model.TipoReducaoTextual;
import org.junit.jupiter.api.Test;

public class ServicoBenchmarkTest {

  @Test
  public void testExecucaoBenchmarkERankingComparativo() throws SQLException {
    ServicoBenchmark servicoBenchmark = new ServicoBenchmark();

    // 1. Executar benchmark iterando sobre as reduções inteligentes solicitadas:
    // stopwords, chunking, embeddings, chi-square, MMR, clustering semântico, textrank
    List<ConfiguracaoExperimento> gradeReduzida = List.of(
        ConfiguracaoExperimento.criarBaseline(), // NMF K=12 (Baseline)
        new ConfiguracaoExperimento(
            "nmf_k08_test",
            "NMF K=8",
            TipoReducaoTextual.STOPWORDS_TFIDF,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "textrank_nmf_k08_test",
            "TextRank + NMF K=8",
            TipoReducaoTextual.TEXTRANK,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "mmr_nmf_k08_test",
            "MMR Diversificado + NMF K=8",
            TipoReducaoTextual.MMR,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "chisquare_nmf_k08_test",
            "Qui-Quadrado + NMF K=8",
            TipoReducaoTextual.CHI_SQUARE,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "chunking_nmf_k08_test",
            "Chunking Sentencial + NMF K=8",
            TipoReducaoTextual.CHUNKING_SENTENCAS,
            org.entryPoint.benchmark.model.TipoChunking.SENTENCE_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "embeddings_nmf_k08_test",
            "Embeddings SVD + NMF K=8",
            TipoReducaoTextual.EMBEDDINGS_SVD,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.DENSE_SVD,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "clustering_semantico_nmf_k08_test",
            "Clustering Semântico + NMF K=8",
            TipoReducaoTextual.CLUSTERING_SEMANTICO,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.NMF,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "lda_k08_test",
            "LDA K=8",
            TipoReducaoTextual.STOPWORDS_TFIDF,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.SPARSE_TFIDF,
            TipoAlgoritmoTopicModeling.LDA,
            8, 15, 42L, false
        ),
        new ConfiguracaoExperimento(
            "cluster_ctfidf_k08_test",
            "Cluster c-TF-IDF K=8",
            TipoReducaoTextual.TEXTRANK,
            org.entryPoint.benchmark.model.TipoChunking.DOCUMENT_LEVEL,
            org.entryPoint.benchmark.model.TipoRepresentacaoSemantica.DENSE_SVD,
            TipoAlgoritmoTopicModeling.CLUSTER_C_TFIDF,
            8, 15, 42L, false
        )
    );

    RelatorioBenchmark relatorio = servicoBenchmark.executarGrade(gradeReduzida, 300, true);

    // 2. Asserções do relatório
    assertNotNull(relatorio);
    assertTrue(relatorio.totalDocumentos() > 0);
    assertEquals(10, relatorio.todosResultados().size());

    // 3. Verificar que a Baseline NMF K=12 está explicitamente identificada
    ResultadoExperimento baseline = relatorio.resultadoBaseline();
    assertNotNull(baseline);
    assertTrue(baseline.configuracao().isBaseline());
    assertEquals(12, baseline.configuracao().k());
    assertEquals(TipoAlgoritmoTopicModeling.NMF, baseline.configuracao().algoritmo());

    // 4. Verificar que a melhor combinação foi selecionada e supera o baseline
    ResultadoExperimento melhor = relatorio.melhorResultado();
    assertNotNull(melhor);
    assertTrue(melhor.metricas().scoreCompostoGlobal() >= baseline.metricas().scoreCompostoGlobal());

    // 5. Verificar métricas reais e justificativas analíticas em cada resultado
    for (ResultadoExperimento r : relatorio.todosResultados()) {
      var m = r.metricas();
      assertTrue(m.scoreCompostoGlobal() >= 0.0 && m.scoreCompostoGlobal() <= 1.0);
      assertTrue(m.diversidadeTopicos() > 0.0 && m.diversidadeTopicos() <= 1.0);
      assertTrue(m.estabilidadeJaccard() >= 0.0 && m.estabilidadeJaccard() <= 1.0);
      assertTrue(m.utilidadeTematicaAutoral() >= 0.0 && m.utilidadeTematicaAutoral() <= 1.0);
      assertNotNull(r.justificativaEscolha());
      assertFalse(r.justificativaEscolha().isBlank(), "Deveria ter diagnóstico empírico evidenciando o porquê da avaliação");
      assertFalse(r.topicos().isEmpty());
      assertTrue(r.topicos().size() <= r.configuracao().k(), "Número de tópicos deve ser <= K devido ao filtro de clusters vazios");
    }

    // 6. Verificar perfis temáticos construídos
    assertNotNull(melhor.perfisOrgaos());
    for (var perfil : melhor.perfisOrgaos()) {
      assertNotNull(perfil.nome());
      assertTrue(perfil.totalNormas() > 0);
      assertNotNull(perfil.nomeTopicoPrincipal());
      assertTrue(perfil.proporcaoTopicoPrincipal() > 0.0);
    }

    // 7. Definir e Fixar o Melhor Pipeline para prosseguimento oficial
    DefinicaoPipeline pipelineDefinido = servicoBenchmark.definirEFixarMelhorPipeline(relatorio);
    assertNotNull(pipelineDefinido);
    assertTrue(pipelineDefinido.ativo());
    assertEquals(melhor.configuracao().nome(), pipelineDefinido.nome());
    assertEquals(melhor.configuracao().k(), pipelineDefinido.k());
    assertNotNull(pipelineDefinido.justificativaEscolha());

    // 8. Recuperar o pipeline ativo do banco e verificar consistência
    var optAtivo = servicoBenchmark.getServicoPipeline().obterPipelineAtivo();
    assertTrue(optAtivo.isPresent(), "O pipeline definido deve estar persistido e ativo no banco");
    assertEquals(pipelineDefinido.nome(), optAtivo.get().nome());
    assertEquals(pipelineDefinido.reducao(), optAtivo.get().reducao());

    // 9. Verificar que os arquivos estruturados em disco foram criados
    File arquivoMd = new File("benchmark_results.md");
    assertTrue(arquivoMd.exists());
    assertTrue(arquivoMd.length() > 0);

    File arquivoJson = new File("pipeline_config.json");
    assertTrue(arquivoJson.exists());
    assertTrue(arquivoJson.length() > 0);
  }
}
