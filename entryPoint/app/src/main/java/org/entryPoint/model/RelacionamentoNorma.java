package org.entryPoint.model;

public record RelacionamentoNorma(
    long idNormaOrigem,
    Long idNormaDestino,
    String tipoRelacionamento,
    String urlAlvo,
    String tipoSlugAlvo,
    Integer anoAlvo,
    Integer numeroAlvo,
    String trechoContexto
) {}
