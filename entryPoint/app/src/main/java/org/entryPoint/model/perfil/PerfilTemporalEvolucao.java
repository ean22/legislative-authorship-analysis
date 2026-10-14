package org.entryPoint.model.perfil;

import java.util.Map;

/**
 * Perfil Temporal e de Evolução: mapeia a trajetória cronológica do autor ao longo
 * dos anos e mandatos, identificando picos de atividade e mudanças de temas.
 */
public record PerfilTemporalEvolucao(
    int primeiroAno,
    int ultimoAno,
    int totalAnosAtivos,
    Map<Integer, Integer> producaoPorAno,
    int anoPico,
    int volumeAnoPico,
    double mediaNormasPorAno,
    Map<Integer, Map<String, Integer>> temasPorAno,
    Map<Integer, String> temaDominantePorAno
) {

  public String getIntervaloAnosFormatado() {
    if (primeiroAno == 0 && ultimoAno == 0) return "Sem registros temporais";
    if (primeiroAno == ultimoAno) return String.valueOf(primeiroAno);
    return String.format("%d - %d (%d anos de atuação)", primeiroAno, ultimoAno, totalAnosAtivos);
  }

  public String getTendenciaRecente() {
    if (temaDominantePorAno == null || temaDominantePorAno.isEmpty()) {
      return "Indefinida";
    }
    return temaDominantePorAno.getOrDefault(ultimoAno, "N/A");
  }
}
