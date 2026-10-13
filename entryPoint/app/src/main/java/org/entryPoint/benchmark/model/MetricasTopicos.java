package org.entryPoint.benchmark.model;

/**
 * Agrupa as métricas quantitativas e qualitativas avaliadas pelo benchmark.
 * Não depende de uma única métrica matemática isolada; combina coerência semântica,
 * diversidade vocabular, estabilidade determinística e utilidade prática na separação de autores.
 */
public record MetricasTopicos(
    double coerenciaUmass,           // Coerência semântica UMass (coocorrência no corpus real)
    double coerenciaNormalizada,     // Normalizada [0, 1] para composição
    double diversidadeTopicos,       // Topic Diversity: proporção de termos únicos no top-N [0, 1]
    double estabilidadeJaccard,      // Reprodutibilidade: concordância de Jaccard entre sementes [0, 1]
    double purezaEspecializada,      // Pureza/concentração de secretarias temáticas (Saúde, Finanças, etc.) [0, 1]
    double discriminabilidadeOrgaos, // Divergência e separabilidade entre diferentes órgãos emissores [0, 1]
    double multitematicidadePrefeito,// Entropia/amplitude da carteira legislativa do Prefeito [0, 1]
    double utilidadeTematicaAutoral, // Score ponderado de utilidade para perfis institucionais [0, 1]
    double scoreCompostoGlobal       // Thematic Utility Score composto final [0, 1]
) {

  public String getSumarioFormatado() {
    return String.format(
        "Score: %.3f | Coerência(UMass): %.2f (norm: %.2f) | Div: %.2f | Estab: %.2f | UtilAutoral: %.2f (Pureza: %.2f, Sep: %.2f)",
        scoreCompostoGlobal,
        coerenciaUmass,
        coerenciaNormalizada,
        diversidadeTopicos,
        estabilidadeJaccard,
        utilidadeTematicaAutoral,
        purezaEspecializada,
        discriminabilidadeOrgaos
    );
  }
}
