package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.entryPoint.service.agent.AgenteGemini;
import org.junit.jupiter.api.Test;

class ServicoNormalizacaoCargoTest {

  @Test
  void testSanitizacaoTexto() {
    assertEquals("CONTROLADOR GERAL DO MUNICIPIO", AgenteGemini.sanitizarTexto("CONTROLADOR GERAL DO MUNICÍPIO"));
    assertEquals("CONTROLADOR GERAL DO MUNICIPIO - SUBSTITUTO", AgenteGemini.sanitizarTexto("CONTROLADOR GERAL DO MUNICÍPIO - SUBSTITUTO"));
    assertEquals("CONTROLADOR GERAL DO MUNICIPIO", AgenteGemini.sanitizarTexto("CONTROLADOR GERAL DO MUNICÍPIO."));
    assertEquals("PREFEITA EM EXERCICIO", AgenteGemini.sanitizarTexto("PREFEITA EM EXERCÍCIO"));
    assertEquals("PREFEITO", AgenteGemini.sanitizarTexto("PREFEITO;"));
    assertEquals("PREFEITO", AgenteGemini.sanitizarTexto("PREFEITO."));
    assertEquals("SECRETARIO MUNICIPAL DOS NEGOCIOS JURIDICOS", AgenteGemini.sanitizarTexto("SECRETÁRIO MUNICIPAL DOS NEGÓCIOS JURÍDICOS]"));
  }

  @Test
  void testParsearRespostaGemini() {
    String jsonResposta = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "text": "{\\"resultados\\": [{\\"original\\": \\"CONTROLADOR GERAL DO MUNICÍPIO.\\", \\"normalizado\\": \\"CONTROLADOR GERAL DO MUNICIPIO\\"}, {\\"original\\": \\"SECRETÁRIA MUNICIPAL DA CASA CIVIL = SUBSTITUTA\\", \\"normalizado\\": \\"SECRETARIA MUNICIPAL DA CASA CIVIL - SUBSTITUTA\\"}]}"
                }
              ]
            }
          }
        ]
      }
      """;

    List<String> loteOriginal = List.of(
      "CONTROLADOR GERAL DO MUNICÍPIO.",
      "SECRETÁRIA MUNICIPAL DA CASA CIVIL = SUBSTITUTA"
    );

    Map<String, String> resultado = AgenteGemini.parsearResposta(jsonResposta, loteOriginal);

    assertEquals("CONTROLADOR GERAL DO MUNICIPIO", resultado.get("CONTROLADOR GERAL DO MUNICÍPIO."));
    assertEquals("SECRETARIA MUNICIPAL DA CASA CIVIL - SUBSTITUTA", resultado.get("SECRETÁRIA MUNICIPAL DA CASA CIVIL = SUBSTITUTA"));
  }
}
