package org.entryPoint.model;

import java.util.List;

public record TopicoTematico(
  int id,
  String nome,
  List<String> termosPrincipais
) {
  public String getDescricaoFormatada() {
    return String.format("%s (%s)", nome, String.join(", ", termosPrincipais));
  }
}
