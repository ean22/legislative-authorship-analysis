package org.entryPoint.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.entryPoint.model.perfil.ComparativoPerfisAutores;
import org.entryPoint.model.perfil.GrafoColaboracaoPolitica;
import org.entryPoint.model.perfil.PerfilPoliticoAutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicoPerfilPoliticoTest {
  private ServicoPerfilPolitico servicoPerfil;

  @BeforeEach
  public void setUp() {
    servicoPerfil = new ServicoPerfilPolitico();
  }

  @Test
  public void testGeracaoPerfilPoliticoCompletoAutor() throws SQLException {
    // 1. Gerar perfil de um dos autores mais prolíficos
    Optional<PerfilPoliticoAutor> optPerfil = servicoPerfil.gerarPerfilAutor("FERNANDO HADDAD");
    assertTrue(optPerfil.isPresent(), "Perfil de Fernando Haddad deve ser encontrado");

    PerfilPoliticoAutor perfil = optPerfil.get();
    assertNotNull(perfil.nomeAutor());
    assertTrue(perfil.idAutor() > 0);

    // 2. Validação da Dimensão Temática
    assertNotNull(perfil.perfilTematico());
    assertTrue(perfil.perfilTematico().totalNormasComTopico() > 0, "Deve possuir normas classificadas com tópicos");
    assertNotNull(perfil.perfilTematico().topicoDominante());
    assertTrue(perfil.perfilTematico().percentualTopicoDominante() > 0);
    assertTrue(perfil.perfilTematico().indiceEspecializacaoHhi() >= 0.0 && perfil.perfilTematico().indiceEspecializacaoHhi() <= 1.0);
    assertTrue(perfil.perfilTematico().entropiaTematica() >= 0.0 && perfil.perfilTematico().entropiaTematica() <= 1.0);
    assertFalse(perfil.perfilTematico().distribuicaoTopicos().isEmpty());
    assertNotNull(perfil.perfilTematico().classificarNivelEspecializacao());

    // 3. Validação da Dimensão de Atuação Legislativa
    assertNotNull(perfil.perfilAtuacao());
    assertTrue(perfil.perfilAtuacao().totalNormas() > 0);
    assertTrue(perfil.perfilAtuacao().normasColetivas() > 0 || perfil.perfilAtuacao().normasIndividuais() > 0);
    assertTrue(perfil.perfilAtuacao().taxaCoautoria() >= 0.0 && perfil.perfilAtuacao().taxaCoautoria() <= 100.0);
    assertTrue(perfil.perfilAtuacao().mediaAutoresPorNorma() >= 1.0);
    assertFalse(perfil.perfilAtuacao().distribuicaoTiposNormas().isEmpty());
    assertNotNull(perfil.perfilAtuacao().classificarPadraoAutoria());

    // 4. Validação da Dimensão de Colaboração Política
    assertNotNull(perfil.perfilColaboracao());
    assertTrue(perfil.perfilColaboracao().totalParceirosDistintos() > 0);
    assertFalse(perfil.perfilColaboracao().principaisParcerias().isEmpty());
    assertTrue(perfil.perfilColaboracao().intensidadeMediaColaboracao() > 0.0);
    assertNotNull(perfil.perfilColaboracao().classificarDensidadeRede());

    // 5. Validação da Dimensão Temporal e de Evolução
    assertNotNull(perfil.perfilTemporal());
    assertTrue(perfil.perfilTemporal().totalAnosAtivos() > 0);
    assertTrue(perfil.perfilTemporal().anoPico() >= 2000);
    assertTrue(perfil.perfilTemporal().volumeAnoPico() > 0);
    assertFalse(perfil.perfilTemporal().producaoPorAno().isEmpty());

    // 6. Validação do Relatório Visual
    String relatorio = perfil.gerarRelatorioVisual();
    assertNotNull(relatorio);
    assertTrue(relatorio.contains("PERFIL POLÍTICO LEGISLATIVO: FERNANDO HADDAD"));
    assertTrue(relatorio.contains("PERFIL TEMÁTICO LEGISLATIVO"));
    assertTrue(relatorio.contains("PERFIL DE ATUAÇÃO LEGISLATIVA"));
    assertTrue(relatorio.contains("PERFIL DE COLABORAÇÃO POLÍTICA"));
    assertTrue(relatorio.contains("PERFIL TEMPORAL E DE EVOLUÇÃO"));
  }

  @Test
  public void testGeracaoPerfisMultiplos() throws SQLException {
    List<PerfilPoliticoAutor> perfis = servicoPerfil.gerarPerfisPrincipaisAutores(5);
    assertFalse(perfis.isEmpty());
    assertTrue(perfis.size() <= 5);

    for (PerfilPoliticoAutor p : perfis) {
      assertNotNull(p.nomeAutor());
      assertTrue(p.perfilAtuacao().totalNormas() > 0);
    }
  }

  @Test
  public void testConstrucaoGrafoColaboracaoGlobalEExportacoes() throws SQLException, IOException {
    GrafoColaboracaoPolitica grafo = servicoPerfil.construirGrafoColaboracao(20, 20);
    assertNotNull(grafo);
    assertFalse(grafo.nos().isEmpty(), "O grafo deve conter nós de atores políticos");
    assertFalse(grafo.arestas().isEmpty(), "O grafo deve conter arestas de colaboração");

    // 1. Exportação DOT (Graphviz)
    String dot = grafo.exportarDot("RedePoliticaSP", 20);
    assertNotNull(dot);
    assertTrue(dot.startsWith("graph RedePoliticaSP {"));
    assertTrue(dot.contains("--"));

    // 2. Exportação JSON (D3.js / Vis.js)
    String json = grafo.exportarJsonD3(20);
    assertNotNull(json);
    assertTrue(json.contains("\"nodes\": ["));
    assertTrue(json.contains("\"links\": ["));

    // 3. Visualização em Console
    String visualConsole = grafo.gerarVisualizacaoConsole(10);
    assertNotNull(visualConsole);
    assertTrue(visualConsole.contains("REDE DE COLABORAÇÃO POLÍTICA"));
    assertTrue(visualConsole.contains("PRINCIPAIS HUBS POLÍTICOS"));

    // 4. Teste de gravação em arquivo temporário
    File tempDot = File.createTempFile("teste_grafo", ".dot");
    File tempJson = File.createTempFile("teste_grafo", ".json");
    try {
      servicoPerfil.exportarGrafoDot(grafo, tempDot.getAbsolutePath(), 20);
      servicoPerfil.exportarGrafoJson(grafo, tempJson.getAbsolutePath(), 20);
      assertTrue(tempDot.exists() && tempDot.length() > 0);
      assertTrue(tempJson.exists() && tempJson.length() > 0);
    } finally {
      tempDot.delete();
      tempJson.delete();
    }
  }

  @Test
  public void testGrafoColaboracaoEgoAutor() throws SQLException {
    Optional<GrafoColaboracaoPolitica> optGrafo = servicoPerfil.construirGrafoColaboracaoAutor(1731, 15); // Fernando Haddad
    assertTrue(optGrafo.isPresent());

    GrafoColaboracaoPolitica grafo = optGrafo.get();
    assertFalse(grafo.nos().isEmpty());
    assertFalse(grafo.arestas().isEmpty());

    // Pelo menos o nó central (id 1731) deve estar presente
    boolean contemCentro = grafo.nos().stream().anyMatch(n -> n.id() == 1731);
    assertTrue(contemCentro);
  }

  @Test
  public void testComparativoPerfisAutores() throws SQLException {
    // Comparar Fernando Haddad e Ricardo Nunes
    Optional<ComparativoPerfisAutores> optComp = servicoPerfil.compararAutores(1731, 2006);
    assertTrue(optComp.isPresent());

    ComparativoPerfisAutores comp = optComp.get();
    assertEquals("FERNANDO HADDAD", comp.autor1().nomeAutor());
    assertEquals("RICARDO NUNES", comp.autor2().nomeAutor());
    assertTrue(comp.similaridadeTematicaCosseno() >= 0.0 && comp.similaridadeTematicaCosseno() <= 1.0);
    assertNotNull(comp.diagnosticoAfinidade());

    String relatorioComp = comp.gerarRelatorioComparativo();
    assertNotNull(relatorioComp);
    assertTrue(relatorioComp.contains("COMPARAÇÃO POLÍTICA LEGISLATIVA"));
    assertTrue(relatorioComp.contains("Similaridade Temática Cosseno"));
  }

  private void assertEquals(String expected, String actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }
}
