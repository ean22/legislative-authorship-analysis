package org.entryPoint.model;

public record NormaTopico(
  long idNorma,
  int idTopico,
  int idRotuloTopico,
  String rotuloTopico,
  double scorePertinencia,
  String termosChave
) {}
