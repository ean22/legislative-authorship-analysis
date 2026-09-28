package org.entryPoint.service.preprocessamento;

public interface EstrategiaReducao {
  String getNome();
  String processar(String ementa, CorpusStats corpus);
}
