package org.entryPoint.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicoAfericaoAssertividadeTest {

  @Test
  void testLocalizarELerCsv() throws IOException {
    File csv = ServicoAfericaoAssertividade.localizarArquivoCsvPadrao();
    assertTrue(csv.exists(), "O arquivo CSV de normas classificadas deve existir no repositório");

    ServicoAfericaoAssertividade servico = new ServicoAfericaoAssertividade();
    List<ServicoAfericaoAssertividade.NormaCsv> normas = servico.lerNormasDoCsv(csv);

    assertEquals(500, normas.size(), "O CSV deve conter exatamente 500 normas classificadas");

    for (ServicoAfericaoAssertividade.NormaCsv n : normas) {
      assertNotNull(n.normaOriginal(), "Norma original não pode ser nula");
      assertTrue(n.numero() > 0, "Número da norma deve ser positivo: " + n.normaOriginal());
      assertTrue(n.ano() >= 2013 && n.ano() <= 2026, "Ano da norma deve ser válido: " + n.normaOriginal());
      assertNotNull(n.classeEsperada(), "Classe esperada não pode ser nula");
      assertFalse(n.classeEsperada().isBlank(), "Classe esperada não pode ser vazia");
    }
  }

  @Test
  void testEquivalenciasSemanticas() {
    assertTrue(ServicoAfericaoAssertividade.saoClassesEquivalentes(
        "Bens Imóveis e Desapropriação",
        "Gestão Imobiliária, Habitação e Regularização Fundiária"
    ));

    assertTrue(ServicoAfericaoAssertividade.saoClassesEquivalentes(
        "Gestão Imobiliária, Habitação e Regularização Fundiária",
        "Habitação e Regularização Fundiária"
    ));

    assertTrue(ServicoAfericaoAssertividade.saoClassesEquivalentes(
        "Crédito Orçamentário e Finanças",
        "crédito orçamentário e finanças"
    ));

    assertFalse(ServicoAfericaoAssertividade.saoClassesEquivalentes(
        "Crédito Orçamentário e Finanças",
        "Saúde Pública e Assistência Hospitalar"
    ));
  }

  @Test
  void testAfericaoCompletaComBanco() throws IOException, SQLException {
    ServicoAfericaoAssertividade servico = new ServicoAfericaoAssertividade();
    var relatorio = servico.aferirAssertividade();

    assertNotNull(relatorio, "O relatório não pode ser nulo");
    assertEquals(500, relatorio.totalNormasValidasCsv());
    assertEquals(500, relatorio.totalEncontradasNoBanco(), "Todas as 500 normas devem constar no banco");
    assertTrue(relatorio.totalClassificadasNoBanco() > 0, "Deve haver normas com tópicos atribuídos no banco");
    assertTrue(relatorio.acuraciaExata() >= 0.0, "A acurácia exata deve ser calculada");
    assertTrue(relatorio.acuraciaEquivalente() >= 0.0, "A acurácia equivalente deve ser calculada");
    assertTrue(relatorio.f1Macro() >= 0.0, "O F1-Macro deve ser calculado");
    assertTrue(relatorio.f1Weighted() >= 0.0, "O F1-Weighted deve ser calculado");

    // Testar exportação em Markdown
    File tempMd = File.createTempFile("teste_assertividade_", ".md");
    tempMd.deleteOnExit();

    File exportado = servico.exportarRelatorioMarkdown(relatorio, tempMd);
    assertTrue(exportado.exists());
    assertTrue(exportado.length() > 500, "O arquivo Markdown deve conter conteúdo substancial");
  }

  @Test
  void testResolucaoRevogacoesPorHerancaRelacional() throws SQLException, IOException {
    ServicoPipelineLegislativo pipeline = new ServicoPipelineLegislativo();
    int resolvidas = pipeline.resolverRevogacoesPorHerancaRelacional();
    System.out.printf("Revogações resolvidas por herança relacional no banco: %d%n", resolvidas);

    ServicoAfericaoAssertividade servico = new ServicoAfericaoAssertividade();
    var relatorio = servico.aferirAssertividade();
    assertNotNull(relatorio);
    assertEquals(495, relatorio.totalClassificadasNoBanco(),
        "Exatamente 495 normas devem estar classificadas (475 originais + 20 revogações resolvidas por herança relacional)");
    assertTrue(relatorio.acuraciaEquivalente() > 0.518, "A acurácia equivalente deve superar os 51.80% do baseline");
    servico.imprimirRelatorio(relatorio);

    File relatorioMd = new File("relatorio_assertividade_classificacao.md");
    servico.exportarRelatorioMarkdown(relatorio, relatorioMd);
    File pai = new File("..").getCanonicalFile();
    if ("entryPoint".equals(pai.getName()) || "legislative-authorship-analysis".equals(pai.getName())) {
      servico.exportarRelatorioMarkdown(relatorio, new File(pai, "relatorio_assertividade_classificacao.md"));
    }
  }
}
