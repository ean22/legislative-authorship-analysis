package org.entryPoint.service;

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
  public void testExecucaoTopicModelingEPersistenciaTopicos() throws SQLException {
    ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
    RepositorioTopicos repositorioTopicos = new RepositorioTopicos();

    // 1. Executar Topic Modeling (K=12, 25 iterações) e persistir tópicos
    ServicoTopicModeling.ResultadoTopicModeling resultado = servicoTopicModeling.executarTopicModeling(12, 25, true);
    assertNotNull(resultado);
    assertFalse(resultado.topicos().isEmpty());
    assertTrue(resultado.totalTokens() > 0);

    // 2. Verificar que todos os tópicos identificados foram persistidos no banco
    List<TopicoTematico> topicosSalvos = repositorioTopicos.listarTopicosSalvos();
    assertFalse(topicosSalvos.isEmpty());
    assertTrue(topicosSalvos.size() >= resultado.topicos().size());

    // 3. Verificar formato da descrição dos tópicos
    for (TopicoTematico t : resultado.topicos()) {
      assertNotNull(t.nome());
      assertFalse(t.termosPrincipais().isEmpty());
      String desc = t.getDescricaoFormatada();
      assertTrue(desc.startsWith(t.nome() + " ("));
      assertTrue(desc.endsWith(")"));
    }
  }
}
