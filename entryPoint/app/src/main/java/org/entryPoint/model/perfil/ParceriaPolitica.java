package org.entryPoint.model.perfil;

import java.util.List;

/**
 * Representa um vínculo de colaboração política e coautoria entre dois autores.
 */
public record ParceriaPolitica(
    long idParceiro,
    String nomeParceiro,
    String cargoParceiro,
    int normasCompartilhadas,
    double percentualDoTotalAutor,
    List<String> topicosEmComum
) {
  public String getFormatacaoResumo() {
    String topicos = (topicosEmComum != null && !topicosEmComum.isEmpty())
        ? String.join(", ", topicosEmComum)
        : "N/A";
    return String.format("%-32s (%s) | %4d normas (%4.1f%%) | Temas: [%s]",
        nomeParceiro,
        (cargoParceiro != null && !cargoParceiro.isBlank() ? cargoParceiro : "CARGO N/D"),
        normasCompartilhadas,
        percentualDoTotalAutor,
        topicos);
  }
}
