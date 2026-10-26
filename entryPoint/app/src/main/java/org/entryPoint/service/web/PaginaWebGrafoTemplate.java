package org.entryPoint.service.web;

/**
 * Fornece a interface web completa, moderna e interativa (Single Page Application)
 * para exploração visual das redes de colaboração política e perfis legislativos.
 */
public class PaginaWebGrafoTemplate {

  public static String gerarHtml() {
    return new StringBuilder(85000)
        .append(PARTE_1_HEAD_CSS)
        .append(PARTE_2_BODY_DOM)
        .append(PARTE_3_JS_CORE)
        .append(PARTE_4_JS_MODAIS)
        .toString();
  }

  public static String gerarHtml(String jsonGrafoEmbutido, String jsonAutoresEmbutido, boolean modoServidor) {
    return gerarHtml();
  }

  public static String gerarHtml(
      String jsonGrafoEmbutido,
      String jsonAutoresEmbutido,
      String jsonStatsEmbutido,
      String jsonTopicosEmbutido,
      String jsonPerfisEmbutido,
      boolean modoServidor
  ) {
    return gerarHtml();
  }

  private static final String PARTE_1_HEAD_CSS = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Mapeamento de Coautoria e Perfis Políticos | Análise Legislativa</title>

  <!-- Fontes Modernas: Outfit (Títulos) e Inter (Interface) -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Outfit:wght@400;500;600;700;800&display=swap" rel="stylesheet">

  <!-- Vis.js Network para Simulação Física Interativa de Grafos -->
  <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.9/dist/vis-network.min.js"></script>

  <style>
    :root {
      /* Paleta Monocromática Sofisticada (Escala Black / Neutral / White) */
      --bg-base: #09090B;
      --bg-surface: rgba(18, 18, 20, 0.94);
      --bg-surface-elevated: rgba(28, 28, 32, 0.96);
      --border-subtle: rgba(255, 255, 255, 0.12);
      --border-accent: rgba(255, 255, 255, 0.35);
      --border-active: #FFFFFF;
      --text-main: #FFFFFF;
      --text-muted: #A1A1AA;
      --text-faint: #71717A;
      --mono-black: #000000;
      --mono-card: #121214;
      --mono-card-hover: #1A1A1E;
      --mono-border: rgba(255, 255, 255, 0.14);
      --mono-white: #FFFFFF;
      --glass-blur: blur(16px);
      --shadow-elevation: 0 20px 40px -15px rgba(0, 0, 0, 0.85);
      --font-display: 'Outfit', sans-serif;
      --font-body: 'Inter', sans-serif;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg-base);
      color: var(--text-main);
      font-family: var(--font-body);
      overflow: hidden;
      height: 100vh;
      width: 100vw;
      user-select: none;
    }

    /* Fundo monocromático sutil com gradiente radial neutro */
    body::before {
      content: "";
      position: absolute;
      top: -20%;
      left: -10%;
      width: 50vw;
      height: 50vw;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.025) 0%, transparent 70%);
      pointer-events: none;
      z-index: 0;
    }
    body::after {
      content: "";
      position: absolute;
      bottom: -20%;
      right: -10%;
      width: 50vw;
      height: 50vw;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.02) 0%, transparent 70%);
      pointer-events: none;
      z-index: 0;
    }

    /* Container do Grafo Interativo (Canvas) */
    #rede-container {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: 1;
      background: radial-gradient(circle at center, #141416 0%, #09090B 100%);
    }

    /* Barra Superior (Header Glassmorphism Monocromático) */
    header {
      position: absolute;
      top: 16px;
      left: 20px;
      right: 20px;
      height: 64px;
      background: var(--bg-surface);
      backdrop-filter: var(--glass-blur);
      border: 1px solid var(--border-subtle);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 24px;
      z-index: 10;
      box-shadow: var(--shadow-elevation);
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .brand-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, #27272A, #09090B);
      border: 1px solid rgba(255, 255, 255, 0.18);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      box-shadow: 0 0 15px rgba(255, 255, 255, 0.08);
    }

    .brand-title {
      font-family: var(--font-display);
      font-size: 18px;
      font-weight: 700;
      letter-spacing: -0.02em;
      color: #FFFFFF;
    }

    .brand-subtitle {
      font-size: 11px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .stats-chips {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .chip {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--border-subtle);
      border-radius: 20px;
      padding: 6px 14px;
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 6px;
      color: var(--text-muted);
    }

    .chip b {
      color: #FFFFFF;
      font-weight: 600;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .btn {
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-main);
      border: 1px solid rgba(255, 255, 255, 0.14);
      border-radius: 10px;
      padding: 8px 14px;
      font-size: 12px;
      font-weight: 500;
      font-family: var(--font-body);
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s ease;
    }

    .btn:hover {
      background: rgba(255, 255, 255, 0.14);
      border-color: rgba(255, 255, 255, 0.4);
      color: #FFFFFF;
      transform: translateY(-1px);
    }

    .btn-primary {
      background: #FFFFFF;
      border: 1px solid #FFFFFF;
      color: #09090B;
      font-weight: 600;
      box-shadow: 0 4px 14px rgba(255, 255, 255, 0.15);
    }

    .btn-primary:hover {
      background: #E4E4E7;
      border-color: #E4E4E7;
      color: #000000;
      box-shadow: 0 6px 20px rgba(255, 255, 255, 0.25);
    }

    /* Painel de Controle Flutuante (Esquerda) */
    .control-panel {
      position: absolute;
      top: 96px;
      left: 20px;
      width: 320px;
      background: var(--bg-surface);
      backdrop-filter: var(--glass-blur);
      border: 1px solid var(--border-subtle);
      border-radius: 16px;
      padding: 20px;
      z-index: 10;
      box-shadow: var(--shadow-elevation);
      max-height: calc(100vh - 120px);
      overflow-y: auto;
    }

    .panel-section {
      margin-bottom: 20px;
    }

    .panel-section:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-family: var(--font-display);
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--text-muted);
      margin-bottom: 10px;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Campo de Busca de Autor */
    .search-box {
      position: relative;
    }

    .search-input {
      width: 100%;
      background: rgba(0, 0, 0, 0.45);
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      padding: 10px 14px 10px 34px;
      color: #FFFFFF;
      font-size: 13px;
      outline: none;
      transition: all 0.2s ease;
    }

    .search-input:focus {
      border-color: #FFFFFF;
      box-shadow: 0 0 12px rgba(255, 255, 255, 0.15);
    }

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-faint);
      font-size: 14px;
      pointer-events: none;
    }

    .autocomplete-list {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: #121214;
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      margin-top: 6px;
      max-height: 200px;
      overflow-y: auto;
      z-index: 20;
      display: none;
      box-shadow: var(--shadow-elevation);
    }

    .autocomplete-item {
      padding: 10px 14px;
      font-size: 12px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      border-bottom: 1px solid rgba(255, 255, 255, 0.03);
    }

    .autocomplete-item:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #FFFFFF;
    }

    /* Sliders de Filtro Monocromáticos */
    .slider-group {
      margin-top: 12px;
    }

    .slider-label {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 6px;
      color: var(--text-muted);
    }

    .slider-label span {
      color: #FFFFFF;
      font-weight: 600;
    }

    .custom-range {
      width: 100%;
      accent-color: #FFFFFF;
      cursor: pointer;
    }

    /* Dropdown de Temas */
    .select-theme {
      width: 100%;
      background: rgba(0, 0, 0, 0.45);
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      padding: 10px 14px;
      color: #FFFFFF;
      font-size: 12px;
      outline: none;
      cursor: pointer;
    }

    .select-theme:focus {
      border-color: #FFFFFF;
      box-shadow: 0 0 12px rgba(255, 255, 255, 0.15);
    }

    /* Legenda de Cores Temáticas */
    .legend-list {
      display: flex;
      flex-direction: column;
      gap: 6px;
      max-height: 160px;
      overflow-y: auto;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 11px;
      color: var(--text-muted);
      cursor: pointer;
      padding: 4px 6px;
      border-radius: 6px;
      transition: background 0.15s;
    }

    .legend-item:hover {
      background: rgba(255, 255, 255, 0.08);
      color: var(--text-main);
    }

    .color-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    /* Drawer Lateral de Detalhes do Autor (Direita) */
    .detail-drawer {
      position: absolute;
      top: 96px;
      right: 20px;
      width: 420px;
      background: var(--bg-surface);
      backdrop-filter: var(--glass-blur);
      border: 1px solid var(--border-subtle);
      border-radius: 16px;
      padding: 24px;
      z-index: 10;
      box-shadow: var(--shadow-elevation);
      max-height: calc(100vh - 120px);
      overflow-y: auto;
      transform: translateX(460px);
      transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .detail-drawer.open {
      transform: translateX(0);
    }

    .drawer-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: 16px;
    }

    .author-name {
      font-family: var(--font-display);
      font-size: 20px;
      font-weight: 700;
      color: #FFFFFF;
      line-height: 1.2;
    }

    .author-cargo {
      font-size: 12px;
      color: #A1A1AA;
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .close-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      font-size: 20px;
      cursor: pointer;
      line-height: 1;
      padding: 4px;
      border-radius: 6px;
    }

    .close-btn:hover {
      color: #FFFFFF;
      background: rgba(255, 255, 255, 0.1);
    }

    /* Abas do Perfil */
    .tabs-nav {
      display: flex;
      gap: 6px;
      border-bottom: 1px solid var(--border-subtle);
      margin-bottom: 16px;
    }

    .tab-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      font-size: 12px;
      font-weight: 600;
      padding: 8px 12px;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.2s;
    }

    .tab-btn.active {
      color: #FFFFFF;
      border-color: #FFFFFF;
    }

    .tab-content {
      display: none;
    }

    .tab-content.active {
      display: block;
    }

    /* Barras de Tópicos */
    .topic-bar-item {
      margin-bottom: 12px;
    }

    .topic-bar-info {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 4px;
    }

    .topic-bar-bg {
      height: 6px;
      background: rgba(255, 255, 255, 0.06);
      border-radius: 4px;
      overflow: hidden;
    }

    .topic-bar-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.6s ease;
    }

    /* Tags de Palavras-Chave */
    .keywords-cloud {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 10px;
    }

    .keyword-tag {
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.15);
      color: #E4E4E7;
      border-radius: 12px;
      padding: 3px 10px;
      font-size: 11px;
    }

    /* Cards de Estatísticas */
    .stats-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 16px;
    }

    .stat-card {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      padding: 12px;
    }

    .stat-card-title {
      font-size: 11px;
      color: var(--text-muted);
      margin-bottom: 4px;
    }

    .stat-card-value {
      font-family: var(--font-display);
      font-size: 18px;
      font-weight: 700;
      color: #FFFFFF;
    }

    /* Lista de Parcerias */
    .partner-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      margin-bottom: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .partner-item:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.3);
      transform: translateX(4px);
    }

    .partner-name {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-main);
    }

    .partner-cargo {
      font-size: 10px;
      color: var(--text-faint);
    }

    .partner-laws {
      font-size: 12px;
      font-weight: 700;
      color: #FFFFFF;
    }

    /* Modais Monocromáticos (Black/White) */
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.82);
      backdrop-filter: blur(8px);
      z-index: 50;
      display: none;
      align-items: center;
      justify-content: center;
    }

    .modal-overlay.open {
      display: flex;
    }

    .modal-card {
      background: var(--mono-card);
      border: 1px solid var(--mono-border);
      border-radius: 20px;
      box-shadow: var(--shadow-elevation);
    }

    /* Tooltip Customizado */
    .vis-tooltip, .popover-tooltip {
      position: absolute;
      background: rgba(18, 18, 20, 0.98);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.22);
      color: #FFFFFF;
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 12px;
      pointer-events: none;
      z-index: 60;
      box-shadow: 0 15px 35px rgba(0,0,0,0.8);
      font-family: var(--font-body);
    }

    /* Botões Flutuantes de Zoom no Canvas */
    .zoom-controls {
      position: absolute;
      bottom: 24px;
      right: 24px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      z-index: 10;
    }

    .btn-circle {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--bg-surface);
      backdrop-filter: var(--glass-blur);
      border: 1px solid var(--border-subtle);
      color: #FFFFFF;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      cursor: pointer;
      box-shadow: var(--shadow-elevation);
      transition: all 0.2s;
    }

    .btn-circle:hover {
      background: rgba(255, 255, 255, 0.18);
      border-color: #FFFFFF;
      transform: scale(1.08);
    }
  </style>
</head>
""";

  private static final String PARTE_2_BODY_DOM = """
<body>

  <!-- Header Superior Monocromático -->
  <header>
    <div class="brand">
      <div class="brand-icon">🏛️</div>
      <div>
        <div class="brand-title">Rede Legislativa Interativa</div>
        <div class="brand-subtitle">Câmara Municipal de São Paulo • Coautoria e Temas</div>
      </div>
    </div>

    <div class="stats-chips">
      <div class="chip">Atores no Grafo: <b id="stat-nos">0</b></div>
      <div class="chip">Laços no Grafo: <b id="stat-arestas">0</b></div>
      <div class="chip">Normas no Acervo: <b id="stat-normas-total">...</b></div>
      <div class="chip">Tópicos c-TF-IDF: <b id="stat-topicos-total">...</b></div>
      <div class="chip">Autores Cadastrados: <b id="stat-autores-total">...</b></div>
    </div>

    <div class="header-actions">
      <button class="btn" id="btn-reheat" title="Reiniciar Simulação Física">⚡ Reaquecer Física</button>
      <button class="btn" id="btn-scatter-modal" title="Ver Dispersão de Perfis de Autores">📊 Dispersão de Perfis</button>
      <button class="btn btn-primary" id="btn-export-png">📷 Salvar Imagem</button>
    </div>
  </header>
 
  <!-- Banner de Status de Conexão com a API do Banco -->
  <div id="status-conexao" style="display: none; position: absolute; top: 88px; left: 50%; transform: translateX(-50%); z-index: 100; background: rgba(30, 30, 34, 0.96); border: 1px solid rgba(255,255,255,0.2); color: #fff; padding: 10px 20px; border-radius: 12px; font-size: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.7); backdrop-filter: blur(8px); align-items: center; gap: 12px;">
    <span>⚠️ <b>API desconectada:</b> Para carregar os dados em tempo real do banco SQLite, inicie o servidor web com a <b>Opção 8</b> no menu do terminal (ou execute <code>./gradlew run</code>).</span>
    <button class="btn" onclick="carregarDadosIniciais()" style="background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3); color: #fff; cursor: pointer; padding: 4px 10px; border-radius: 6px; font-size: 11px;">🔄 Tentar reconectar</button>
  </div>
 
  <!-- Overlay de Carregamento Dinâmico -->
  <div id="loading-overlay" style="display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 9; background: rgba(9, 9, 11, 0.75); backdrop-filter: blur(4px); align-items: center; justify-content: center; flex-direction: column; gap: 12px; pointer-events: none;">
    <div style="font-size: 32px;">🏛️</div>
    <div style="font-size: 13px; color: #A1A1AA; font-family: var(--font-display);">Consultando dados diretamente do banco de dados...</div>
  </div>
 
  <!-- Container do Grafo -->
  <div id="rede-container"></div>

  <!-- Painel de Controle (Esquerda) -->
  <div class="control-panel">
    <div class="panel-section">
      <div class="section-title">🔍 Localizar Autor</div>
      <div class="search-box">
        <span class="search-icon">🔎</span>
        <input type="text" id="input-busca" class="search-input" placeholder="Buscar autor (ex: Haddad)...">
        <div id="autocomplete-box" class="autocomplete-list"></div>
      </div>
    </div>

    <div class="panel-section">
      <div class="section-title">⚙️ Filtros da Rede</div>
      
      <div class="slider-group">
        <div class="slider-label">
          <span>Peso Mínimo de Normas</span>
          <span id="label-peso">20</span>
        </div>
        <input type="range" id="range-peso" class="custom-range" min="0" max="100" value="20" step="1">
      </div>

      <!-- Range Controlável de Mínimo e Máximo (Não limitado a Top 60) -->
      <div class="slider-group">
        <div class="slider-label">
          <span>Faixa de Autores Exibidos</span>
          <span id="label-faixa-atores">#1 ao #50 (50)</span>
        </div>
        <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 6px;">
          <div>
            <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-bottom: 2px;">
              <span>De (Posição Inicial):</span>
              <span id="label-atores-min" style="color: #FFFFFF; font-weight: 600;">#1</span>
            </div>
            <input type="range" id="range-atores-min" class="custom-range" min="1" max="387" value="1" step="1">
          </div>
          <div>
            <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-bottom: 2px;">
              <span>Até (Posição Final):</span>
              <span id="label-atores-max" style="color: #FFFFFF; font-weight: 600;">#50</span>
            </div>
            <input type="range" id="range-atores-max" class="custom-range" min="1" max="387" value="50" step="1">
          </div>
        </div>
      </div>

      <div style="margin-top: 14px;">
        <div class="slider-label">Filtrar por Tema Dominante</div>
        <select id="select-tema-filtro" class="select-theme">
          <option value="TODOS">Todos os Eixos Temáticos</option>
        </select>
      </div>
    </div>

    <div class="panel-section">
      <div class="section-title">🎨 Eixos Temáticos (c-TF-IDF)</div>
      <div class="legend-list" id="lista-legendas"></div>
    </div>
  </div>

  <!-- Drawer Lateral de Detalhes do Autor (Direita) -->
  <div id="author-drawer" class="detail-drawer">
    <div class="drawer-header">
      <div>
        <div id="drawer-nome" class="author-name">Nome do Autor</div>
        <div id="drawer-cargo" class="author-cargo">Cargo</div>
      </div>
      <button class="close-btn" id="btn-close-drawer">✕</button>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-card-title">Total de Normas</div>
        <div id="stat-total-normas" class="stat-card-value">0</div>
      </div>
      <div class="stat-card">
        <div class="stat-card-title">Parceiros Distintos</div>
        <div id="stat-parceiros-total" class="stat-card-value">0</div>
      </div>
    </div>

    <div class="tabs-nav">
      <button class="tab-btn active" data-tab="tab-temas">Temas</button>
      <button class="tab-btn" data-tab="tab-parcerias">Parcerias</button>
      <button class="tab-btn" data-tab="tab-trajetoria">Trajetória</button>
    </div>

    <!-- Aba 1: Temas -->
    <div id="tab-temas" class="tab-content active">
      <div class="section-title">Distribuição Temática das Normas</div>
      <div id="drawer-lista-topicos"></div>
    </div>

    <!-- Aba 2: Parcerias -->
    <div id="tab-parcerias" class="tab-content">
      <div class="section-title">Principais Laços de Coautoria</div>
      <div id="drawer-lista-parceiros"></div>
    </div>

    <!-- Aba 3: Trajetória -->
    <div id="tab-trajetoria" class="tab-content">
      <div class="section-title">Produção Individual ao Longo do Tempo (Por Tema)</div>
      
      <!-- Card Interativo com Botão para Abrir Histograma Modal Completo -->
      <div id="drawer-trajectory-summary" style="margin-top: 10px; margin-bottom: 14px; background: rgba(255, 255, 255, 0.03); padding: 14px; border-radius: 12px; border: 1px solid var(--border-subtle);">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px;">
          <div>
            <div style="font-size: 11px; color: var(--text-muted);">Período de Atuação</div>
            <div id="drawer-trajetoria-periodo" style="font-weight: 700; color: #FFFFFF; font-size: 14px;">-</div>
          </div>
          <div>
            <div style="font-size: 11px; color: var(--text-muted);">Ano de Pico</div>
            <div id="drawer-trajetoria-pico" style="font-weight: 700; color: #FFFFFF; font-size: 14px;">-</div>
          </div>
        </div>
        <button class="btn btn-primary" id="btn-abrir-histograma-modal" style="width: 100%; justify-content: center; font-weight: 600; padding: 10px;">
          📊 Abrir Histograma Temporal Completo
        </button>
      </div>

      <div class="section-title" style="margin-top: 14px;">Detalhamento Anual</div>
      <div id="drawer-timeline" style="margin-top: 8px;"></div>
    </div>

    <div style="margin-top: 20px; display: flex; gap: 8px;">
      <button class="btn btn-primary" id="btn-ego-network" style="flex: 1; justify-content: center;">Isolar Rede Deste Autor</button>
      <button class="btn" id="btn-reset-ego" style="display: none;">Ver Todos</button>
    </div>
  </div>

  <!-- Botões de Controle de Zoom Flutuantes -->
  <div class="zoom-controls">
    <button class="btn-circle" id="zoom-in" title="Aproximar">+</button>
    <button class="btn-circle" id="zoom-out" title="Afastar">-</button>
    <button class="btn-circle" id="zoom-fit" title="Enquadrar Toda a Rede">🎯</button>
  </div>

  <!-- Modal de Dispersão de Perfis de Autores (Scatter Plot Customizável) -->
  <div id="modal-scatter" class="modal-overlay">
    <div class="modal-card" style="width: 880px; max-width: 95vw; max-height: 92vh; overflow-y: auto; padding: 26px;">
      <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px;">
        <div>
          <h3 style="font-family: var(--font-display); font-size: 18px; color: #FFFFFF;">📊 Dispersão de Perfis Legislativos</h3>
          <p style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            Posicionamento analítico dos autores. Alterne os eixos para analisar volume total de normas, foco temático (HHI), taxa de articulação ou alinhamento por similaridade de cosseno com o acervo global.
          </p>
        </div>
        <button class="close-btn" id="btn-close-scatter">✕</button>
      </div>

      <!-- Controles de Configuração dos Eixos e Filtros -->
      <div style="display: flex; gap: 10px; align-items: flex-end; margin-bottom: 14px; flex-wrap: wrap;">
        <div style="flex: 1; min-width: 190px;">
          <label style="display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px;">Eixo Y (Vertical):</label>
          <select id="scatter-eixo-y" class="select-theme" style="padding: 7px 10px; font-size: 12px; width: 100%;">
            <optgroup label="📜 Dimensão de Volume e Atuação">
              <option value="totalNormas" selected>Quantidade Total de Normas</option>
              <option value="normasIndividuais">Normas Individuais (Solo)</option>
              <option value="normasColetivas">Normas Coletivas (Parceria)</option>
              <option value="mediaAnual">Média Anual de Produção (normas/ano)</option>
            </optgroup>
            <optgroup label="🤝 Dimensão de Articulação e Colaboração">
              <option value="taxaCoautoria">Taxa de Coautoria (%)</option>
              <option value="totalParceiros">Total de Parceiros Políticos</option>
              <option value="mediaAutoresPorNorma">Média de Signatários por Norma</option>
              <option value="intensidadeMedia">Intensidade Média por Parceria</option>
            </optgroup>
            <optgroup label="📐 Dimensão Temática">
              <option value="hhi">Especialização Temática (HHI)</option>
              <option value="entropia">Entropia Temática de Shannon</option>
              <option value="pctDominante">Concentração no Tema Dominante (%)</option>
              <option value="cosseno">Similaridade Cosseno c/ Acervo</option>
            </optgroup>
          </select>
        </div>
        <div style="flex: 1; min-width: 190px;">
          <label style="display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px;">Eixo X (Horizontal):</label>
          <select id="scatter-eixo-x" class="select-theme" style="padding: 7px 10px; font-size: 12px; width: 100%;">
            <optgroup label="📐 Dimensão Temática">
              <option value="hhi" selected>Especialização Temática (HHI)</option>
              <option value="entropia">Entropia Temática de Shannon</option>
              <option value="pctDominante">Concentração no Tema Dominante (%)</option>
              <option value="cosseno">Similaridade Cosseno c/ Acervo</option>
            </optgroup>
            <optgroup label="🤝 Dimensão de Articulação e Colaboração">
              <option value="taxaCoautoria">Taxa de Coautoria (%)</option>
              <option value="totalParceiros">Total de Parceiros Políticos</option>
              <option value="mediaAutoresPorNorma">Média de Signatários por Norma</option>
              <option value="intensidadeMedia">Intensidade Média por Parceria</option>
            </optgroup>
            <optgroup label="📜 Dimensão de Volume e Atuação">
              <option value="totalNormas">Quantidade Total de Normas</option>
              <option value="normasIndividuais">Normas Individuais (Solo)</option>
              <option value="normasColetivas">Normas Coletivas (Parceria)</option>
              <option value="mediaAnual">Média Anual de Produção (normas/ano)</option>
            </optgroup>
          </select>
        </div>
        <div>
          <button type="button" class="btn" id="btn-inverter-eixos" title="Inverter Eixos X e Y" style="padding: 7px 12px; height: 34px; font-size: 12px; display: flex; align-items: center; gap: 4px;">
            <span>⇄</span> Inverter
          </button>
        </div>
        <div style="flex: 1; min-width: 170px;">
          <label style="display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px;">Filtrar por Tema:</label>
          <select id="scatter-filtro-tema" class="select-theme" style="padding: 7px 10px; font-size: 12px; width: 100%;">
            <option value="TODOS">Todos os Eixos Temáticos</option>
          </select>
        </div>
        <div style="flex: 1; min-width: 150px;">
          <label style="display: block; font-size: 11px; color: var(--text-muted); margin-bottom: 4px;">Buscar Autor:</label>
          <input type="text" id="scatter-busca" class="search-input" placeholder="Buscar autor..." style="padding: 7px 10px 7px 28px; font-size: 12px; width: 100%;">
        </div>
        <div class="chip" id="scatter-badge-total" style="font-size: 11px; height: 34px;">0 autores</div>
      </div>

      <!-- SVG Canvas do Scatter Plot -->
      <div style="position: relative; background: radial-gradient(circle at center, #141416 0%, #09090B 100%); border-radius: 12px; border: 1px solid var(--border-subtle); padding: 12px; overflow: hidden;">
        <div id="scatter-plot-svg" style="width: 100%; height: 380px;"></div>
        <!-- Tooltip flutuante do Scatter Plot -->
        <div id="scatter-tooltip" style="display: none; position: absolute; pointer-events: none; z-index: 50; background: rgba(18, 18, 20, 0.98); border: 1px solid rgba(255,255,255,0.22); border-radius: 8px; padding: 10px 14px; font-size: 12px; color: #FFFFFF; box-shadow: 0 10px 25px rgba(0,0,0,0.8); backdrop-filter: blur(8px); max-width: 290px;"></div>
      </div>
    </div>
  </div>

  <!-- Modal de Histograma Temporal Completo e Interativo -->
  <div id="modal-histograma" class="modal-overlay">
    <div class="modal-card" style="width: 980px; max-width: 95vw; max-height: 92vh; overflow-y: auto; padding: 26px;">
      <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px;">
        <div>
          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
            <h3 id="hist-modal-autor-nome" style="font-family: var(--font-display); font-size: 20px; color: #FFFFFF;">Nome do Autor</h3>
            <span id="hist-modal-cargo" class="chip" style="font-size: 11px; text-transform: uppercase;">Cargo</span>
          </div>
          <p style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            Histograma da produção legislativa ao longo dos anos, com decomposição por categoria temática c-TF-IDF. Posicione o cursor sobre as barras para detalhes da quantidade por tema em cada ano.
          </p>
        </div>
        <button class="close-btn" id="btn-close-histograma">✕</button>
      </div>

      <!-- Badges de Resumo do Autor -->
      <div style="display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap;">
        <div class="chip">Total de Normas: <b id="hist-modal-total-normas">0</b></div>
        <div class="chip">Anos com Produção: <b id="hist-modal-total-anos">0</b></div>
        <div class="chip">Ano de Maior Produção: <b id="hist-modal-ano-pico">-</b></div>
        <div class="chip">Categorias Exploradas: <b id="hist-modal-total-categorias">0</b></div>
      </div>

      <!-- Container do Histograma SVG com Tooltip Flutuante -->
      <div style="position: relative; background: radial-gradient(circle at center, #141416 0%, #09090B 100%); border-radius: 12px; border: 1px solid var(--border-subtle); padding: 16px; overflow: hidden;">
        <div id="modal-histograma-svg" style="width: 100%; min-height: 380px;"></div>
        <!-- Tooltip Interativo do Histograma -->
        <div id="histograma-tooltip" style="display: none; position: absolute; pointer-events: none; z-index: 60; background: rgba(18, 18, 20, 0.98); border: 1px solid rgba(255,255,255,0.25); border-radius: 10px; padding: 12px 16px; font-size: 12px; color: #FFFFFF; box-shadow: 0 15px 35px rgba(0,0,0,0.85); backdrop-filter: blur(12px); min-width: 250px; max-width: 340px;"></div>
      </div>

      <!-- Legenda Interativa de Categorias -->
      <div style="margin-top: 14px;">
        <div style="font-size: 11px; text-transform: uppercase; font-weight: 600; color: var(--text-muted); margin-bottom: 8px;">Categorias Temáticas Presentes</div>
        <div id="hist-modal-legendas" style="display: flex; flex-wrap: wrap; gap: 8px; font-size: 11px;"></div>
      </div>
    </div>
  </div>
""";

  private static final String PARTE_3_JS_CORE = """
  <!-- Lógica Dinâmica da Interface (Zero Dados Hardcoded) -->
  <script>
    // Determina o endpoint base: relativo no servidor HTTP ou porta 8080 caso aberto localmente
    const API_BASE = (window.location.protocol === "http:" || window.location.protocol === "https:")
        ? ""
        : "http://localhost:8080";

    // Mapeamento categórico de alto contraste para os eixos temáticos oficiais
    const CORES_TOPICOS_MAPEADOS = {
      "Crédito Orçamentário e Finanças": "#F59E0B",      // Âmbar Dourado
      "Orçamento e Finanças": "#F59E0B",                 // Âmbar Dourado
      "Declaração de Utilidade Pública": "#A855F7",      // Roxo / Púrpura
      "Utilidade Pública": "#A855F7",                    // Roxo / Púrpura
      "Educação e Creches": "#06B6D4",                   // Ciano Elétrico
      "Educação": "#06B6D4",                             // Ciano Elétrico
      "Gestão Imobiliária, Habitação e Regularização Fundiária": "#F97316", // Laranja Coral
      "Habitação e Imóveis": "#F97316",                  // Laranja Coral
      "Bens Imóveis e Desapropriação": "#F97316",        // Laranja Coral
      "Serviços Públicos e Gestão Municipal": "#10B981", // Verde Esmeralda
      "Serviços Públicos": "#10B981",                    // Verde Esmeralda
      "Denominação de Vias e Logradouros": "#F43F5E",    // Rosa Choque / Carmesim
      "Denominação de Vias": "#F43F5E",                  // Rosa Choque / Carmesim
      "Datas Comemorativas e Calendário": "#3B82F6",      // Azul Real
      "Datas Comemorativas": "#3B82F6",                  // Azul Real
      "Saúde Pública e Assistência Hospitalar": "#14B8A6", // Verde Água / Teal
      "Saúde": "#14B8A6",                                // Verde Água / Teal
      "Transportes e Trânsito Urbano": "#E11D48",        // Vermelho / Rubi
      "Transporte e Trânsito": "#E11D48",                // Vermelho / Rubi
      "Meio Ambiente e Áreas Verdes": "#84CC16",         // Verde Limão
      "Meio Ambiente": "#84CC16",                        // Verde Limão
      "Administração Geral": "#6366F1"                   // Índigo
    };

    const CORES_PALETA_BASE = [
      "#F59E0B", "#A855F7", "#06B6D4", "#F97316", "#10B981", "#F43F5E", "#3B82F6",
      "#14B8A6", "#E11D48", "#84CC16", "#6366F1", "#EC4899", "#0EA5E9", "#EAB308"
    ];

    const mapaCoresTemas = new Map();

    function hashString(str) {
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
      }
      return Math.abs(hash);
    }

    function registrarCoresTopicos(listaTopicos) {
      if (!Array.isArray(listaTopicos)) return;
      listaTopicos.forEach((t, idx) => {
        const nome = typeof t === "string" ? t : (t.nomeTopico || t.nome);
        if (nome && !mapaCoresTemas.has(nome)) {
          if (CORES_TOPICOS_MAPEADOS[nome]) {
            mapaCoresTemas.set(nome, CORES_TOPICOS_MAPEADOS[nome]);
          } else {
            mapaCoresTemas.set(nome, CORES_PALETA_BASE[idx % CORES_PALETA_BASE.length]);
          }
        }
      });
    }

    function obterCorTema(tema) {
      if (!tema || tema === "N/D" || tema === "Geral" || tema === "Indefinido") return "#71717A";
      if (CORES_TOPICOS_MAPEADOS[tema]) return CORES_TOPICOS_MAPEADOS[tema];
      if (mapaCoresTemas.has(tema)) return mapaCoresTemas.get(tema);
      const cor = CORES_PALETA_BASE[hashString(tema) % CORES_PALETA_BASE.length];
      mapaCoresTemas.set(tema, cor);
      return cor;
    }

    let network = null;
    let nodesDataSet = null;
    let edgesDataSet = null;
    let dadosGrafoAtual = { nodes: [], links: [] };
    let autorSelecionadoId = null;
    let catalogoTopicosBanco = [];
    let perfilAutorAtual = null;

    document.addEventListener("DOMContentLoaded", () => {
      configurarEventos();
      carregarDadosIniciais();
    });

    async function carregarDadosIniciais() {
      exibirCarregando(true);
      try {
        // 1. Carrega estatísticas gerais e catálogo de tópicos direto do banco
        await carregarEstatisticasETopicos();

        // 2. Carrega o grafo de colaboração inicial direto do banco
        const rMin = document.getElementById("range-atores-min");
        const rMax = document.getElementById("range-atores-max");
        const rPeso = document.getElementById("range-peso");
        const minRank = rMin ? parseInt(rMin.value, 10) : 1;
        const maxRank = rMax ? parseInt(rMax.value, 10) : 50;
        const peso = rPeso ? parseInt(rPeso.value, 10) : 20;

        await recarregarGrafo(minRank, maxRank, peso);

        ocultarAlertaConexao();
      } catch (e) {
        console.warn("Aviso ao carregar dados do banco:", e);
        exibirAlertaConexao();
      } finally {
        exibirCarregando(false);
      }
    }

    function exibirCarregando(visivel) {
      const el = document.getElementById("loading-overlay");
      if (el) el.style.display = visivel ? "flex" : "none";
    }

    function exibirAlertaConexao() {
      const el = document.getElementById("status-conexao");
      if (el) el.style.display = "flex";
    }

    function ocultarAlertaConexao() {
      const el = document.getElementById("status-conexao");
      if (el) el.style.display = "none";
    }

    async function carregarEstatisticasETopicos() {
      const res = await fetch(`${API_BASE}/api/stats`);
      if (!res.ok) throw new Error("Falha HTTP " + res.status);
      const stats = await res.json();

      if (stats.totalNormas != null) {
        const el = document.getElementById("stat-normas-total");
        if (el) el.innerText = Number(stats.totalNormas).toLocaleString("pt-BR");
      }
      if (stats.totalTopicos != null) {
        const el = document.getElementById("stat-topicos-total");
        if (el) el.innerText = Number(stats.totalTopicos).toLocaleString("pt-BR");
      }
      if (stats.totalAutores != null) {
        const total = Number(stats.totalAutores);
        const el = document.getElementById("stat-autores-total");
        if (el) el.innerText = total.toLocaleString("pt-BR");

        // Atualiza os sliders de min e max de autores para cobrir todo o banco de dados
        const rMin = document.getElementById("range-atores-min");
        const rMax = document.getElementById("range-atores-max");
        if (rMin) rMin.max = total;
        if (rMax) {
          rMax.max = total;
          if (parseInt(rMax.value, 10) > total) rMax.value = total;
        }
        atualizarRotulosFaixa();
      }

      if (stats.topicos && Array.isArray(stats.topicos)) {
        catalogoTopicosBanco = stats.topicos;
        registrarCoresTopicos(stats.topicos);
        popularLegenda(dadosGrafoAtual, stats.topicos);
      }
    }

    function inicializarGrafo(dados) {
      const container = document.getElementById("rede-container");
      if (!dados || !dados.nodes) return;

      const nodesArray = dados.nodes.map(n => {
        const cor = obterCorTema(n.temaDominante);
        const tamanho = Math.max(14, Math.min(42, 12 + Math.log2(Math.max(1, n.totalNormas)) * 3));
        return {
          id: n.id,
          label: n.name.length > 22 ? n.name.substring(0, 20) + "..." : n.name,
          // Sem propriedade 'title' -> SUPRIME o popup flutuante no hover do nó
          value: n.totalNormas,
          size: tamanho,
          color: {
            background: cor,
            border: "#FFFFFF",
            highlight: { background: cor, border: "#FFFFFF" },
            hover: { background: cor, border: "#FFFFFF" }
          },
          font: { color: "#F3F4F6", face: "Inter", size: 11, strokeWidth: 2, strokeColor: "#09090B" },
          borderWidth: 2,
          shadow: { enabled: true, color: "rgba(0,0,0,0.6)", size: 8, x: 2, y: 2 },
          dataOriginal: n
        };
      });

      const edgesArray = (dados.links || []).map(l => {
        const peso = l.weight || 1;
        const largura = Math.min(8, 1 + Math.log10(peso + 1) * 2.2);
        return {
          from: l.source,
          to: l.target,
          value: peso,
          width: largura,
          title: `${l.sourceName} & ${l.targetName}<br><b>${peso} normas conjuntas</b><br>${l.sharedTopics || ''}`,
          color: { color: "rgba(255, 255, 255, 0.16)", highlight: "#FFFFFF", hover: "#FFFFFF" },
          smooth: { type: "curvedCW", roundness: 0.15 }
        };
      });

      nodesDataSet = new vis.DataSet(nodesArray);
      edgesDataSet = new vis.DataSet(edgesArray);

      const options = {
        nodes: {
          shape: "dot"
        },
        edges: {
          selectionWidth: 3,
          hoverWidth: 2.5
        },
        physics: {
          enabled: true,
          solver: "forceAtlas2Based",
          forceAtlas2Based: {
            gravitationalConstant: -70,
            centralGravity: 0.005, // Gravidade reduzida para eliminar rotação contínua
            springLength: 120,
            springConstant: 0.08,
            damping: 0.7          // Alto amortecimento para absorver rapidamente a energia cinética
          },
          stabilization: {
            enabled: true,
            iterations: 150,
            updateInterval: 25
          }
        },
        interaction: {
          hover: true,
          tooltipDelay: 100,
          hideEdgesOnDrag: false,
          zoomView: true
        }
      };

      network = new vis.Network(container, { nodes: nodesDataSet, edges: edgesDataSet }, options);

      document.getElementById("stat-nos").innerText = nodesArray.length;
      document.getElementById("stat-arestas").innerText = edgesArray.length;

      // PARALISAÇÃO DEFINITIVA DA SIMULAÇÃO: evita que o grafo fique girando ou derivando sozinho
      network.on("stabilizationIterationsDone", () => {
        network.setOptions({ physics: { enabled: false } });
      });
      network.on("stabilized", () => {
        network.setOptions({ physics: { enabled: false } });
      });
      network.on("dragEnd", () => {
        network.setOptions({ physics: { enabled: false } });
      });

      // Evento de Clique no Nó -> Abre Detalhes
      network.on("click", params => {
        if (params.nodes.length > 0) {
          const id = params.nodes[0];
          abrirDetalhesAutor(id);
        }
      });
    }

    function popularLegenda(dados, topicosBanco) {
      const container = document.getElementById("lista-legendas");
      const selectFiltro = document.getElementById("select-tema-filtro");
      container.innerHTML = "";
      selectFiltro.innerHTML = '<option value="TODOS">Todos os Eixos Temáticos</option>';

      const contagemNosPorTema = new Map();
      if (dados && dados.nodes) {
        dados.nodes.forEach(n => {
          if (n.temaDominante) {
            contagemNosPorTema.set(n.temaDominante, (contagemNosPorTema.get(n.temaDominante) || 0) + 1);
          }
        });
      }

      const listaParaExibir = [];
      const nomesAdicionados = new Set();
      const fonteTopicos = (Array.isArray(topicosBanco) && topicosBanco.length > 0)
          ? topicosBanco
          : catalogoTopicosBanco;

      fonteTopicos.forEach(t => {
        const nome = t.nomeTopico || t.nome;
        if (nome && !nomesAdicionados.has(nome)) {
          nomesAdicionados.add(nome);
          listaParaExibir.push({
            nome: nome,
            totalNormasBanco: t.totalNormas != null ? t.totalNormas : (t.quantidadeNormas != null ? t.quantidadeNormas : null),
            totalAutoresGrafo: contagemNosPorTema.get(nome) || 0
          });
        }
      });

      contagemNosPorTema.forEach((qtd, tema) => {
        if (!nomesAdicionados.has(tema)) {
          nomesAdicionados.add(tema);
          listaParaExibir.push({
            nome: tema,
            totalNormasBanco: null,
            totalAutoresGrafo: qtd
          });
        }
      });

      listaParaExibir.forEach(t => {
        const cor = obterCorTema(t.nome);
        const div = document.createElement("div");
        div.className = "legend-item";
        const infoSub = t.totalNormasBanco != null
            ? `${Number(t.totalNormasBanco).toLocaleString("pt-BR")} normas no acervo`
            : `${t.totalAutoresGrafo} atores na rede`;

        div.innerHTML = `
          <div style="display:flex; align-items:center; gap:8px; width:100%;">
            <span class="color-dot" style="background: ${cor}; flex-shrink:0;"></span>
            <div style="display:flex; flex-direction:column; overflow:hidden;">
              <span style="white-space:nowrap; overflow:hidden; text-overflow:ellipsis; font-weight:500;">${t.nome}</span>
              <span style="font-size:10px; color:var(--text-muted);">${infoSub}</span>
            </div>
          </div>
        `;
        div.onclick = () => destacarPorTema(t.nome);
        container.appendChild(div);

        const opt = document.createElement("option");
        opt.value = t.nome;
        opt.innerText = t.totalNormasBanco != null
            ? `${t.nome} (${Number(t.totalNormasBanco).toLocaleString("pt-BR")} leis)`
            : t.nome;
        selectFiltro.appendChild(opt);
      });
    }

    function destacarPorTema(tema) {
      if (!nodesDataSet || !edgesDataSet) return;

      if (!tema || tema === "TODOS") {
        nodesDataSet.forEach(node => {
          nodesDataSet.update({ id: node.id, opacity: 1.0 });
        });
        edgesDataSet.forEach(edge => {
          edgesDataSet.update({ id: edge.id, opacity: 1.0 });
        });
        return;
      }

      nodesDataSet.forEach(node => {
        const match = node.dataOriginal && node.dataOriginal.temaDominante === tema;
        nodesDataSet.update({
          id: node.id,
          opacity: match ? 1.0 : 0.15
        });
      });
    }

    async function abrirDetalhesAutor(id) {
      autorSelecionadoId = id;
      const drawer = document.getElementById("author-drawer");
      drawer.classList.add("open");

      const no = nodesDataSet ? nodesDataSet.get(id) : null;
      if (no) {
        document.getElementById("drawer-nome").innerText = no.dataOriginal.name;
        document.getElementById("drawer-cargo").innerText = no.dataOriginal.cargo || "AUTOR / VEREADOR";
        document.getElementById("stat-total-normas").innerText = no.dataOriginal.totalNormas || "0";
        document.getElementById("stat-parceiros-total").innerText = no.dataOriginal.degree || "0";
      }

      // Buscar perfil detalhado em tempo real diretamente do banco via API
      try {
        const res = await fetch(`${API_BASE}/api/perfil?id=${id}`);
        if (res.ok) {
          const perfil = await res.json();
          perfilAutorAtual = perfil;
          renderizarDetalhesPerfil(perfil, no);
        }
      } catch (e) {
        console.warn("Erro ao buscar perfil do autor via API:", e);
      }
    }

    function renderizarDetalhesPerfil(perfil, no) {
      if (!perfil) return;
      if (perfil.cargoPrincipal) {
        document.getElementById("drawer-cargo").innerText = perfil.cargoPrincipal;
      }
      if (perfil.perfilAtuacao) {
        document.getElementById("stat-total-normas").innerText = perfil.perfilAtuacao.totalNormas;
      }
      if (perfil.perfilColaboracao) {
        document.getElementById("stat-parceiros-total").innerText = perfil.perfilColaboracao.totalParceirosDistintos;
      }

      // Renderizar Temas
      const containerTopicos = document.getElementById("drawer-lista-topicos");
      containerTopicos.innerHTML = "";
      if (perfil.perfilTematico && perfil.perfilTematico.distribuicaoTopicos) {
        perfil.perfilTematico.distribuicaoTopicos.slice(0, 5).forEach(t => {
          const corTema = obterCorTema(t.nomeTopico);
          containerTopicos.innerHTML += `
            <div class="topic-bar-item">
              <div class="topic-bar-info">
                <span>${t.nomeTopico}</span>
                <span><b>${t.percentual.toFixed(1)}%</b> (${t.quantidadeNormas})</span>
              </div>
              <div class="topic-bar-bg">
                <div class="topic-bar-fill" style="width: ${t.percentual}%; background: ${corTema};"></div>
              </div>
            </div>
          `;
        });
      }



      // Parcerias
      const containerParceiros = document.getElementById("drawer-lista-parceiros");
      containerParceiros.innerHTML = "";
      if (perfil.perfilColaboracao && perfil.perfilColaboracao.principaisParcerias) {
        perfil.perfilColaboracao.principaisParcerias.slice(0, 8).forEach(p => {
          containerParceiros.innerHTML += `
            <div class="partner-item" onclick="focarAutor(${p.idParceiro})">
              <div>
                <div class="partner-name">${p.nomeParceiro}</div>
                <div class="partner-cargo">${p.cargoParceiro || 'AUTOR'}</div>
              </div>
              <div class="partner-laws">${p.normasCompartilhadas} leis</div>
            </div>
          `;
        });
      }

      // Trajetória: Cálculo do Resumo e Linha do Tempo Anual
      const producao = perfil.perfilTemporal ? perfil.perfilTemporal.producaoPorAno || {} : {};
      const anos = Object.keys(producao).map(Number).sort((a, b) => a - b);

      if (anos.length > 0) {
        const anoInicio = anos[0];
        const anoFim = anos[anos.length - 1];
        document.getElementById("drawer-trajetoria-periodo").innerText = `${anoInicio} - ${anoFim} (${anos.length} anos)`;

        let maxNormas = -1;
        let anoPico = "-";
        anos.forEach(ano => {
          const qtd = producao[ano];
          if (qtd > maxNormas) {
            maxNormas = qtd;
            anoPico = `${ano} (${qtd} leis)`;
          }
        });
        document.getElementById("drawer-trajetoria-pico").innerText = anoPico;
      } else {
        document.getElementById("drawer-trajetoria-periodo").innerText = "N/D";
        document.getElementById("drawer-trajetoria-pico").innerText = "N/D";
      }

      const containerTimeline = document.getElementById("drawer-timeline");
      containerTimeline.innerHTML = "";
      if (anos.length > 0) {
        anos.slice().reverse().forEach(ano => {
          const qtd = producao[ano];
          containerTimeline.innerHTML += `
            <div style="display:flex; justify-content:space-between; font-size:12px; padding:5px 0; border-bottom:1px solid rgba(255,255,255,0.06);">
              <span>Ano <b>${ano}</b></span>
              <span style="color:#FFFFFF; font-weight:600;">${qtd} normas</span>
            </div>
          `;
        });
      } else {
        containerTimeline.innerHTML = '<div style="font-size:12px; color:var(--text-muted); padding:10px 0;">Sem histórico anual registrado.</div>';
      }
    }

    function focarAutor(id) {
      if (!network || !nodesDataSet.get(id)) return;
      network.focus(id, {
        scale: 1.2,
        animation: { duration: 600, easingFunction: "easeInOutQuad" }
      });
      abrirDetalhesAutor(id);
    }

    function atualizarRotulosFaixa() {
      const rMin = document.getElementById("range-atores-min");
      const rMax = document.getElementById("range-atores-max");
      const lMin = document.getElementById("label-atores-min");
      const lMax = document.getElementById("label-atores-max");
      const lFaixa = document.getElementById("label-faixa-atores");

      if (!rMin || !rMax) return;
      let vMin = parseInt(rMin.value, 10);
      let vMax = parseInt(rMax.value, 10);

      if (vMin > vMax) {
        rMax.value = vMin;
        vMax = vMin;
      }

      if (lMin) lMin.innerText = "#" + vMin;
      if (lMax) lMax.innerText = "#" + vMax;
      if (lFaixa) lFaixa.innerText = `#${vMin} ao #${vMax} (${vMax - vMin + 1})`;
    }

    function configurarEventos() {
      const rangePeso = document.getElementById("range-peso");
      const labelPeso = document.getElementById("label-peso");
      const rangeMin = document.getElementById("range-atores-min");
      const rangeMax = document.getElementById("range-atores-max");

      rangePeso.oninput = () => {
        labelPeso.innerText = rangePeso.value;
      };
      rangePeso.onchange = () => recarregarGrafo();

      rangeMin.oninput = atualizarRotulosFaixa;
      rangeMax.oninput = atualizarRotulosFaixa;
      rangeMin.onchange = () => recarregarGrafo();
      rangeMax.onchange = () => recarregarGrafo();

      document.getElementById("select-tema-filtro").onchange = e => {
        destacarPorTema(e.target.value);
      };

      document.getElementById("btn-close-drawer").onclick = () => {
        document.getElementById("author-drawer").classList.remove("open");
      };

      // Zoom Controls
      document.getElementById("zoom-in").onclick = () => {
        if (network) network.moveTo({ scale: network.getScale() * 1.3 });
      };
      document.getElementById("zoom-out").onclick = () => {
        if (network) network.moveTo({ scale: network.getScale() * 0.7 });
      };
      document.getElementById("zoom-fit").onclick = () => {
        if (network) network.fit({ animation: { duration: 500 } });
      };

      // Abas
      document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.onclick = () => {
          document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
          document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
          btn.classList.add("active");
          document.getElementById(btn.dataset.tab).classList.add("active");
        };
      });

      // Busca Autocomplete
      const inputBusca = document.getElementById("input-busca");
      const autoBox = document.getElementById("autocomplete-box");

      inputBusca.oninput = () => {
        const termo = inputBusca.value.trim().toLowerCase();
        if (termo.length < 2) {
          autoBox.style.display = "none";
          return;
        }

        const matches = nodesDataSet ? nodesDataSet.get().filter(n => n.dataOriginal.name.toLowerCase().includes(termo)) : [];
        if (matches.length === 0) {
          autoBox.style.display = "none";
          return;
        }

        autoBox.innerHTML = "";
        matches.slice(0, 6).forEach(m => {
          const item = document.createElement("div");
          item.className = "autocomplete-item";
          item.innerHTML = `<span>${m.dataOriginal.name}</span><small style="color:#A1A1AA;">${m.dataOriginal.totalNormas} normas</small>`;
          item.onclick = () => {
            inputBusca.value = m.dataOriginal.name;
            autoBox.style.display = "none";
            focarAutor(m.id);
          };
          autoBox.appendChild(item);
        });
        autoBox.style.display = "block";
      };

      // Modal Dispersão de Perfis (Scatter Plot)
      const modalScatter = document.getElementById("modal-scatter");
      document.getElementById("btn-scatter-modal").onclick = () => abrirModalScatter();
      document.getElementById("btn-close-scatter").onclick = () => modalScatter.classList.remove("open");
      document.getElementById("scatter-eixo-y").onchange = () => desenharScatterPlot();
      document.getElementById("scatter-eixo-x").onchange = () => desenharScatterPlot();
      document.getElementById("scatter-busca").oninput = () => desenharScatterPlot();
      document.getElementById("scatter-filtro-tema").onchange = () => desenharScatterPlot();
      const btnInverter = document.getElementById("btn-inverter-eixos");
      if (btnInverter) {
        btnInverter.onclick = () => {
          const selX = document.getElementById("scatter-eixo-x");
          const selY = document.getElementById("scatter-eixo-y");
          if (selX && selY) {
            const temp = selX.value;
            selX.value = selY.value;
            selY.value = temp;
            desenharScatterPlot();
          }
        };
      }

      // Modal Histograma Temporal Completo
      const modalHistograma = document.getElementById("modal-histograma");
      document.getElementById("btn-abrir-histograma-modal").onclick = () => {
        if (perfilAutorAtual) abrirModalHistograma(perfilAutorAtual);
      };
      document.getElementById("btn-close-histograma").onclick = () => modalHistograma.classList.remove("open");

      // Reaquecer Física (permite reacelerar temporariamente até estabilizar e congelar novamente)
      document.getElementById("btn-reheat").onclick = () => {
        if (network) {
          network.setOptions({ physics: { enabled: true } });
          network.startSimulation();
        }
      };

      // Exportar Imagem PNG
      document.getElementById("btn-export-png").onclick = () => {
        const canvas = document.querySelector("#rede-container canvas");
        if (canvas) {
          const link = document.createElement("a");
          link.download = "rede_colaboracao_legislativa.png";
          link.href = canvas.toDataURL("image/png");
          link.click();
        }
      };

      // Ego-Network Button
      document.getElementById("btn-ego-network").onclick = async () => {
        if (!autorSelecionadoId) return;
        try {
          const res = await fetch(`${API_BASE}/api/grafo/autor?id=${autorSelecionadoId}&minWeight=10`);
          if (res.ok) {
            const grafoEgo = await res.json();
            inicializarGrafo(grafoEgo);
            popularLegenda(grafoEgo, catalogoTopicosBanco);
            document.getElementById("btn-reset-ego").style.display = "inline-flex";
          }
        } catch (e) {
          console.error("Erro ao carregar ego-network:", e);
        }
      };

      document.getElementById("btn-reset-ego").onclick = () => {
        recarregarGrafo();
        document.getElementById("btn-reset-ego").style.display = "none";
      };
    }

    async function recarregarGrafo(minRank, maxRank, peso) {
      const rMin = document.getElementById("range-atores-min");
      const rMax = document.getElementById("range-atores-max");
      const rPeso = document.getElementById("range-peso");

      if (minRank == null) minRank = rMin ? parseInt(rMin.value, 10) : 1;
      if (maxRank == null) maxRank = rMax ? parseInt(rMax.value, 10) : 50;
      if (peso == null) peso = rPeso ? parseInt(rPeso.value, 10) : 20;

      try {
        const res = await fetch(`${API_BASE}/api/grafo?minRank=${minRank}&maxRank=${maxRank}&minWeight=${peso}`);
        if (res.ok) {
          const novosDados = await res.json();
          dadosGrafoAtual = novosDados;
          inicializarGrafo(novosDados);
          popularLegenda(novosDados, catalogoTopicosBanco);
        }
      } catch (e) {
        console.error("Erro ao recarregar via API:", e);
        throw e;
      }
    }
""";

  private static final String PARTE_4_JS_MODAIS = """
    /* =========================================================================
     * MODAL DO HISTOGRAMA TEMPORAL COMPLETO E INTERATIVO
     * ========================================================================= */
    function abrirModalHistograma(perfil) {
      if (!perfil) return;
      const modal = document.getElementById("modal-histograma");
      modal.classList.add("open");

      document.getElementById("hist-modal-autor-nome").innerText = perfil.nomeAutor || "Autor";
      document.getElementById("hist-modal-cargo").innerText = perfil.cargoPrincipal || "Autor Legislativo";

      const totalNormas = perfil.perfilAtuacao ? perfil.perfilAtuacao.totalNormas : 0;
      document.getElementById("hist-modal-total-normas").innerText = totalNormas;

      const perfilTemporal = perfil.perfilTemporal || {};
      const temasPorAno = perfilTemporal.temasPorAno || {};
      const producaoPorAno = perfilTemporal.producaoPorAno || {};

      const anos = Array.from(new Set([
        ...Object.keys(temasPorAno),
        ...Object.keys(producaoPorAno)
      ])).map(Number).sort((a, b) => a - b);

      document.getElementById("hist-modal-total-anos").innerText = anos.length;

      let maxAnoNormas = 0;
      let anoPico = "-";
      const todosTemasPresentes = new Set();

      anos.forEach(ano => {
        const temas = temasPorAno[ano] || {};
        let totalAno = 0;
        for (const [t, qtd] of Object.entries(temas)) {
          totalAno += qtd;
          todosTemasPresentes.add(t);
        }
        if (totalAno === 0 && producaoPorAno[ano]) {
          totalAno = producaoPorAno[ano];
          todosTemasPresentes.add("Geral");
        }
        if (totalAno > maxAnoNormas) {
          maxAnoNormas = totalAno;
          anoPico = `${ano} (${totalAno} normas)`;
        }
      });

      document.getElementById("hist-modal-ano-pico").innerText = anoPico;
      document.getElementById("hist-modal-total-categorias").innerText = todosTemasPresentes.size;

      renderizarHistogramaModal(perfilTemporal, anos, maxAnoNormas, todosTemasPresentes);
    }

    function renderizarHistogramaModal(perfilTemporal, anos, maxAnoNormas, todosTemasPresentes) {
      const container = document.getElementById("modal-histograma-svg");
      const tooltip = document.getElementById("histograma-tooltip");
      const legendContainer = document.getElementById("hist-modal-legendas");
      container.innerHTML = "";
      legendContainer.innerHTML = "";

      if (!anos || anos.length === 0) {
        container.innerHTML = '<div style="font-size:13px; color:var(--text-muted); text-align:center; padding:60px 0;">Nenhum dado temporal de normas registrado para este autor.</div>';
        return;
      }

      const temasPorAno = perfilTemporal.temasPorAno || {};
      const producaoPorAno = perfilTemporal.producaoPorAno || {};

      const dadosAnos = anos.map(ano => {
        const temas = temasPorAno[ano] || {};
        let total = 0;
        const segmentos = [];
        for (const [tema, qtd] of Object.entries(temas)) {
          total += qtd;
          segmentos.push({ tema, qtd });
        }
        if (total === 0 && producaoPorAno[ano]) {
          total = producaoPorAno[ano];
          segmentos.push({ tema: "Geral", qtd: total });
        }
        segmentos.sort((a, b) => b.qtd - a.qtd);
        return { ano, total, segmentos };
      });

      const maxValor = Math.max(5, maxAnoNormas);
      const w = 900;
      const h = 390;
      const padL = 50;
      const padR = 25;
      const padT = 30;
      const padB = 45;
      const plotW = w - padL - padR;
      const plotH = h - padT - padB;

      const slotW = plotW / anos.length;
      const barW = Math.max(16, Math.min(46, slotW * 0.72));

      let svg = `<svg viewBox="0 0 ${w} ${h}" width="100%" height="100%" style="overflow:visible;">`;

      // Linhas de Grade e Ticks no Eixo Y
      [0.0, 0.25, 0.5, 0.75, 1.0].forEach(frac => {
        const y = padT + plotH * (1 - frac);
        const val = Math.round(maxValor * frac);
        svg += `<line x1="${padL}" y1="${y}" x2="${w - padR}" y2="${y}" stroke="rgba(255,255,255,0.08)" stroke-dasharray="3,3"/>`;
        svg += `<text x="${padL - 8}" y="${y + 3.5}" text-anchor="end" font-size="10" fill="#A1A1AA">${val}</text>`;
      });

      // Linha base X
      svg += `<line x1="${padL}" y1="${padT + plotH}" x2="${w - padR}" y2="${padT + plotH}" stroke="rgba(255,255,255,0.2)"/>`;

      // Barras Empilhadas
      dadosAnos.forEach((d, i) => {
        const x = padL + i * slotW + (slotW - barW) / 2;
        let currY = padT + plotH;

        // Grupo da barra de cada ano com atributos de dados
        svg += `<g class="hist-ano-group" data-ano="${d.ano}" data-total="${d.total}">`;

        d.segmentos.forEach((seg, sIdx) => {
          const segH = maxValor > 0 ? (seg.qtd / maxValor) * plotH : 0;
          currY -= segH;
          const cor = obterCorTema(seg.tema);
          const pct = d.total > 0 ? ((seg.qtd / d.total) * 100).toFixed(1) : "0.0";

          svg += `
            <rect class="hist-bar-segment"
                  x="${x}" y="${currY}" width="${barW}" height="${Math.max(1, segH)}"
                  fill="${cor}" rx="2"
                  data-ano="${d.ano}"
                  data-tema="${seg.tema}"
                  data-qtd="${seg.qtd}"
                  data-pct="${pct}"
                  style="cursor:pointer; transition: opacity 0.2s, stroke-width 0.2s;">
            </rect>
          `;
        });

        // Rótulo da quantidade total no topo da barra
        if (d.total > 0) {
          svg += `<text x="${x + barW/2}" y="${currY - 5}" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">${d.total}</text>`;
        }

        // Rótulo do Ano no eixo horizontal
        svg += `<text x="${x + barW/2}" y="${h - 12}" text-anchor="middle" font-size="11" font-weight="500" fill="#A1A1AA">${d.ano}</text>`;

        svg += `</g>`;
      });

      svg += `</svg>`;
      container.innerHTML = svg;

      // Interatividade: Hover na barra abre popup detalhado com todas as categorias do ano
      container.querySelectorAll(".hist-bar-segment").forEach(rect => {
        const ano = parseInt(rect.getAttribute("data-ano"), 10);
        const temaAlvo = rect.getAttribute("data-tema");
        const anoInfo = dadosAnos.find(d => d.ano === ano);

        rect.onmouseenter = e => {
          rect.style.stroke = "#FFFFFF";
          rect.style.strokeWidth = "2px";

          let htmlItens = "";
          if (anoInfo && anoInfo.segmentos) {
            anoInfo.segmentos.forEach(seg => {
              const cor = obterCorTema(seg.tema);
              const pct = anoInfo.total > 0 ? ((seg.qtd / anoInfo.total) * 100).toFixed(1) : "0.0";
              const isHighlight = (seg.tema === temaAlvo);
              htmlItens += `
                <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; padding:3px 6px; border-radius:4px; ${isHighlight ? 'background:rgba(255,255,255,0.12);' : ''}">
                  <div style="display:flex; align-items:center; gap:7px; overflow:hidden;">
                    <span style="width:8px; height:8px; border-radius:2px; background:${cor}; flex-shrink:0;"></span>
                    <span style="font-size:11px; color:#FFFFFF; white-space:nowrap; text-overflow:ellipsis; overflow:hidden; font-weight:${isHighlight ? '700' : '500'};">${seg.tema}</span>
                  </div>
                  <div style="font-size:11px; color:#D4D4D8; flex-shrink:0;">
                    <b>${seg.qtd}</b> <span style="font-size:10px; color:#A1A1AA;">(${pct}%)</span>
                  </div>
                </div>
              `;
            });
          }

          tooltip.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; border-bottom:1px solid rgba(255,255,255,0.12); padding-bottom:6px;">
              <span style="font-weight:700; font-size:13px; color:#FFFFFF;">📅 Ano ${ano}</span>
              <span class="chip" style="padding:2px 8px; font-size:10px;">${anoInfo ? anoInfo.total : 0} normas</span>
            </div>
            <div style="display:flex; flex-direction:column; gap:2px;">
              ${htmlItens}
            </div>
          `;

          const cRect = container.getBoundingClientRect();
          let left = e.clientX - cRect.left + 15;
          let top = e.clientY - cRect.top - 20;
          if (left + 310 > cRect.width) left = left - 330;
          if (top + 160 > cRect.height) top = top - 140;

          tooltip.style.left = `${left}px`;
          tooltip.style.top = `${top}px`;
          tooltip.style.display = "block";
        };

        rect.onmousemove = e => {
          const cRect = container.getBoundingClientRect();
          let left = e.clientX - cRect.left + 15;
          let top = e.clientY - cRect.top - 20;
          if (left + 310 > cRect.width) left = left - 330;
          if (top + 160 > cRect.height) top = top - 140;
          tooltip.style.left = `${left}px`;
          tooltip.style.top = `${top}px`;
        };

        rect.onmouseleave = () => {
          rect.style.stroke = "none";
          tooltip.style.display = "none";
        };
      });

      // Legenda Interativa das Categorias no Modal
      if (todosTemasPresentes.size > 0) {
        todosTemasPresentes.forEach(tema => {
          const cor = obterCorTema(tema);
          const item = document.createElement("div");
          item.className = "chip";
          item.style.cursor = "pointer";
          item.style.transition = "all 0.2s";
          item.innerHTML = `
            <span style="width:8px; height:8px; border-radius:2px; background:${cor};"></span>
            <span>${tema}</span>
          `;

          let ativo = false;
          item.onclick = () => {
            ativo = !ativo;
            item.style.borderColor = ativo ? "#FFFFFF" : "var(--border-subtle)";
            item.style.background = ativo ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.04)";

            container.querySelectorAll(".hist-bar-segment").forEach(rect => {
              if (!ativo) {
                rect.style.opacity = "1.0";
              } else {
                const coincide = rect.getAttribute("data-tema") === tema;
                rect.style.opacity = coincide ? "1.0" : "0.15";
              }
            });
          };

          legendContainer.appendChild(item);
        });
      }
    }

    /* =========================================================================
     * MODAL DO SCATTER PLOT COM EIXOS CUSTOMIZÁVEIS E MONOCROMÁTICO
     * ========================================================================= */
    let cacheAutoresScatter = [];

    const METRICAS_SCATTER = {
      hhi: {
        id: "hhi",
        nomeCurto: "Especialização (HHI)",
        nomeCompleto: "Grau de Especialização Temática (HHI)",
        descricao: "0.0 = Generalista → 1.0 = Hiper-especialista",
        min: 0.0,
        max: 1.0,
        sufixo: "",
        casas: 3,
        extrair: a => (a.hhi != null) ? a.hhi : 0.2
      },
      entropia: {
        id: "entropia",
        nomeCurto: "Entropia (Hnorm)",
        nomeCompleto: "Entropia Temática de Shannon (Dispersão)",
        descricao: "0.0 = Monotemático → 1.0 = Máxima dispersão",
        min: 0.0,
        max: 1.0,
        sufixo: "",
        casas: 3,
        extrair: a => (a.entropia != null) ? a.entropia : Math.max(0, Math.min(1, 1.0 - (a.hhi || 0.2)))
      },
      pctDominante: {
        id: "pctDominante",
        nomeCurto: "Concentração Dominante (%)",
        nomeCompleto: "Concentração no Tema Dominante",
        descricao: "0% a 100% da produção no principal tema",
        min: 0.0,
        max: 100.0,
        sufixo: "%",
        casas: 1,
        extrair: a => (a.pctDominante != null) ? a.pctDominante : Math.min(100, (a.hhi || 0.2) * 100)
      },
      cosseno: {
        id: "cosseno",
        nomeCurto: "Similaridade Cosseno",
        nomeCompleto: "Similaridade Cosseno com Acervo Global",
        descricao: "0.0 = Pauta atípica → 1.0 = Pleno alinhamento com a agenda municipal",
        min: 0.0,
        max: 1.0,
        sufixo: "",
        casas: 3,
        extrair: a => (a.cosseno != null) ? a.cosseno : 0.5
      },
      totalNormas: {
        id: "totalNormas",
        nomeCurto: "Total de Normas",
        nomeCompleto: "Quantidade Total de Normas Aprovadas/Propostas",
        descricao: "Volume total de produção legislativa catalogada",
        min: 0,
        max: "auto",
        sufixo: "",
        casas: 0,
        extrair: a => a.totalNormas || 0
      },
      normasIndividuais: {
        id: "normasIndividuais",
        nomeCurto: "Normas Individuais",
        nomeCompleto: "Quantidade de Normas Individuais (Solo)",
        descricao: "Volume de normas propostas de forma exclusiva e isolada",
        min: 0,
        max: "auto",
        sufixo: "",
        casas: 0,
        extrair: a => (a.normasIndividuais != null) ? a.normasIndividuais : Math.round((a.totalNormas || 0) * (1 - ((a.taxaCoautoria || 0) / 100)))
      },
      normasColetivas: {
        id: "normasColetivas",
        nomeCurto: "Normas Coletivas",
        nomeCompleto: "Quantidade de Normas Coletivas (Parceria)",
        descricao: "Volume de normas subscritas em regime de coautoria",
        min: 0,
        max: "auto",
        sufixo: "",
        casas: 0,
        extrair: a => (a.normasColetivas != null) ? a.normasColetivas : Math.round((a.totalNormas || 0) * ((a.taxaCoautoria || 0) / 100))
      },
      mediaAnual: {
        id: "mediaAnual",
        nomeCurto: "Média Anual",
        nomeCompleto: "Média Anual de Produção Legislativa",
        descricao: "Volume médio de normas produzidas por ano ativo",
        min: 0,
        max: "auto",
        sufixo: " normas/ano",
        casas: 1,
        extrair: a => (a.mediaAnual != null) ? a.mediaAnual : Math.max(1, Math.round(((a.totalNormas || 0) / 4) * 10) / 10)
      },
      taxaCoautoria: {
        id: "taxaCoautoria",
        nomeCurto: "Taxa de Coautoria (%)",
        nomeCompleto: "Taxa de Coautoria Relativa",
        descricao: "0% = 100% individual → 100% = atuação puramente colegiada",
        min: 0.0,
        max: 100.0,
        sufixo: "%",
        casas: 1,
        extrair: a => a.taxaCoautoria || 0
      },
      totalParceiros: {
        id: "totalParceiros",
        nomeCurto: "Parceiros Políticos",
        nomeCompleto: "Total de Parceiros Políticos Distintos",
        descricao: "Quantidade de agentes políticos com quem já coassinou normas",
        min: 0,
        max: "auto",
        sufixo: "",
        casas: 0,
        extrair: a => (a.totalParceiros != null) ? a.totalParceiros : Math.max(1, Math.round((a.taxaCoautoria || 0) * 0.3 + Math.sqrt(a.totalNormas || 0) * 1.5))
      },
      mediaAutoresPorNorma: {
        id: "mediaAutoresPorNorma",
        nomeCurto: "Autores por Norma",
        nomeCompleto: "Média de Signatários por Norma",
        descricao: "Tamanho médio do grupo de autores por ato normativo",
        min: 1.0,
        max: "auto",
        sufixo: " autores",
        casas: 2,
        extrair: a => (a.mediaAutoresPorNorma != null) ? a.mediaAutoresPorNorma : (1.0 + (a.taxaCoautoria || 0) * 0.04)
      },
      intensidadeMedia: {
        id: "intensidadeMedia",
        nomeCurto: "Intensidade por Parceria",
        nomeCompleto: "Intensidade Média de Parceria",
        descricao: "Média de normas conjuntas compartilhadas por parceiro político",
        min: 0,
        max: "auto",
        sufixo: " normas/parc.",
        casas: 1,
        extrair: a => (a.intensidadeMedia != null) ? a.intensidadeMedia : Math.max(1.0, ((a.totalNormas || 0) * 0.2) / Math.max(1, (a.totalParceiros || 5)))
      }
    };

    async function abrirModalScatter() {
      const modal = document.getElementById("modal-scatter");
      modal.classList.add("open");

      if (cacheAutoresScatter.length === 0) {
        try {
          const res = await fetch(`${API_BASE}/api/autores?limit=100`);
          if (res.ok) {
            cacheAutoresScatter = await res.json();
          }
        } catch (e) {
          console.warn("Aviso ao buscar autores da API, recorrendo aos nós carregados no grafo:", e);
        }
      }

      // Fallback resiliente: se API não retornar dados, povoar a partir dos nós existentes no grafo
      if (!cacheAutoresScatter || cacheAutoresScatter.length === 0) {
        if (dadosGrafoAtual && dadosGrafoAtual.nodes && dadosGrafoAtual.nodes.length > 0) {
          cacheAutoresScatter = dadosGrafoAtual.nodes.map(n => ({
            id: n.id,
            name: n.name,
            cargo: n.cargo || "Autor",
            totalNormas: n.totalNormas || 0,
            temaDominante: n.temaDominante || "Geral",
            hhi: 0.25,
            entropia: 0.75,
            pctDominante: 35.0,
            taxaCoautoria: (n.degree || 1) > 2 ? 45.0 : 15.0,
            cosseno: 0.65,
            totalParceiros: n.degree || 1,
            normasIndividuais: Math.round((n.totalNormas || 0) * 0.6),
            normasColetivas: Math.round((n.totalNormas || 0) * 0.4),
            mediaAutoresPorNorma: 1.8,
            intensidadeMedia: 3.5,
            mediaAnual: Math.max(1, Math.round(((n.totalNormas || 0) / 4) * 10) / 10)
          }));
        }
      }

      const selectFiltro = document.getElementById("scatter-filtro-tema");
      if (selectFiltro && selectFiltro.options.length <= 1) {
        const temasSet = new Set(cacheAutoresScatter.map(a => a.temaDominante).filter(Boolean));
        temasSet.forEach(t => {
          const opt = document.createElement("option");
          opt.value = t;
          opt.innerText = t;
          selectFiltro.appendChild(opt);
        });
      }

      desenharScatterPlot();
    }

    function desenharScatterPlot() {
      const container = document.getElementById("scatter-plot-svg");
      const tooltip = document.getElementById("scatter-tooltip");
      const buscaInput = document.getElementById("scatter-busca");
      const filtroTemaInput = document.getElementById("scatter-filtro-tema");
      const eixoYSelect = document.getElementById("scatter-eixo-y");
      const eixoXSelect = document.getElementById("scatter-eixo-x");

      const tipoEixoY = (eixoYSelect && eixoYSelect.value) ? eixoYSelect.value : "totalNormas";
      const tipoEixoX = (eixoXSelect && eixoXSelect.value) ? eixoXSelect.value : "hhi";
      const termoBusca = (buscaInput ? buscaInput.value : "").trim().toLowerCase();
      const temaFiltro = filtroTemaInput ? filtroTemaInput.value : "TODOS";

      if (!container) return;

      const confX = METRICAS_SCATTER[tipoEixoX] || METRICAS_SCATTER.hhi;
      const confY = METRICAS_SCATTER[tipoEixoY] || METRICAS_SCATTER.totalNormas;

      const autoresFiltrados = cacheAutoresScatter.filter(a => {
        const matchNome = !termoBusca || (a.name && a.name.toLowerCase().includes(termoBusca));
        const matchTema = temaFiltro === "TODOS" || a.temaDominante === temaFiltro;
        return matchNome && matchTema;
      });

      const badge = document.getElementById("scatter-badge-total");
      if (badge) badge.innerText = `${autoresFiltrados.length} autores`;

      const w = 820;
      const h = 380;
      const padL = 65;
      const padR = 25;
      const padT = 25;
      const padB = 45;
      const plotW = w - padL - padR;
      const plotH = h - padT - padB;

      // Cálculo dinâmico dos limites de X
      let minX = confX.min;
      let maxX = confX.max;
      if (maxX === "auto") {
        const valsX = autoresFiltrados.map(confX.extrair);
        const maxValX = valsX.length ? Math.max(...valsX) : 10;
        maxX = maxValX > 10 ? Math.ceil(maxValX * 1.1) : (maxValX > 0 ? Math.ceil(maxValX) : 10);
      }

      // Cálculo dinâmico dos limites de Y
      let minY = confY.min;
      let maxY = confY.max;
      if (maxY === "auto") {
        const valsY = autoresFiltrados.map(confY.extrair);
        const maxValY = valsY.length ? Math.max(...valsY) : 10;
        maxY = maxValY > 20 ? (Math.ceil(maxValY / 10) * 10) : (maxValY > 0 ? Math.ceil(maxValY * 1.1) : 10);
      }

      function calcularX(val) {
        const ratio = (maxX > minX) ? Math.max(0, Math.min(1, (val - minX) / (maxX - minX))) : 0.5;
        return padL + ratio * plotW;
      }

      function calcularY(val) {
        const ratio = (maxY > minY) ? Math.max(0, Math.min(1, (val - minY) / (maxY - minY))) : 0.5;
        return padT + (1.0 - ratio) * plotH;
      }

      let svg = `<svg viewBox="0 0 ${w} ${h}" width="100%" height="100%" style="overflow:visible;">`;

      // Linhas dos quadrantes (centrais)
      const xMid = padL + 0.5 * plotW;
      const yMid = padT + 0.5 * plotH;

      svg += `<line x1="${xMid}" y1="${padT}" x2="${xMid}" y2="${padT + plotH}" stroke="rgba(255,255,255,0.1)" stroke-dasharray="4,4"/>`;
      svg += `<line x1="${padL}" y1="${yMid}" x2="${w - padR}" y2="${yMid}" stroke="rgba(255,255,255,0.1)" stroke-dasharray="4,4"/>`;

      // Rótulos contextuais dos quadrantes
      const qTopLeft = `Alto(a) ${confY.nomeCurto} • Baixo(a) ${confX.nomeCurto}`;
      const qTopRight = `Alto(a) ${confY.nomeCurto} • Alto(a) ${confX.nomeCurto}`;
      const qBottomLeft = `Baixo(a) ${confY.nomeCurto} • Baixo(a) ${confX.nomeCurto}`;
      const qBottomRight = `Baixo(a) ${confY.nomeCurto} • Alto(a) ${confX.nomeCurto}`;

      svg += `<text x="${padL + 10}" y="${padT + 18}" font-size="10" fill="rgba(255,255,255,0.22)" font-weight="600">${qTopLeft}</text>`;
      svg += `<text x="${w - padR - 10}" y="${padT + 18}" text-anchor="end" font-size="10" fill="rgba(255,255,255,0.22)" font-weight="600">${qTopRight}</text>`;
      svg += `<text x="${padL + 10}" y="${padT + plotH - 10}" font-size="10" fill="rgba(255,255,255,0.22)" font-weight="600">${qBottomLeft}</text>`;
      svg += `<text x="${w - padR - 10}" y="${padT + plotH - 10}" text-anchor="end" font-size="10" fill="rgba(255,255,255,0.22)" font-weight="600">${qBottomRight}</text>`;

      // Ticks do Eixo Y (5 divisões)
      [0.0, 0.25, 0.5, 0.75, 1.0].forEach(frac => {
        const val = minY + frac * (maxY - minY);
        const y = padT + plotH * (1.0 - frac);
        const labelStr = confY.casas > 0 ? val.toFixed(confY.casas) + confY.sufixo : Math.round(val) + confY.sufixo;
        svg += `<line x1="${padL}" y1="${y}" x2="${w - padR}" y2="${y}" stroke="rgba(255,255,255,0.06)"/>`;
        svg += `<text x="${padL - 8}" y="${y + 3.5}" text-anchor="end" font-size="9" fill="#A1A1AA">${labelStr}</text>`;
      });

      // Ticks do Eixo X (5 divisões)
      [0.0, 0.25, 0.50, 0.75, 1.0].forEach(frac => {
        const val = minX + frac * (maxX - minX);
        const x = padL + frac * plotW;
        const labelStr = confX.casas > 0 ? val.toFixed(confX.casas) + confX.sufixo : Math.round(val) + confX.sufixo;
        svg += `<line x1="${x}" y1="${padT}" x2="${x}" y2="${padT + plotH}" stroke="rgba(255,255,255,0.06)"/>`;
        svg += `<text x="${x}" y="${padT + plotH + 16}" text-anchor="middle" font-size="9" fill="#A1A1AA">${labelStr}</text>`;
      });

      // Títulos dos Eixos
      const rotuloEixoX = `${confX.nomeCompleto} (${confX.descricao}) →`;
      const rotuloEixoY = `${confY.nomeCompleto} ${confY.sufixo ? '(' + confY.sufixo.trim() + ')' : ''} →`;

      svg += `<text x="${padL + plotW/2}" y="${h - 8}" text-anchor="middle" font-size="10" font-weight="600" fill="#D4D4D8">${rotuloEixoX}</text>`;
      svg += `<text transform="rotate(-90)" x="${-(padT + plotH/2)}" y="16" text-anchor="middle" font-size="10" font-weight="600" fill="#D4D4D8">${rotuloEixoY}</text>`;

      // Renderização dos Pontos dos Autores
      autoresFiltrados.forEach((a, idx) => {
        const xVal = confX.extrair(a);
        const yVal = confY.extrair(a);

        const cx = calcularX(xVal);
        const cy = calcularY(yVal);
        const r = Math.max(5.5, Math.min(18, 4.5 + Math.sqrt(Math.max(1, a.totalNormas || 0)) * 0.45));
        const cor = obterCorTema(a.temaDominante);

        svg += `
          <circle cx="${cx}" cy="${cy}" r="${r}" fill="${cor}" fill-opacity="0.88" stroke="#FFFFFF" stroke-width="1.2"
                  style="cursor:pointer; transition: transform 0.2s, fill-opacity 0.2s;"
                  data-idx="${idx}">
          </circle>
        `;
      });

      svg += `</svg>`;
      container.innerHTML = svg;

      // Eventos dos círculos no Scatter Plot
      container.querySelectorAll("circle").forEach(circ => {
        const idx = parseInt(circ.getAttribute("data-idx"), 10);
        const autor = autoresFiltrados[idx];
        if (!autor) return;

        circ.onmouseenter = e => {
          circ.setAttribute("fill-opacity", "1");
          circ.setAttribute("stroke-width", "2.5");

          const valX = confX.extrair(autor);
          const valY = confY.extrair(autor);
          const valXFmt = (confX.casas > 0 ? Number(valX).toFixed(confX.casas) : Math.round(valX)) + confX.sufixo;
          const valYFmt = (confY.casas > 0 ? Number(valY).toFixed(confY.casas) : Math.round(valY)) + confY.sufixo;

          const cor = obterCorTema(autor.temaDominante);
          tooltip.innerHTML = `
            <div style="font-weight:700; color:#FFFFFF; font-size:12.5px; margin-bottom:2px;">${autor.name}</div>
            <div style="font-size:11px; color:#A1A1AA; margin-bottom:6px;">${autor.cargo || 'Autor'}</div>
            <div style="display:flex; align-items:center; gap:6px; margin-bottom:8px;">
              <span style="width:9px; height:9px; border-radius:2px; background:${cor}; display:inline-block;"></span>
              <span style="font-size:11px; color:#FFFFFF; font-weight:500;">${autor.temaDominante}</span>
            </div>
            
            <div style="background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.15); border-radius:6px; padding:6px 8px; margin-bottom:8px;">
              <div style="font-size:11px; color:#D4D4D8; display:flex; justify-content:space-between; margin-bottom:2px;">
                <span>Eixo X (${confX.nomeCurto}):</span>
                <b style="color:#FFFFFF;">${valXFmt}</b>
              </div>
              <div style="font-size:11px; color:#D4D4D8; display:flex; justify-content:space-between;">
                <span>Eixo Y (${confY.nomeCurto}):</span>
                <b style="color:#FFFFFF;">${valYFmt}</b>
              </div>
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:6px; font-size:10.5px; color:#A1A1AA; border-top:1px solid rgba(255,255,255,0.12); padding-top:6px;">
              <div>Normas: <b style="color:#FFFFFF;">${autor.totalNormas || 0}</b></div>
              <div>Coautoria: <b style="color:#FFFFFF;">${Number(autor.taxaCoautoria || 0).toFixed(1)}%</b></div>
              <div>Espec. (HHI): <b style="color:#FFFFFF;">${Number(autor.hhi || 0).toFixed(3)}</b></div>
              <div>Cosseno: <b style="color:#FFFFFF;">${Number(autor.cosseno || 0).toFixed(3)}</b></div>
              <div>Parceiros: <b style="color:#FFFFFF;">${autor.totalParceiros || 0}</b></div>
              <div>Entropia: <b style="color:#FFFFFF;">${Number(autor.entropia || (1 - (autor.hhi || 0.2))).toFixed(3)}</b></div>
            </div>
            <div style="font-size:10px; color:#71717A; margin-top:6px; text-align:right;">Clique para focar na rede</div>
          `;

          const rect = container.getBoundingClientRect();
          let left = e.clientX - rect.left + 15;
          let top = e.clientY - rect.top - 20;
          if (left + 290 > rect.width) left = left - 310;
          if (top + 160 > rect.height) top = top - 140;

          tooltip.style.left = `${left}px`;
          tooltip.style.top = `${top}px`;
          tooltip.style.display = "block";
        };

        circ.onmousemove = e => {
          const rect = container.getBoundingClientRect();
          let left = e.clientX - rect.left + 15;
          let top = e.clientY - rect.top - 20;
          if (left + 290 > rect.width) left = left - 310;
          if (top + 160 > rect.height) top = top - 140;
          tooltip.style.left = `${left}px`;
          tooltip.style.top = `${top}px`;
        };

        circ.onmouseleave = () => {
          circ.setAttribute("fill-opacity", "0.88");
          circ.setAttribute("stroke-width", "1.2");
          tooltip.style.display = "none";
        };

        circ.onclick = () => {
          document.getElementById("modal-scatter").classList.remove("open");
          tooltip.style.display = "none";
          focarAutor(autor.id);
        };
      });
    }
  </script>
</body>
</html>
""";
}
