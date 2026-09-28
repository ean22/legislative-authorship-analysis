package org.entryPoint.service.preprocessamento;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.entryPoint.model.EmentaPreProcessada;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioBancoDados.NormaComEmenta;

public class ServicoPreProcessamentoEmentas {
  private final RepositorioBancoDados repositorio;

  private final EstrategiaStopwordsTfidfTopK estrategiaStopwordsTfidf = new EstrategiaStopwordsTfidfTopK(7);
  private final EstrategiaTfidfTopK estrategiaTfidfTopK = new EstrategiaTfidfTopK(7);
  private final EstrategiaResumoExtrativoTfidf estrategiaResumoTfidf = new EstrategiaResumoExtrativoTfidf(1);
  private final EstrategiaTextRank estrategiaTextRank = new EstrategiaTextRank(7, 3);
  private final EstrategiaTfidfPosicaoSentenca estrategiaTfidfPosicao = new EstrategiaTfidfPosicaoSentenca(1);
  private final EstrategiaTfidfMmr estrategiaTfidfMmr = new EstrategiaTfidfMmr(1, 0.65);
  private final EstrategiaTfidfSimilaridadeFrases estrategiaSimilaridadeFrases = new EstrategiaTfidfSimilaridadeFrases(1);
  private final EstrategiaSvdLsa estrategiaSvdLsa = new EstrategiaSvdLsa(7);
  private final EstrategiaTfidfFeatureSelection estrategiaFeatureSelection = new EstrategiaTfidfFeatureSelection(2, 8);
  private final EstrategiaChiSquare estrategiaChiSquare = new EstrategiaChiSquare(7);
  private final EstrategiaMutualInformation estrategiaMutualInformation = new EstrategiaMutualInformation(7);
  private final EstrategiaClusteringSentencas estrategiaClusteringSentencas = new EstrategiaClusteringSentencas(2);

  public ServicoPreProcessamentoEmentas() {
    this.repositorio = new RepositorioBancoDados();
  }

  public ServicoPreProcessamentoEmentas(RepositorioBancoDados repositorio) {
    this.repositorio = repositorio;
  }

  public void processarTodasEmentas() {
    System.out.println("==========================================================");
    System.out.println("Iniciando pré-processamento e redução textual das ementas");
    System.out.println("==========================================================");

    try {
      List<NormaComEmenta> normas = repositorio.listarNormasComEmenta();
      if (normas.isEmpty()) {
        System.out.println("Nenhuma ementa encontrada para processamento.");
        return;
      }

      System.out.printf("Total de normas com ementa carregadas: %d%n", normas.size());

      // 1. Treinamento das estatísticas do Corpus (TF-IDF, Chi-Square, MI)
      System.out.println("Calculando estatísticas globais do corpus (TF-IDF, Chi-Square, MI)...");
      CorpusStats corpus = new CorpusStats();
      List<CorpusStats.DocumentoInput> corpusInput = normas.stream()
          .map(n -> new CorpusStats.DocumentoInput(n.id(), n.ementa(), n.tipoEscrito()))
          .toList();
      corpus.treinar(corpusInput);
      System.out.printf("Corpus treinado com sucesso (%d documentos analisados).%n", corpus.getTotalDocumentos());

      // 2. Aplicação das estratégias e geração dos registros
      System.out.println("Aplicando estratégias de redução textual...");
      List<EmentaPreProcessada> lote = new ArrayList<>();
      int totalProcessadas = 0;
      int tamanhoLote = 500;

      for (NormaComEmenta n : normas) {
        String ementaOriginal = n.ementa();

        EmentaPreProcessada pre = EmentaPreProcessada.builder()
            .idNorma(n.id())
            .stopwordsTfidfTopk(estrategiaStopwordsTfidf.processar(ementaOriginal, corpus))
            .tfidfTopk(estrategiaTfidfTopK.processar(ementaOriginal, corpus))
            .resumoExtrativoTfidf(estrategiaResumoTfidf.processar(ementaOriginal, corpus))
            .textrank(estrategiaTextRank.processar(ementaOriginal, corpus))
            .tfidfPosicaoSentenca(estrategiaTfidfPosicao.processar(ementaOriginal, corpus))
            .tfidfMmr(estrategiaTfidfMmr.processar(ementaOriginal, corpus))
            .tfidfSimilaridadeFrases(estrategiaSimilaridadeFrases.processar(ementaOriginal, corpus))
            .svdLsa(estrategiaSvdLsa.processar(ementaOriginal, corpus))
            .tfidfFeatureSelection(estrategiaFeatureSelection.processar(ementaOriginal, corpus))
            .chiSquareTermos(estrategiaChiSquare.processar(ementaOriginal, corpus))
            .mutualInformation(estrategiaMutualInformation.processar(ementaOriginal, corpus))
            .clusteringSentencas(estrategiaClusteringSentencas.processar(ementaOriginal, corpus))
            .build();

        lote.add(pre);
        totalProcessadas++;

        if (lote.size() >= tamanhoLote) {
          repositorio.salvarEmentasPreProcessadasEmLote(lote);
          System.out.printf("Processadas %d de %d ementas (%.1f%%)...%n",
              totalProcessadas, normas.size(), (totalProcessadas * 100.0) / normas.size());
          lote.clear();
        }
      }

      if (!lote.isEmpty()) {
        repositorio.salvarEmentasPreProcessadasEmLote(lote);
        lote.clear();
      }

      long totalNoBanco = repositorio.contarEmentasPreProcessadas();
      System.out.println("==========================================================");
      System.out.printf("Concluído! Total de ementas pré-processadas no banco: %d%n", totalNoBanco);
      System.out.println("==========================================================");

    } catch (SQLException e) {
      System.err.println("Erro ao processar ementas no banco de dados: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public EmentaPreProcessada processarEmentaIndividual(long idNorma, String ementa, String tipoNorma, CorpusStats corpus) {
    return EmentaPreProcessada.builder()
        .idNorma(idNorma)
        .stopwordsTfidfTopk(estrategiaStopwordsTfidf.processar(ementa, corpus))
        .tfidfTopk(estrategiaTfidfTopK.processar(ementa, corpus))
        .resumoExtrativoTfidf(estrategiaResumoTfidf.processar(ementa, corpus))
        .textrank(estrategiaTextRank.processar(ementa, corpus))
        .tfidfPosicaoSentenca(estrategiaTfidfPosicao.processar(ementa, corpus))
        .tfidfMmr(estrategiaTfidfMmr.processar(ementa, corpus))
        .tfidfSimilaridadeFrases(estrategiaSimilaridadeFrases.processar(ementa, corpus))
        .svdLsa(estrategiaSvdLsa.processar(ementa, corpus))
        .tfidfFeatureSelection(estrategiaFeatureSelection.processar(ementa, corpus))
        .chiSquareTermos(estrategiaChiSquare.processar(ementa, corpus))
        .mutualInformation(estrategiaMutualInformation.processar(ementa, corpus))
        .clusteringSentencas(estrategiaClusteringSentencas.processar(ementa, corpus))
        .build();
  }
}
