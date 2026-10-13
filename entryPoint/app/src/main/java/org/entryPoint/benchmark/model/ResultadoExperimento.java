package org.entryPoint.benchmark.model;

import java.util.List;
import org.entryPoint.model.TopicoTematico;

/**
 * Resultado completo da execução de um experimento no benchmark,
 * incluindo métricas reais e o diagnóstico empírico evidenciando o porquê de sua avaliação.
 */
public record ResultadoExperimento(
    ConfiguracaoExperimento configuracao,
    List<TopicoTematico> topicos,
    double[][] matrizDocTopico,      // W (distribuição documento -> tópicos)
    MetricasTopicos metricas,
    long tempoExecucaoMs,
    long memoriaUsadaMb,
    List<PerfilTematicoEntidade> perfisOrgaos,
    List<PerfilTematicoEntidade> perfisAutores,
    String justificativaEscolha
) {

  public String getLinhaTabelaFormatada(int ranking) {
    return String.format(
        "| %2d | %-28s | %-16s | %-14s | %2d | %6.3f | %7.2f | %5.2f | %5.2f | %5.2f | %6d ms |",
        ranking,
        configuracao.nome(),
        configuracao.reducao().name(),
        configuracao.algoritmo().name(),
        configuracao.k(),
        metricas.scoreCompostoGlobal(),
        metricas.coerenciaUmass(),
        metricas.diversidadeTopicos(),
        metricas.estabilidadeJaccard(),
        metricas.utilidadeTematicaAutoral(),
        tempoExecucaoMs
    );
  }

  public String getLinhaTabelaComJustificativa(int ranking) {
    return String.format(
        "| %2d | %-24s | %-12s | %2d | %6.3f | %5.2f | %5.2f | %5.2f | %-55s |",
        ranking,
        configuracao.nome(),
        configuracao.reducao().name(),
        configuracao.k(),
        metricas.scoreCompostoGlobal(),
        metricas.coerenciaUmass(),
        metricas.diversidadeTopicos(),
        metricas.utilidadeTematicaAutoral(),
        justificativaEscolha
    );
  }
}
