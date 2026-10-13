package org.entryPoint.benchmark.algoritmos;

import java.util.List;
import java.util.Locale;

/**
 * Utilitário para inferência de rótulos temáticos legíveis por humanos
 * a partir dos termos mais salientes de cada tópico.
 */
public class InferidorRotuloTema {

  public static String inferirRotuloTema(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
    }

    String concat = String.join(" ", topPalavras).toLowerCase(Locale.ROOT);

    if (concat.contains("utilidade") || (concat.contains("declara") && concat.contains("pública"))
        || concat.contains("entidade") || concat.contains("associação") || concat.contains("instituto")) {
      return "Declaração de Utilidade Pública";
    } else if (concat.contains("infantil") || concat.contains("educação") || concat.contains("creche")
        || concat.contains("escola") || concat.contains("ensino") || concat.contains("cei")
        || concat.contains("criação")) {
      return "Educação e Creches";
    } else if (concat.contains("calendário") || concat.contains("eventos") || concat.contains("comemorado")
        || concat.contains("anualmente") || concat.contains("semana") || concat.contains("dia")
        || concat.contains("conscientização")) {
      return "Datas Comemorativas e Calendário";
    } else if (concat.contains("denomina") || concat.contains("logradouro") || concat.contains("praça")
        || concat.contains("rua") || concat.contains("especifica") || concat.contains("situado")
        || concat.contains("avenida")) {
      return "Denominação de Vias e Logradouros";
    } else if (concat.contains("suplementar") || concat.contains("abre") || concat.contains("adicional")
        || concat.contains("crédito") || concat.contains("orçamento") || concat.contains("subvenções")
        || concat.contains("econômicas") || concat.contains("imposto") || concat.contains("tribut")) {
      return "Crédito Orçamentário e Finanças";
    } else if (concat.contains("retifica") || concat.contains("confere") || concat.contains("altera")
        || concat.contains("redação") || concat.contains("ementa") || concat.contains("substitui")
        || concat.contains("anexo")) {
      return "Legislação Complementar / Retificações";
    } else if (concat.contains("saúde") || concat.contains("médic") || concat.contains("sanitár")
        || concat.contains("hospital") || concat.contains("enfermagem") || concat.contains("sus")) {
      return "Saúde Pública e Assistência Hospitalar";
    } else if (concat.contains("imóveis") || concat.contains("imóvel") || concat.contains("particulares")
        || concat.contains("desafeta") || concat.contains("desapropria") || concat.contains("patrimônio")) {
      return "Bens Imóveis e Patrimônio Público";
    } else if (concat.contains("zeis") || concat.contains("fundiária")
        || (concat.contains("social") && concat.contains("interesse")) || concat.contains("habitação")) {
      return "Habitação e Regularização Fundiária";
    } else if (concat.contains("serviço") || concat.contains("preço") || concat.contains("tarifa")
        || concat.contains("prestados")) {
      return "Serviços Públicos e Tarifas";
    } else if (concat.contains("trânsito") || concat.contains("transporte") || concat.contains("veículo")
        || concat.contains("mobilidade")) {
      return "Transportes e Trânsito Urbano";
    } else if (concat.contains("meio ambiente") || concat.contains("verde") || concat.contains("parque")
        || concat.contains("ambiental") || concat.contains("sustentab")) {
      return "Meio Ambiente e Áreas Verdes";
    } else {
      return "Administração e Normas Gerais (" + topPalavras.get(0) + ")";
    }
  }
}
