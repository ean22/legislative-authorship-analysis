package org.entryPoint.benchmark.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Relatório consolidado do benchmark experimental.
 */
public record RelatorioBenchmark(
    LocalDateTime dataExecucao,
    int totalDocumentos,
    ResultadoExperimento resultadoBaseline,
    ResultadoExperimento melhorResultado,
    List<ResultadoExperimento> todosResultados,
    double ganhoSobreBaselinePercentual
) {

  public String gerarMarkdown() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    StringBuilder sb = new StringBuilder();

    sb.append("# Relatório Experimental de Benchmark: Modelagem de Tópicos e Autoria Legislativa\n\n");
    sb.append(String.format("- **Data da Execução:** %s\n", dataExecucao.format(dtf)));
    sb.append(String.format("- **Corpus Analisado:** %d normas legislativas\n", totalDocumentos));
    sb.append(String.format("- **Total de Configurações Comparadas:** %d experimentos\n", todosResultados.size()));
    sb.append(String.format("- **Baseline:** %s (Score: %.3f)\n",
        resultadoBaseline.configuracao().nome(),
        resultadoBaseline.metricas().scoreCompostoGlobal()));
    sb.append(String.format("- **Melhor Combinação Selecionada:** %s (Score: %.3f)\n",
        melhorResultado.configuracao().nome(),
        melhorResultado.metricas().scoreCompostoGlobal()));
    sb.append(String.format("- **Ganho sobre Baseline:** +%.1f%%\n\n", ganhoSobreBaselinePercentual));

    sb.append("## Tabela Comparativa de Desempenho e Métricas Reais\n\n");
    sb.append("| Rank | Configuração | Redução Textual | Algoritmo | K | Score Global | Coerência (UMass) | Diversidade | Estabilidade | Utilidade Autores | Por que escolher? (Diagnóstico Empírico) |\n");
    sb.append("|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---|\n");

    for (int i = 0; i < todosResultados.size(); i++) {
      ResultadoExperimento r = todosResultados.get(i);
      String marker = r.configuracao().isBaseline() ? " *(Baseline)*" : (r == melhorResultado ? " 🏆 *(Vencedor)*" : "");
      sb.append(String.format("| %d | %s%s | %s | %s | %d | **%.3f** | %.2f | %.2f | %.2f | %.2f | %s |\n",
          i + 1,
          r.configuracao().nome(),
          marker,
          r.configuracao().reducao().name(),
          r.configuracao().algoritmo().name(),
          r.configuracao().k(),
          r.metricas().scoreCompostoGlobal(),
          r.metricas().coerenciaUmass(),
          r.metricas().diversidadeTopicos(),
          r.metricas().estabilidadeJaccard(),
          r.metricas().utilidadeTematicaAutoral(),
          r.justificativaEscolha()));
    }

    sb.append("\n## Tópicos Descobertos pela Melhor Combinação\n\n");
    for (var topico : melhorResultado.topicos()) {
      sb.append(String.format("- **Tópico %02d [%s]:** %s\n",
          topico.id(),
          topico.nome(),
          String.join(", ", topico.termosPrincipais())));
    }

    sb.append("\n## Perfis Temáticos dos Principais Órgãos Emissores (Melhor Combinação)\n\n");
    for (var perfil : melhorResultado.perfisOrgaos()) {
      sb.append(String.format("- **%s** (%d normas):\n  - Tópico Dominante: *%s* (%.1f%%)\n  - Tópico Secundário: *%s* (%.1f%%)\n",
          perfil.nome(),
          perfil.totalNormas(),
          perfil.nomeTopicoPrincipal(),
          perfil.proporcaoTopicoPrincipal() * 100.0,
          perfil.nomeTopicoSecundario(),
          perfil.proporcaoTopicoSecundario() * 100.0));
    }

    sb.append("\n## Perfis Temáticos dos Principais Autores Políticos (Melhor Combinação)\n\n");
    for (var perfil : melhorResultado.perfisAutores()) {
      sb.append(String.format("- **%s** (%d normas):\n  - Dominante: *%s* (%.1f%%)\n  - Secundário: *%s* (%.1f%%)\n",
          perfil.nome(),
          perfil.totalNormas(),
          perfil.nomeTopicoPrincipal(),
          perfil.proporcaoTopicoPrincipal() * 100.0,
          perfil.nomeTopicoSecundario(),
          perfil.proporcaoTopicoSecundario() * 100.0));
    }

    return sb.toString();
  }
}
