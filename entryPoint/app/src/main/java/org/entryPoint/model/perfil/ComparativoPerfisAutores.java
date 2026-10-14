package org.entryPoint.model.perfil;

/**
 * Representa a comparação analítica entre dois perfis políticos,
 * avaliando afinidade temática (similaridade cosseno), convergência de temas
 * e laço de coautoria direta.
 */
public record ComparativoPerfisAutores(
    PerfilPoliticoAutor autor1,
    PerfilPoliticoAutor autor2,
    double similaridadeTematicaCosseno,
    int normasCompartilhadas,
    String diagnosticoAfinidade
) {

  public String gerarRelatorioComparativo() {
    StringBuilder sb = new StringBuilder();
    String divisor = "=".repeat(95);

    sb.append("\n").append(divisor).append("\n");
    sb.append(String.format("⚖️  COMPARAÇÃO POLÍTICA LEGISLATIVA: %s  VS  %s%n",
        autor1.nomeAutor(), autor2.nomeAutor()));
    sb.append(divisor).append("\n");

    sb.append(String.format("• Similaridade Temática Cosseno: %.3f (%.1f%% de convergência de pautas)%n",
        similaridadeTematicaCosseno, similaridadeTematicaCosseno * 100.0));
    sb.append(String.format("• Normas Compartilhadas em Coautoria: %d normas conjuntas%n",
        normasCompartilhadas));
    sb.append(String.format("• Diagnóstico de Alinhamento: %s%n%n", diagnosticoAfinidade));

    sb.append(String.format("%-30s | %-30s | %-30s%n", "DIMENSÃO", autor1.nomeAutor(), autor2.nomeAutor()));
    sb.append("-".repeat(95)).append("\n");

    sb.append(String.format("%-30s | %-30s | %-30s%n",
        "Cargo Principal", autor1.cargoPrincipal(), autor2.cargoPrincipal()));
    sb.append(String.format("%-30s | %-30s | %-30s%n",
        "Período de Atuação",
        autor1.perfilTemporal().getIntervaloAnosFormatado(),
        autor2.perfilTemporal().getIntervaloAnosFormatado()));
    sb.append(String.format("%-30s | %-30d | %-30d%n",
        "Total de Normas",
        autor1.perfilAtuacao().totalNormas(),
        autor2.perfilAtuacao().totalNormas()));
    sb.append(String.format("%-30s | %-30s | %-30s%n",
        "Eixo Temático Principal",
        autor1.perfilTematico().topicoDominante(),
        autor2.perfilTematico().topicoDominante()));
    sb.append(String.format("%-30s | %-30s | %-30s%n",
        "Padrão de Autoria",
        autor1.perfilAtuacao().classificarPadraoAutoria(),
        autor2.perfilAtuacao().classificarPadraoAutoria()));
    sb.append(String.format("%-30s | %-30.1f%% | %-30.1f%%%n",
        "Taxa de Coautoria",
        autor1.perfilAtuacao().taxaCoautoria(),
        autor2.perfilAtuacao().taxaCoautoria()));
    sb.append(String.format("%-30s | %-30s | %-30s%n",
        "Densidade da Rede",
        autor1.perfilColaboracao().classificarDensidadeRede(),
        autor2.perfilColaboracao().classificarDensidadeRede()));

    sb.append(divisor).append("\n");
    return sb.toString();
  }
}
