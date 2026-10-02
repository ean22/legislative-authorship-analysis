package org.entryPoint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmentaPreProcessada {
  private Long id;
  private long idNorma;
  private String stopwordsTfidfTopk;
  private String tfidfTopk;
  private String resumoExtrativoTfidf;
  private String textrank;
  private String tfidfPosicaoSentenca;
  private String tfidfMmr;
  private String tfidfSimilaridadeFrases;
  private String svdLsa;
  private String tfidfFeatureSelection;
  private String chiSquareTermos;
  private String mutualInformation;
  private String clusteringSentencas;
  private String embeddings;
}
