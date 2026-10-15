package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.entryPoint.model.TopicoTematico;
import org.entryPoint.repository.RepositorioTopicos;
import org.junit.jupiter.api.Test;

public class ServicoTopicModelingTest {

  @Test
  public void testExecucaoTopicModelingPipelineVencedor() throws SQLException {
    ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
    RepositorioTopicos repositorioTopicos = new RepositorioTopicos();

    // 1. Executar Topic Modeling padrão (Pipeline Vencedor: Cluster c-TF-IDF, K=8)
    ServicoTopicModeling.ResultadoTopicModeling resultado = servicoTopicModeling.executarTopicModeling();
    assertNotNull(resultado);
    assertFalse(resultado.topicos().isEmpty());
    assertTrue(resultado.topicos().size() <= ServicoTopicModeling.DEFAULT_K, "Número de tópicos deve ser <= K devido ao filtro de clusters vazios");
    assertTrue(resultado.totalTokens() > 0);

    // 2. Verificar que os tópicos oficiais válidos foram persistidos no banco
    List<TopicoTematico> topicosSalvos = repositorioTopicos.listarTopicosSalvos();
    assertFalse(topicosSalvos.isEmpty());
    assertEquals(resultado.topicos().size(), topicosSalvos.size());

    // 3. Verificar formato da descrição dos tópicos
    for (TopicoTematico t : resultado.topicos()) {
      assertNotNull(t.nome());
      assertFalse(t.termosPrincipais().isEmpty());
      String desc = t.getDescricaoFormatada();
      assertTrue(desc.startsWith(t.nome() + " ("));
      assertTrue(desc.endsWith(")"));
    }
  }

  @Test
  public void testExecucaoTopicModelingComKCustomizado() throws SQLException {
    ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
    ServicoTopicModeling.ResultadoTopicModeling resultado = servicoTopicModeling.executarTopicModeling(6, 15, false);

    assertNotNull(resultado);
    assertFalse(resultado.topicos().isEmpty());
    assertTrue(resultado.topicos().size() <= 6);
    assertTrue(resultado.totalTokens() > 0);
  }

  @Test
  public void testMetodosAuditoriaTopicModeling() throws SQLException {
    ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();

    // 1. Auditoria geométrica de centroides
    servicoTopicModeling.exibirRelatorioCentroides();

    // 2. Detalhamento de c-TF-IDF
    servicoTopicModeling.exibirRelatorioCTfIdf(6);

    // 3. Auditoria NMF baseline
    servicoTopicModeling.executarAuditoriaNmf(6, 15);

    // 4. Estatísticas do banco SQLite
    servicoTopicModeling.auditarClassificacoesBanco();

    // 5. Inspecionar normas do tópico #1
    servicoTopicModeling.inspecionarAmostraNormasPorTopico(1, 3);
  }
}
