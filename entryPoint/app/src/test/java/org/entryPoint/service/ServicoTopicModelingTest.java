package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.entryPoint.model.MetricasClassificacao;
import org.entryPoint.model.PerfilAutor;
import org.junit.jupiter.api.Test;

public class ServicoTopicModelingTest {

  @Test
  public void testExecucaoTopicModelingEPerfis() throws SQLException {
    ServicoTopicModeling servicoTopicModeling = new ServicoTopicModeling();
    ServicoPerfilAutores servicoPerfil = new ServicoPerfilAutores();
    ServicoAvaliacaoClassificacao servicoAvaliacao = new ServicoAvaliacaoClassificacao();

    // 1. Executar Topic Modeling (K=12, 25 iterações)
    ServicoTopicModeling.ResultadoTopicModeling resultado = servicoTopicModeling.executarTopicModeling(12, 25, true);
    assertNotNull(resultado);
    assertFalse(resultado.topicos().isEmpty());
    assertFalse(resultado.classificacoes().isEmpty());
    assertTrue(resultado.totalTokens() > 0);

    // 2. Gerar Perfis dos Autores
    List<PerfilAutor> perfis = servicoPerfil.gerarPerfisAutores(10);
    assertNotNull(perfis);

    // 3. Avaliar Métricas de Classificação
    MetricasClassificacao metricas = servicoAvaliacao.avaliarDesempenho(resultado.tempoMs(), resultado.totalTokens(), resultado.memoriaMb());
    assertNotNull(metricas);
    assertTrue(metricas.acuracia() >= 0.0);
    assertTrue(metricas.f1ScoreMacro() >= 0.0);
  }
}
