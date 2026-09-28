package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicoRelacionamentoNormasTest {

  private ServicoRelacionamentoNormas servico;

  @BeforeEach
  void setUp() {
    servico = new ServicoRelacionamentoNormas();
  }

  @Test
  void testClassificarRelacionamentoRevogacao() {
    assertEquals("REVOGACAO", servico.classificarRelacionamento("(Revogado pela Lei nº"));
    assertEquals("REVOGACAO", servico.classificarRelacionamento("Revogam-se as disposições em contrário da Lei nº"));
    assertEquals("REVOGACAO", servico.classificarRelacionamento("Fica revogado o Decreto nº"));
  }

  @Test
  void testClassificarRelacionamentoAlteracao() {
    assertEquals("ALTERACAO", servico.classificarRelacionamento("(Redação dada pelo Decreto nº"));
    assertEquals("ALTERACAO", servico.classificarRelacionamento("CONFERE NOVA REDAÇÃO AO ARTIGO 3º DO DECRETO Nº"));
    assertEquals("ALTERACAO", servico.classificarRelacionamento("Altera a Lei nº"));
    assertEquals("ALTERACAO", servico.classificarRelacionamento("SUBSTITUI OS ANEXOS I E II DO DECRETO Nº"));
  }

  @Test
  void testClassificarRelacionamentoAcrescimo() {
    assertEquals("ACRESCIMO", servico.classificarRelacionamento("(Acrescido pela Lei nº"));
    assertEquals("ACRESCIMO", servico.classificarRelacionamento("(Inserido pelo Decreto nº"));
    assertEquals("ACRESCIMO", servico.classificarRelacionamento("Acrescenta dispositivo à Lei nº"));
  }

  @Test
  void testClassificarRelacionamentoRegulamentacao() {
    assertEquals("REGULAMENTACAO", servico.classificarRelacionamento("Regulamenta a Lei nº"));
    assertEquals("REGULAMENTACAO", servico.classificarRelacionamento("Dá regulamento ao Decreto nº"));
  }

  @Test
  void testClassificarRelacionamentoBaseLegal() {
    assertEquals("BASE_LEGAL", servico.classificarRelacionamento("DE ACORDO COM A LEI Nº"));
    assertEquals("BASE_LEGAL", servico.classificarRelacionamento("nos termos do Decreto nº"));
    assertEquals("BASE_LEGAL", servico.classificarRelacionamento("com fundamento na Lei nº"));
  }

  @Test
  void testClassificarRelacionamentoVide() {
    assertEquals("VIDE", servico.classificarRelacionamento("(Vide Decreto nº"));
    assertEquals("VIDE", servico.classificarRelacionamento("Veja o Decreto nº"));
  }

  @Test
  void testClassificarRelacionamentoReferenciaPadrao() {
    assertEquals("REFERENCIA", servico.classificarRelacionamento("conforme previsto"));
    assertEquals("REFERENCIA", servico.classificarRelacionamento(""));
    assertEquals("REFERENCIA", servico.classificarRelacionamento(null));
  }

  @Test
  void testGerarChaveNorma() {
    assertEquals("lei-ordinaria:2013:15764", ServicoRelacionamentoNormas.gerarChaveNorma("lei-ordinaria", 2013, 15764));
    assertEquals("decreto:2015:56123", ServicoRelacionamentoNormas.gerarChaveNorma("DECRETO", 2015, 56123));
  }
}
