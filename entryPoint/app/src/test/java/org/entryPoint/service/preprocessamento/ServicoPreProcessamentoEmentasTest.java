package org.entryPoint.service.preprocessamento;

import java.util.List;
import org.entryPoint.model.EmentaPreProcessada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServicoPreProcessamentoEmentasTest {

  private CorpusStats corpus;
  private ServicoPreProcessamentoEmentas servico;

  @BeforeEach
  void setUp() {
    corpus = new CorpusStats();
    List<CorpusStats.DocumentoInput> docs = List.of(
        new CorpusStats.DocumentoInput(1L, "DISPÕE SOBRE A CRIAÇÃO DO PARQUE MUNICIPAL E DÁ OUTRAS PROVIDÊNCIAS.", "Decreto"),
        new CorpusStats.DocumentoInput(2L, "INSTITUI O PROGRAMA DE EDUCAÇÃO AMBIENTAL NAS ESCOLAS PÚBLICAS MUNICIPAIS.", "Lei Ordinária"),
        new CorpusStats.DocumentoInput(3L, "REGULAMENTA O SISTEMA DE TRANSPORTE PÚBLICO E TRÂNSITO URBANO.", "Decreto"),
        new CorpusStats.DocumentoInput(4L, "AUTORIZA A ABERTURA DE CRÉDITO ADICIONAL SUPLEMENTAR PARA SAÚDE.", "Decreto"),
        new CorpusStats.DocumentoInput(5L, "DISPÕE SOBRE A PRESERVAÇÃO DO MEIO AMBIENTE E PROTEÇÃO DAS ÁREAS VERDES.", "Lei Ordinária")
    );
    corpus.treinar(docs);
    servico = new ServicoPreProcessamentoEmentas();
  }

  @Test
  @DisplayName("Deve calcular estatísticas do corpus e IDF corretamente")
  void testCorpusStats() {
    assertTrue(corpus.getTotalDocumentos() > 0);
    assertTrue(corpus.getIdf("parque") > 0);
    assertTrue(corpus.getDocumentFrequency("educacao") > 0 || corpus.getDocumentFrequency("educação") > 0);
  }

  @Test
  @DisplayName("Deve processar ementa com todas as estratégias de redução")
  void testProcessarEmentaIndividual() {
    String ementa = "DISPÕE SOBRE A CRIAÇÃO DO PARQUE MUNICIPAL E PRESERVAÇÃO AMBIENTAL. REGULAMENTA O ACESSO PÚBLICO.";
    EmentaPreProcessada resultado = servico.processarEmentaIndividual(100L, ementa, "Decreto", corpus);

    assertNotNull(resultado);
    assertEquals(100L, resultado.getIdNorma());

    assertNotNull(resultado.getStopwordsTfidfTopk());
    assertFalse(resultado.getStopwordsTfidfTopk().isBlank());

    assertNotNull(resultado.getTfidfTopk());
    assertFalse(resultado.getTfidfTopk().isBlank());

    assertNotNull(resultado.getResumoExtrativoTfidf());
    assertFalse(resultado.getResumoExtrativoTfidf().isBlank());

    assertNotNull(resultado.getTextrank());
    assertFalse(resultado.getTextrank().isBlank());

    assertNotNull(resultado.getTfidfPosicaoSentenca());
    assertFalse(resultado.getTfidfPosicaoSentenca().isBlank());

    assertNotNull(resultado.getTfidfMmr());
    assertFalse(resultado.getTfidfMmr().isBlank());

    assertNotNull(resultado.getTfidfSimilaridadeFrases());
    assertFalse(resultado.getTfidfSimilaridadeFrases().isBlank());

    assertNotNull(resultado.getSvdLsa());
    assertFalse(resultado.getSvdLsa().isBlank());

    assertNotNull(resultado.getTfidfFeatureSelection());
    assertFalse(resultado.getTfidfFeatureSelection().isBlank());

    assertNotNull(resultado.getChiSquareTermos());
    assertFalse(resultado.getChiSquareTermos().isBlank());

    assertNotNull(resultado.getMutualInformation());
    assertFalse(resultado.getMutualInformation().isBlank());

    assertNotNull(resultado.getClusteringSentencas());
    assertFalse(resultado.getClusteringSentencas().isBlank());
  }

  @Test
  @DisplayName("Deve remover stopwords corretamente mantendo termos substantivos")
  void testStopwordsTfidf() {
    EstrategiaStopwordsTfidfTopK estrategia = new EstrategiaStopwordsTfidfTopK(5);
    String ementa = "DISPÕE SOBRE A CRIAÇÃO DE NOVAS REGRAS PARA O TRÂNSITO URBANO E DÁ OUTRAS PROVIDÊNCIAS";
    String resultado = estrategia.processar(ementa, corpus);

    assertFalse(resultado.contains("dispoe"));
    assertFalse(resultado.contains("dispõe"));
    assertFalse(resultado.contains("sobre"));
    assertTrue(resultado.contains("transito") || resultado.contains("trânsito") || resultado.contains("regras") || resultado.contains("criacao") || resultado.contains("criação"));
  }

  @Test
  @DisplayName("Deve extrair termos via TextRank com base em coocorrência")
  void testTextRank() {
    EstrategiaTextRank estrategia = new EstrategiaTextRank(3, 2);
    String ementa = "ORÇAMENTO ANUAL ESTIMANDO A RECEITA E FIXANDO A DESPESA DO MUNICÍPIO.";
    String resultado = estrategia.processar(ementa, corpus);

    assertNotNull(resultado);
    assertFalse(resultado.isBlank());
    String[] termos = resultado.split("\\s+");
    assertTrue(termos.length <= 3);
  }
}
