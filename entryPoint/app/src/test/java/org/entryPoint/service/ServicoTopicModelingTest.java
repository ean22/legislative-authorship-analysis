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
    assertEquals(ServicoTopicModeling.DEFAULT_K, resultado.topicos().size());
    assertTrue(resultado.totalTokens() > 0);

    // 2. Verificar que os 8 tópicos oficiais foram persistidos no banco
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
    assertEquals(6, resultado.topicos().size());
    assertTrue(resultado.totalTokens() > 0);
  }
}
