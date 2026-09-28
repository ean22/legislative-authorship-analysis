package org.entryPoint.model;

import java.util.List;

public record TopicoTematico(
  int id,
  int idRotuloTopico,
  String rotulo,
  List<String> termosPrincipais,
  long totalNormas,
  double percentualBase
) {
  public String getDescricaoFormatada() {
    return String.format("Tópico #%02d [%s] (ID Rótulo: %d): %s (%.1f%%)",
      id, rotulo, idRotuloTopico, String.join(", ", termosPrincipais), percentualBase);
  }
}
