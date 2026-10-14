package org.entryPoint.model.perfil;

import java.util.Map;

/**
 * Perfil Político Consolidado do Autor: integra as quatro dimensões de análise
 * (Temática, Atuação, Colaboração e Temporal) em uma visão única e estruturada.
 */
public record PerfilPoliticoAutor(
    long idAutor,
    String nomeAutor,
    String cargoPrincipal,
    PerfilTematicoLegislativo perfilTematico,
    PerfilAtuacaoLegislativa perfilAtuacao,
    PerfilColaboracaoPolitica perfilColaboracao,
    PerfilTemporalEvolucao perfilTemporal
) {

  /**
   * Gera um relatório visual textual completo para terminal ou exportação markdown.
   */
  public String gerarRelatorioVisual() {
    StringBuilder sb = new StringBuilder();
    String divisorForte = "=".repeat(95);
    String divisorSuave = "-".repeat(95);

    sb.append("\n").append(divisorForte).append("\n");
    sb.append(String.format("🏛️  PERFIL POLÍTICO LEGISLATIVO: %s (#%d)%n", nomeAutor, idAutor));
    sb.append(String.format("   Cargo Principal: %s | Período: %s%n",
        cargoPrincipal != null ? cargoPrincipal : "NÃO IDENTIFICADO",
        perfilTemporal != null ? perfilTemporal.getIntervaloAnosFormatado() : "N/D"));
    sb.append(divisorForte).append("\n");

    // 1. DIMENSÃO TEMÁTICA
    sb.append("\n1️⃣  PERFIL TEMÁTICO LEGISLATIVO\n");
    sb.append(divisorSuave).append("\n");
    if (perfilTematico != null && perfilTematico.distribuicaoTopicos() != null && !perfilTematico.distribuicaoTopicos().isEmpty()) {
      sb.append(String.format("• Total de normas temáticas analisadas: %d%n", perfilTematico.totalNormasComTopico()));
      sb.append(String.format("• Eixo Temático Predominante: %s (%.1f%%)%n",
          perfilTematico.topicoDominante(), perfilTematico.percentualTopicoDominante()));
      sb.append(String.format("• Grau de Especialização (HHI): %.3f -> %s%n",
          perfilTematico.indiceEspecializacaoHhi(), perfilTematico.classificarNivelEspecializacao()));
      sb.append(String.format("• Entropia Temática de Shannon: %.3f -> %s%n",
          perfilTematico.entropiaTematica(), perfilTematico.classificarEntropia()));
      sb.append("\n   DISTRIBUIÇÃO DOS TÓPICOS LEGISLATIVOS:\n");

      for (DistribuicaoTopico dt : perfilTematico.distribuicaoTopicos()) {
        int barras = (int) Math.round(dt.percentual() / 4.0);
        String barraVisual = "█".repeat(Math.max(1, barras)) + "░".repeat(Math.max(0, 25 - barras));
        sb.append(String.format("   [%s] %5.1f%% | %4d normas | %-38s (conf: %.2f)%n",
            barraVisual, dt.percentual(), dt.quantidadeNormas(), dt.nomeTopico(), dt.scoreMedioPertinencia()));
      }

      if (perfilTematico.termosMaisFrequentes() != null && !perfilTematico.termosMaisFrequentes().isEmpty()) {
        sb.append(String.format("%n   Vocabulário Recorrente: [ %s ]%n",
            String.join(", ", perfilTematico.termosMaisFrequentes())));
      }
    } else {
      sb.append("   (Nenhum dado temático disponível para este autor)\n");
    }

    // 2. DIMENSÃO DE ATUAÇÃO LEGISLATIVA
    sb.append("\n2️⃣  PERFIL DE ATUAÇÃO LEGISLATIVA\n");
    sb.append(divisorSuave).append("\n");
    if (perfilAtuacao != null) {
      sb.append(String.format("• Total Geral de Normas: %d | Instrumento Predominante: %s%n",
          perfilAtuacao.totalNormas(), perfilAtuacao.getInstrumentoPredominante()));
      sb.append(String.format("• Padrão de Autoria: %s%n", perfilAtuacao.classificarPadraoAutoria()));
      sb.append(String.format("  - Normas Individuais: %d (%.1f%%)%n",
          perfilAtuacao.normasIndividuais(),
          perfilAtuacao.totalNormas() > 0 ? (perfilAtuacao.normasIndividuais() * 100.0 / perfilAtuacao.totalNormas()) : 0.0));
      sb.append(String.format("  - Normas Coletivas: %d (%.1f%%) | Taxa de Coautoria: %.1f%%%n",
          perfilAtuacao.normasColetivas(),
          perfilAtuacao.totalNormas() > 0 ? (perfilAtuacao.normasColetivas() * 100.0 / perfilAtuacao.totalNormas()) : 0.0,
          perfilAtuacao.taxaCoautoria()));
      sb.append(String.format("  - Média de signatários por norma: %.2f autores%n", perfilAtuacao.mediaAutoresPorNorma()));

      sb.append("\n   TIPOS DE ATOS LEGISLATIVOS:\n");
      for (Map.Entry<String, Integer> entry : perfilAtuacao.distribuicaoTiposNormas().entrySet()) {
        double pct = perfilAtuacao.totalNormas() > 0 ? (entry.getValue() * 100.0 / perfilAtuacao.totalNormas()) : 0.0;
        sb.append(String.format("   • %-26s : %5d normas (%5.1f%%)%n", entry.getKey(), entry.getValue(), pct));
      }
    }

    // 3. DIMENSÃO DE COLABORAÇÃO POLÍTICA
    sb.append("\n3️⃣  PERFIL DE COLABORAÇÃO POLÍTICA\n");
    sb.append(divisorSuave).append("\n");
    if (perfilColaboracao != null) {
      sb.append(String.format("• Total de parceiros legislativos distintos: %d | Densidade: %s%n",
          perfilColaboracao.totalParceirosDistintos(), perfilColaboracao.classificarDensidadeRede()));
      sb.append(String.format("• Intensidade Média por Parceria: %.1f normas compartilhadas%n",
          perfilColaboracao.intensidadeMediaColaboracao()));

      if (perfilColaboracao.principaisParcerias() != null && !perfilColaboracao.principaisParcerias().isEmpty()) {
        sb.append("\n   PRINCIPAIS PARCEIROS POLÍTICOS (COAUTORIA):\n");
        int rank = 1;
        for (ParceriaPolitica p : perfilColaboracao.principaisParcerias()) {
          sb.append(String.format("   #%02d %s%n", rank++, p.getFormatacaoResumo()));
        }
      }

      if (perfilColaboracao.parceriasPorCargo() != null && !perfilColaboracao.parceriasPorCargo().isEmpty()) {
        sb.append(String.format("%n   Órgão/Secretaria com maior interface: %s%n",
            perfilColaboracao.getOrgaoParceiroMaisFrequente()));
      }
    }

    // 4. DIMENSÃO TEMPORAL E DE EVOLUÇÃO
    sb.append("\n4️⃣  PERFIL TEMPORAL E DE EVOLUÇÃO\n");
    sb.append(divisorSuave).append("\n");
    if (perfilTemporal != null && perfilTemporal.producaoPorAno() != null && !perfilTemporal.producaoPorAno().isEmpty()) {
      sb.append(String.format("• Período Ativo: %s%n", perfilTemporal.getIntervaloAnosFormatado()));
      sb.append(String.format("• Ano de Pico de Produção: %d (%d normas)%n",
          perfilTemporal.anoPico(), perfilTemporal.volumeAnoPico()));
      sb.append(String.format("• Média Anual de Produção: %.1f normas/ano%n",
          perfilTemporal.mediaNormasPorAno()));
      sb.append(String.format("• Pauta Dominante Recente: %s%n", perfilTemporal.getTendenciaRecente()));

      sb.append("\n   TRAJETÓRIA ANUAL E TEMA DOMINANTE:\n");
      int maxVol = Math.max(1, perfilTemporal.volumeAnoPico());
      for (Map.Entry<Integer, Integer> entry : perfilTemporal.producaoPorAno().entrySet()) {
        int ano = entry.getKey();
        int qtd = entry.getValue();
        int barras = (int) Math.round((qtd * 20.0) / maxVol);
        String barraVisual = "█".repeat(Math.max(1, barras));
        String temaAno = perfilTemporal.temaDominantePorAno().getOrDefault(ano, "-");
        sb.append(String.format("   %d | %-20s | %4d normas | Tema Dominante: %s%n",
            ano, barraVisual, qtd, temaAno));
      }
    }

    sb.append(divisorForte).append("\n");
    return sb.toString();
  }
}
