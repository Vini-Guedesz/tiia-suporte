const historyKey = 'suporte-ai-history';
const themeKey = 'suporte-ai-theme';
const monitorStorageKey = 'suporte-ai-monitors';
const activeStreams = new Map();
const monitorStore = new Map();
const MAX_HISTORY_ITEMS = 8;
const MAX_MONITOR_LOG_ENTRIES = 500;
const MAX_MONITOR_VISIBLE_LINES = 180;
let activeMonitorId = null;
let activeMonitorFilter = 'all';
let monitorPersistTimer = null;

function readHistory() {
    try {
        const parsed = JSON.parse(localStorage.getItem(historyKey) || '[]');
        return Array.isArray(parsed) ? parsed.slice(0, MAX_HISTORY_ITEMS) : [];
    } catch (error) {
        return [];
    }
}

function readPersistedMonitorState() {
    try {
        const parsed = JSON.parse(localStorage.getItem(monitorStorageKey) || '{"monitors":[],"activeMonitorId":null}');

        if (Array.isArray(parsed)) {
            return { monitors: parsed, activeMonitorId: null };
        }

        if (!parsed || !Array.isArray(parsed.monitors)) {
            return { monitors: [], activeMonitorId: null };
        }

        return {
            monitors: parsed.monitors,
            activeMonitorId: typeof parsed.activeMonitorId === 'string' ? parsed.activeMonitorId : null
        };
    } catch (error) {
        return { monitors: [], activeMonitorId: null };
    }
}

function createHistoryItem(tool, value) {
    const item = document.createElement('div');
    item.className = 'history-item';

    const title = document.createElement('strong');
    title.textContent = tool;

    const details = document.createElement('span');
    details.textContent = value;

    item.append(title, details);
    return item;
}

function renderHistory() {
    const history = readHistory();
    const container = document.getElementById('history-list');
    const meta = document.getElementById('history-meta');

    container.replaceChildren();
    meta.textContent = `${history.length} ${history.length === 1 ? 'item' : 'itens'}`;

    if (!history.length) {
        container.append(createHistoryItem('Sem historico', 'As proximas execucoes aparecem aqui.'));
        return;
    }

    history.forEach((item) => {
        container.append(createHistoryItem(item.tool, item.value));
    });
}

function pushHistory(tool, value) {
    const history = readHistory();

    if (history[0] && history[0].tool === tool && history[0].value === value) {
        renderHistory();
        return;
    }

    history.unshift({ tool, value });
    localStorage.setItem(historyKey, JSON.stringify(history.slice(0, MAX_HISTORY_ITEMS)));
    renderHistory();
}

function resolveInitialTheme() {
    const storedTheme = localStorage.getItem(themeKey);
    if (storedTheme === 'dark' || storedTheme === 'light') {
        return storedTheme;
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function updateThemeToggle(theme) {
    const button = document.getElementById('theme-toggle');
    button.textContent = theme === 'dark' ? 'Tema light' : 'Tema dark';
}

function applyTheme(theme) {
    document.body.dataset.theme = theme;
    localStorage.setItem(themeKey, theme);
    updateThemeToggle(theme);
}

function setJsonOutput(id, payload) {
    const element = document.getElementById(id);
    element.classList.remove('json-placeholder');
    element.textContent = JSON.stringify(payload, null, 2);
}

function setErrorOutput(id, payload) {
    const element = document.getElementById(id);
    element.classList.remove('json-placeholder');
    element.textContent = JSON.stringify(payload, null, 2);
}

function setTerminalPlaceholder(id, message) {
    const element = document.getElementById(id);
    element.innerHTML = `<div class="stream-placeholder">${message}</div>`;
}

function appendTerminalLine(id, type, message) {
    const element = document.getElementById(id);

    if (element.querySelector('.stream-placeholder')) {
        element.replaceChildren();
    }

    const line = document.createElement('div');
    line.className = `stream-line type-${type}`;
    line.textContent = message;
    element.append(line);
    element.scrollTop = element.scrollHeight;
}

async function requestJson(url, outputId, historyLabel, historyValue, options = {}) {
    const output = document.getElementById(outputId);
    output.classList.add('json-placeholder');
    output.textContent = 'Processando...';

    try {
        const response = await fetch(url, options);
        const payload = await response.json();

        if (!response.ok) {
            setErrorOutput(outputId, payload);
            return;
        }

        pushHistory(historyLabel, historyValue);
        setJsonOutput(outputId, payload);
    } catch (error) {
        setErrorOutput(outputId, { message: 'Falha ao chamar a API.', details: error.message });
    }
}

function startStream(key, url, outputId, historyLabel, historyValue) {
    if (activeStreams.has(key)) {
        activeStreams.get(key).close();
    }

    setTerminalPlaceholder(outputId, 'Conectando ao stream...');
    const source = new EventSource(url);
    let historyRegistered = false;
    activeStreams.set(key, source);

    ['started', 'output', 'completed', 'error', 'timeout'].forEach((eventName) => {
        source.addEventListener(eventName, (event) => {
            if (!event.data) {
                return;
            }

            const payload = JSON.parse(event.data);
            appendTerminalLine(outputId, payload.type, payload.message);

            if (payload.type === 'started' && !historyRegistered) {
                historyRegistered = true;
                pushHistory(historyLabel, historyValue);
            }

            if (payload.finished) {
                source.close();
                activeStreams.delete(key);
            }
        });
    });

    source.onerror = () => {
        appendTerminalLine(outputId, 'error', 'Conexao SSE encerrada.');
        source.close();
        activeStreams.delete(key);
    };
}

function formatLatency(latency) {
    if (typeof latency !== 'number' || Number.isNaN(latency)) {
        return '--';
    }

    return `${latency % 1 === 0 ? latency.toFixed(0) : latency.toFixed(2)} ms`;
}

function formatPercent(value) {
    if (typeof value !== 'number' || Number.isNaN(value)) {
        return '0%';
    }

    return `${value % 1 === 0 ? value.toFixed(0) : value.toFixed(2)}%`;
}

function formatTimestamp(timestamp) {
    if (!timestamp) {
        return '';
    }

    return new Date(timestamp).toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function formatStatus(status) {
    const labels = {
        WAITING: 'Aguardando',
        CONNECTING: 'Conectando',
        ONLINE: 'Online',
        OFFLINE: 'Sem conexao',
        STOPPED: 'Parado'
    };

    return labels[status] || status;
}

function summarizeMonitorState(monitor) {
    const parts = [
        `${Math.round(monitor.intervalMs / 1000)}s`,
        `${monitor.timeoutMs} ms`,
        `${monitor.attempts} tentativas`,
        `${formatPercent(monitor.packetLossPercent)} perda`
    ];

    return parts.join(' | ');
}

function buildMonitorSummary(monitor) {
    return [
        `Ping medio ${formatLatency(monitor.averageLatencyMs)}`,
        `Quedas ${monitor.outages}`,
        `Perda ${formatPercent(monitor.packetLossPercent)}`
    ].join(' | ');
}

function buildMonitorNarrativeReport(monitor) {
    const status = formatStatus(monitor.status).toLowerCase();
    const averageLatency = monitor.averageLatencyMs == null
        ? 'ainda nao possui media de latencia consolidada'
        : `esta com media de ${formatLatency(monitor.averageLatencyMs)}`;
    const currentLatency = monitor.currentLatencyMs == null
        ? 'No momento nao ha um ping atual valido registrado'
        : `O ping atual esta em ${formatLatency(monitor.currentLatencyMs)}`;
    const intervalSeconds = Math.round(monitor.intervalMs / 1000);
    const timestamp = new Date().toLocaleString('pt-BR');

    return `${monitor.name} (${monitor.target}) esta atualmente ${status} e ${averageLatency}. ${currentLatency}. ` +
        `Ate o momento houve o total de ${monitor.outages} quedas, foram feitos ${monitor.attempts} pings, ` +
        `com ${monitor.successfulAttempts} respostas bem-sucedidas e ${monitor.failedAttempts} falhas. ` +
        `A perda de pacote acumulada esta em ${formatPercent(monitor.packetLossPercent)} e existem ` +
        `${monitor.consecutiveFailures} falhas consecutivas neste instante. ` +
        `O monitor esta configurado com intervalo de ${intervalSeconds} segundos e timeout de ${monitor.timeoutMs} ms. ` +
        `Relatorio gerado em ${timestamp}.`;
}

function sanitizeFileName(value) {
    return value
        .toLowerCase()
        .replace(/[^a-z0-9-_]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 60) || 'monitor';
}

function toFiniteNumber(value, fallback = 0) {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : fallback;
}

function createMonitorId() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }

    return `monitor-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

function createActionButton(label, variant, action, monitorId) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `monitor-action ${variant}`;
    button.dataset.monitorAction = action;
    button.dataset.monitorId = monitorId;
    button.textContent = label;
    return button;
}

function createStatCard(label, value) {
    const card = document.createElement('div');
    card.className = 'stat-card';

    const title = document.createElement('span');
    title.textContent = label;

    const strong = document.createElement('strong');
    strong.textContent = value;

    card.append(title, strong);
    return { card, valueNode: strong };
}

function createMonitorSidebarEmpty(message = 'Nao ha monitores registrados.') {
    const empty = document.createElement('div');
    empty.className = 'monitor-sidebar-empty';
    empty.textContent = message;
    return empty;
}

function createMonitorRecord(snapshot) {
    return {
        id: snapshot.id || createMonitorId(),
        name: snapshot.name,
        target: snapshot.target,
        createdAt: toFiniteNumber(snapshot.createdAt, Date.now()),
        intervalMs: toFiniteNumber(snapshot.intervalMs, 5000),
        timeoutMs: toFiniteNumber(snapshot.timeoutMs, 2000),
        source: null,
        intentionalClose: false,
        awaitingReconnect: false,
        running: Boolean(snapshot.running),
        status: snapshot.status || (snapshot.running ? 'CONNECTING' : 'STOPPED'),
        connected: Boolean(snapshot.connected),
        currentLatencyMs: snapshot.currentLatencyMs == null ? null : toFiniteNumber(snapshot.currentLatencyMs, null),
        averageLatencyMs: snapshot.averageLatencyMs == null ? null : toFiniteNumber(snapshot.averageLatencyMs, null),
        attempts: toFiniteNumber(snapshot.attempts, 0),
        successfulAttempts: toFiniteNumber(snapshot.successfulAttempts, 0),
        failedAttempts: toFiniteNumber(snapshot.failedAttempts, 0),
        outages: toFiniteNumber(snapshot.outages, 0),
        packetLossPercent: toFiniteNumber(snapshot.packetLossPercent, 0),
        consecutiveFailures: toFiniteNumber(snapshot.consecutiveFailures, 0),
        logs: [],
        logsTrimmed: false
    };
}

function serializeMonitor(monitor) {
    return {
        id: monitor.id,
        name: monitor.name,
        target: monitor.target,
        createdAt: monitor.createdAt,
        intervalMs: monitor.intervalMs,
        timeoutMs: monitor.timeoutMs,
        running: monitor.running,
        status: monitor.status,
        connected: monitor.connected,
        currentLatencyMs: monitor.currentLatencyMs,
        averageLatencyMs: monitor.averageLatencyMs,
        attempts: monitor.attempts,
        successfulAttempts: monitor.successfulAttempts,
        failedAttempts: monitor.failedAttempts,
        outages: monitor.outages,
        packetLossPercent: monitor.packetLossPercent,
        consecutiveFailures: monitor.consecutiveFailures
    };
}

function getMonitorEntries() {
    return Array.from(monitorStore.values()).sort((left, right) => right.createdAt - left.createdAt);
}

function persistMonitors() {
    try {
        const monitors = getMonitorEntries().map(serializeMonitor);
        localStorage.setItem(monitorStorageKey, JSON.stringify({ activeMonitorId, monitors }));
    } catch (error) {
        // Ignore persistence errors and keep the monitoring flow running.
    }
}

function schedulePersistMonitors() {
    if (monitorPersistTimer) {
        window.clearTimeout(monitorPersistTimer);
    }

    monitorPersistTimer = window.setTimeout(() => {
        monitorPersistTimer = null;
        persistMonitors();
    }, 150);
}

function updateClearMonitorsButton() {
    const button = document.getElementById('clear-monitors');
    button.disabled = !monitorStore.size;
}

function isMonitorOnline(monitor) {
    return monitor.status === 'ONLINE' && monitor.connected;
}

function hasMonitorIssue(monitor) {
    return monitor.status === 'OFFLINE' || monitor.status === 'CONNECTING';
}

function isMonitorStopped(monitor) {
    return monitor.status === 'STOPPED' || (!monitor.running && monitor.status !== 'ONLINE');
}

function matchesMonitorFilter(monitor) {
    if (activeMonitorFilter === 'online') {
        return isMonitorOnline(monitor);
    }

    if (activeMonitorFilter === 'issues') {
        return hasMonitorIssue(monitor);
    }

    if (activeMonitorFilter === 'stopped') {
        return isMonitorStopped(monitor);
    }

    return true;
}

function getVisibleMonitors() {
    return getMonitorEntries().filter(matchesMonitorFilter);
}

function ensureActiveMonitorSelection() {
    const visibleMonitors = getVisibleMonitors();

    if (activeMonitorId && visibleMonitors.some((monitor) => monitor.id === activeMonitorId)) {
        return;
    }

    activeMonitorId = visibleMonitors[0]?.id || null;
}

function updateMonitorOverview() {
    const monitors = getMonitorEntries();
    const onlineCount = monitors.filter(isMonitorOnline).length;
    const issueCount = monitors.filter((monitor) => hasMonitorIssue(monitor) || isMonitorStopped(monitor)).length;
    const outageCount = monitors.reduce((total, monitor) => total + monitor.outages, 0);
    const latencySamples = monitors
        .map((monitor) => monitor.averageLatencyMs)
        .filter((value) => typeof value === 'number' && !Number.isNaN(value));
    const averageLatency = latencySamples.length
        ? latencySamples.reduce((sum, value) => sum + value, 0) / latencySamples.length
        : null;

    document.getElementById('overview-active-count').textContent = String(onlineCount);
    document.getElementById('overview-issue-count').textContent = String(issueCount);
    document.getElementById('overview-average-latency').textContent = formatLatency(averageLatency);
    document.getElementById('overview-outage-count').textContent = String(outageCount);
}

function updateMonitorFilterButtons() {
    document.querySelectorAll('[data-monitor-filter]').forEach((button) => {
        button.classList.toggle('active', button.dataset.monitorFilter === activeMonitorFilter);
    });
}

function updateMonitorMeta() {
    const running = getMonitorEntries().filter((monitor) => monitor.running).length;
    const total = monitorStore.size;
    const visible = getVisibleMonitors().length;
    const meta = document.getElementById('monitor-meta');

    if (!total) {
        meta.textContent = '0 ativos';
        return;
    }

    meta.textContent = `${running} ativos / ${visible} visiveis / ${total} registrados`;
}

function buildLogMetaText(monitor) {
    const retainedCount = monitor.logs.length;
    const visibleCount = Math.min(retainedCount, MAX_MONITOR_VISIBLE_LINES);
    const parts = [`${retainedCount} linhas retidas`];

    if (retainedCount > visibleCount) {
        parts.push(`${visibleCount} visiveis`);
    }

    if (monitor.logsTrimmed) {
        parts.push(`ultimas ${MAX_MONITOR_LOG_ENTRIES} exportaveis`);
    }

    return parts.join(' | ');
}

function resolveMonitorPlaceholder(monitor) {
    if (monitor.running || monitor.status === 'CONNECTING' || monitor.status === 'WAITING') {
        return 'Conectando ao acompanhamento...';
    }

    if (monitor.attempts > 0) {
        return 'Monitor pausado. Clique em Retomar para continuar.';
    }

    return 'Monitor salvo. Clique em Retomar para iniciar.';
}

function createMonitorDetailEmpty(title, description) {
    const empty = document.createElement('div');
    empty.className = 'monitor-detail-empty';

    const strong = document.createElement('strong');
    strong.textContent = title;

    const text = document.createElement('span');
    text.textContent = description;

    empty.append(strong, text);
    return empty;
}

function renderMonitorLog(container, monitor) {
    const wasPinnedToBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 24;
    const visibleLogs = monitor.logs.slice(-MAX_MONITOR_VISIBLE_LINES);

    container.replaceChildren();

    if (!visibleLogs.length) {
        const placeholder = document.createElement('div');
        placeholder.className = 'stream-placeholder';
        placeholder.textContent = resolveMonitorPlaceholder(monitor);
        container.append(placeholder);
        return;
    }

    const fragment = document.createDocumentFragment();

    visibleLogs.forEach((entry) => {
        const line = document.createElement('div');
        line.className = `stream-line type-${entry.type}`;
        line.textContent = entry.message;
        fragment.append(line);
    });

    container.append(fragment);

    if (wasPinnedToBottom || visibleLogs.length <= 1) {
        container.scrollTop = container.scrollHeight;
    }

    updateMonitorLogFade(container);
}

function updateMonitorLogFade(container) {
    container.classList.toggle('has-top-fade', container.scrollTop > 4);
}

function createMonitorSidebarItem(monitor) {
    const item = document.createElement('div');
    item.className = 'monitor-list-item';
    item.dataset.monitorAction = 'focus';
    item.dataset.monitorId = monitor.id;

    if (monitor.id === activeMonitorId) {
        item.classList.add('active');
    }

    const head = document.createElement('div');
    head.className = 'monitor-list-item-head';

    const textBlock = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = monitor.name;

    const target = document.createElement('span');
    target.className = 'monitor-target';
    target.textContent = monitor.target;

    textBlock.append(title, target);

    const status = document.createElement('span');
    status.className = 'monitor-chip';
    status.dataset.state = monitor.status;
    status.textContent = formatStatus(monitor.status);

    head.append(textBlock, status);

    const meta = document.createElement('div');
    meta.className = 'monitor-meta-line';
    meta.textContent = summarizeMonitorState(monitor);

    const summary = document.createElement('div');
    summary.className = 'monitor-summary';
    summary.textContent = buildMonitorSummary(monitor);

    item.append(head, meta, summary);
    return item;
}

function renderMonitorSidebar() {
    const list = document.getElementById('monitor-list');
    const visibleMonitors = getVisibleMonitors();
    list.replaceChildren();

    if (!monitorStore.size) {
        list.append(createMonitorSidebarEmpty());
        return;
    }

    if (!visibleMonitors.length) {
        list.append(createMonitorSidebarEmpty('Nenhum monitor encontrado neste filtro.'));
        return;
    }

    visibleMonitors.forEach((monitor) => {
        list.append(createMonitorSidebarItem(monitor));
    });
}

function createMonitorDetail(monitor) {
    const card = document.createElement('article');
    card.className = 'monitor-instance';

    const head = document.createElement('div');
    head.className = 'monitor-instance-head';

    const titleBlock = document.createElement('div');
    titleBlock.className = 'monitor-title-block';

    const title = document.createElement('h3');
    title.textContent = monitor.name;

    const target = document.createElement('p');
    target.textContent = monitor.target;

    const chips = document.createElement('div');
    chips.className = 'monitor-meta-grid';

    const intervalChip = document.createElement('span');
    intervalChip.className = 'monitor-chip';
    intervalChip.textContent = `Intervalo ${Math.round(monitor.intervalMs / 1000)}s`;

    const timeoutChip = document.createElement('span');
    timeoutChip.className = 'monitor-chip';
    timeoutChip.textContent = `Timeout ${monitor.timeoutMs} ms`;

    const statusChip = document.createElement('span');
    statusChip.className = 'monitor-chip';
    statusChip.dataset.state = monitor.status;
    statusChip.textContent = formatStatus(monitor.status);

    chips.append(intervalChip, timeoutChip, statusChip);
    titleBlock.append(title, target, chips);

    const actions = document.createElement('div');
    actions.className = 'monitor-actions';
    actions.append(
        createActionButton('Relatorio', 'secondary', 'report', monitor.id),
        createActionButton('Exportar', 'secondary', 'export', monitor.id),
        createActionButton(monitor.running ? 'Parar' : 'Retomar', monitor.running ? 'primary' : 'secondary', 'toggle', monitor.id),
        createActionButton('Remover', 'secondary', 'remove', monitor.id)
    );

    head.append(titleBlock, actions);

    const quickStats = document.createElement('div');
    quickStats.className = 'monitor-quick-stats';

    [
        ['Atual', formatLatency(monitor.currentLatencyMs)],
        ['Medio', formatLatency(monitor.averageLatencyMs)],
        ['Quedas', String(monitor.outages)],
        ['Perda', formatPercent(monitor.packetLossPercent)]
    ].forEach(([label, value]) => {
        const stat = createStatCard(label, value).card;
        stat.className = 'monitor-quick-stat';
        quickStats.append(stat);
    });

    const body = document.createElement('div');
    body.className = 'monitor-instance-body';

    const statsGrid = document.createElement('div');
    statsGrid.className = 'monitor-stats-grid';

    [
        ['Ping atual', formatLatency(monitor.currentLatencyMs)],
        ['Ping medio', formatLatency(monitor.averageLatencyMs)],
        ['Tentativas', String(monitor.attempts)],
        ['Sucessos', String(monitor.successfulAttempts)],
        ['Falhas', String(monitor.failedAttempts)],
        ['Falhas seguidas', String(monitor.consecutiveFailures)]
    ].forEach(([label, value]) => {
        statsGrid.append(createStatCard(label, value).card);
    });

    const logHeader = document.createElement('div');
    logHeader.className = 'monitor-log-header';

    const logTitle = document.createElement('span');
    logTitle.textContent = 'Log do acompanhamento';

    const logMeta = document.createElement('span');
    logMeta.className = 'monitor-log-note';
    logMeta.textContent = buildLogMetaText(monitor);

    logHeader.append(logTitle, logMeta);

    const log = document.createElement('div');
    log.className = 'monitor-log terminal-output';
    log.addEventListener('scroll', () => updateMonitorLogFade(log));
    renderMonitorLog(log, monitor);

    body.append(statsGrid, logHeader, log);
    card.append(head, quickStats, body);
    return card;
}

function renderMonitorDetail() {
    const detail = document.getElementById('monitor-detail');
    const visibleMonitors = getVisibleMonitors();
    detail.replaceChildren();

    if (!monitorStore.size) {
        detail.append(createMonitorDetailEmpty(
            'Nenhum monitor registrado',
            'Cadastre um alvo para acompanhar estabilidade, ping medio, perda de pacote e quedas em tempo real.'
        ));
        return;
    }

    if (!visibleMonitors.length) {
        detail.append(createMonitorDetailEmpty(
            'Nenhum monitor neste filtro',
            'Mude o filtro da lateral para voltar a ver os demais monitores.'
        ));
        return;
    }

    const activeMonitor = activeMonitorId ? monitorStore.get(activeMonitorId) : null;

    if (!activeMonitor) {
        detail.append(createMonitorDetailEmpty(
            'Selecione um monitor',
            'Escolha um item na lateral para abrir o acompanhamento detalhado.'
        ));
        return;
    }

    detail.append(createMonitorDetail(activeMonitor));
}

function captureMonitorUIState() {
    const list = document.getElementById('monitor-list');
    const detail = document.getElementById('monitor-detail');
    const log = detail?.querySelector('.monitor-log');

    return {
        windowScrollY: window.scrollY,
        listScrollTop: list ? list.scrollTop : 0,
        detailScrollTop: detail ? detail.scrollTop : 0,
        logState: log ? {
            monitorId: activeMonitorId,
            atBottom: log.scrollHeight - log.clientHeight - log.scrollTop < 24,
            distanceFromBottom: Math.max(0, log.scrollHeight - log.clientHeight - log.scrollTop)
        } : null
    };
}

function restoreMonitorUIState(state) {
    if (!state) {
        return;
    }

    const list = document.getElementById('monitor-list');
    const detail = document.getElementById('monitor-detail');

    if (list) {
        list.scrollTop = state.listScrollTop;
    }

    if (detail) {
        detail.scrollTop = state.detailScrollTop;
    }

    if (state.logState && state.logState.monitorId === activeMonitorId) {
        const log = detail?.querySelector('.monitor-log');
        if (log) {
            if (state.logState.atBottom) {
                log.scrollTop = log.scrollHeight;
            } else {
                log.scrollTop = Math.max(0, log.scrollHeight - log.clientHeight - state.logState.distanceFromBottom);
            }
        }
    }

    if (window.scrollY !== state.windowScrollY) {
        window.scrollTo({ top: state.windowScrollY, left: window.scrollX, behavior: 'auto' });
    }
}

function refreshMonitorUI() {
    const previousState = captureMonitorUIState();
    ensureActiveMonitorSelection();
    updateClearMonitorsButton();
    updateMonitorFilterButtons();
    updateMonitorOverview();
    updateMonitorMeta();
    renderMonitorSidebar();
    renderMonitorDetail();
    restoreMonitorUIState(previousState);
}

function appendMonitorLog(monitor, type, payload) {
    const timestamp = payload.timestamp ? `[${formatTimestamp(payload.timestamp)}] ` : '';
    const status = payload.status ? `${formatStatus(payload.status)} | ` : '';
    const lineText = `${timestamp}${status}${payload.message || 'Evento do acompanhamento.'}`;

    monitor.logs.push({ type, message: lineText });
    if (monitor.logs.length > MAX_MONITOR_LOG_ENTRIES) {
        monitor.logs = monitor.logs.slice(-MAX_MONITOR_LOG_ENTRIES);
        monitor.logsTrimmed = true;
    }
}

function updateMonitorFromPayload(monitor, payload) {
    const hasOwn = (key) => Object.prototype.hasOwnProperty.call(payload, key);

    if (hasOwn('status') && payload.status) {
        monitor.status = payload.status;
    }
    if (hasOwn('connected')) {
        monitor.connected = Boolean(payload.connected);
    }
    if (hasOwn('currentLatencyMs')) {
        monitor.currentLatencyMs = payload.currentLatencyMs == null ? null : toFiniteNumber(payload.currentLatencyMs, null);
    }
    if (hasOwn('averageLatencyMs')) {
        monitor.averageLatencyMs = payload.averageLatencyMs == null ? null : toFiniteNumber(payload.averageLatencyMs, null);
    }
    if (hasOwn('attempts')) {
        monitor.attempts = toFiniteNumber(payload.attempts, monitor.attempts);
    }
    if (hasOwn('successfulAttempts')) {
        monitor.successfulAttempts = toFiniteNumber(payload.successfulAttempts, monitor.successfulAttempts);
    }
    if (hasOwn('failedAttempts')) {
        monitor.failedAttempts = toFiniteNumber(payload.failedAttempts, monitor.failedAttempts);
    }
    if (hasOwn('outages')) {
        monitor.outages = toFiniteNumber(payload.outages, monitor.outages);
    }
    if (hasOwn('packetLossPercent')) {
        monitor.packetLossPercent = toFiniteNumber(payload.packetLossPercent, monitor.packetLossPercent);
    }
    if (hasOwn('consecutiveFailures')) {
        monitor.consecutiveFailures = toFiniteNumber(payload.consecutiveFailures, monitor.consecutiveFailures);
    }
}

function disconnectMonitorSource(monitor, intentional = true) {
    monitor.intentionalClose = intentional;
    monitor.awaitingReconnect = false;

    if (monitor.source) {
        monitor.source.close();
        monitor.source = null;
    }
}

function stopMonitorSession(monitor, message) {
    if (message) {
        appendMonitorLog(monitor, 'info', { message });
    }

    disconnectMonitorSource(monitor, true);
    monitor.running = false;
    monitor.connected = false;
    monitor.status = 'STOPPED';
    refreshMonitorUI();
    schedulePersistMonitors();
}

function startMonitorSession(monitor) {
    disconnectMonitorSource(monitor, false);
    monitor.running = true;
    monitor.connected = false;
    monitor.status = 'CONNECTING';
    monitor.awaitingReconnect = false;

    const params = new URLSearchParams({
        target: monitor.target,
        intervalMs: String(monitor.intervalMs),
        timeoutMs: String(monitor.timeoutMs)
    });

    const source = new EventSource(`/api/v1/ping/monitor?${params}`);
    monitor.source = source;
    let historyRegistered = false;

    refreshMonitorUI();
    schedulePersistMonitors();

    source.addEventListener('started', (event) => {
        if (!event.data || monitor.source !== source) {
            return;
        }

        const payload = JSON.parse(event.data);
        monitor.awaitingReconnect = false;
        updateMonitorFromPayload(monitor, payload);
        appendMonitorLog(monitor, 'started', payload);
        refreshMonitorUI();
        schedulePersistMonitors();

        if (!historyRegistered) {
            historyRegistered = true;
            pushHistory('Acompanhamento', `${monitor.name} -> ${monitor.target}`);
        }
    });

    source.addEventListener('sample', (event) => {
        if (!event.data || monitor.source !== source) {
            return;
        }

        const payload = JSON.parse(event.data);
        monitor.awaitingReconnect = false;
        updateMonitorFromPayload(monitor, payload);
        appendMonitorLog(monitor, payload.connected ? 'completed' : 'error', payload);
        refreshMonitorUI();
        schedulePersistMonitors();
    });

    source.addEventListener('completed', (event) => {
        if (!event.data || monitor.source !== source) {
            return;
        }

        const payload = JSON.parse(event.data);
        updateMonitorFromPayload(monitor, payload);
        appendMonitorLog(monitor, 'completed', payload);
        disconnectMonitorSource(monitor, true);
        monitor.running = false;
        monitor.connected = false;
        monitor.status = 'STOPPED';
        refreshMonitorUI();
        schedulePersistMonitors();
    });

    source.addEventListener('error', (event) => {
        if (!event.data || monitor.source !== source) {
            return;
        }

        const payload = JSON.parse(event.data);
        updateMonitorFromPayload(monitor, payload);
        appendMonitorLog(monitor, 'error', payload);
        disconnectMonitorSource(monitor, true);
        monitor.running = false;
        monitor.connected = false;
        monitor.status = 'STOPPED';
        refreshMonitorUI();
        schedulePersistMonitors();
    });

    source.onerror = () => {
        if (monitor.source !== source || !monitor.running || monitor.intentionalClose) {
            return;
        }

        if (!monitor.awaitingReconnect) {
            monitor.awaitingReconnect = true;
            monitor.connected = false;
            monitor.status = 'CONNECTING';
            appendMonitorLog(monitor, 'info', { message: 'Conexao com o stream instavel. Tentando reconectar...' });
            refreshMonitorUI();
            schedulePersistMonitors();
        }
    };
}

function exportMonitorLogs(id) {
    const monitor = monitorStore.get(id);
    if (!monitor) {
        return;
    }

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const header = [
        `Monitor: ${monitor.name}`,
        `Alvo: ${monitor.target}`,
        `Status: ${formatStatus(monitor.status)}`,
        `Intervalo: ${Math.round(monitor.intervalMs / 1000)} s`,
        `Timeout: ${monitor.timeoutMs} ms`,
        `Tentativas: ${monitor.attempts}`,
        `Sucessos: ${monitor.successfulAttempts}`,
        `Falhas: ${monitor.failedAttempts}`,
        `Quedas: ${monitor.outages}`,
        `Perda de pacote: ${formatPercent(monitor.packetLossPercent)}`,
        `Ping atual: ${formatLatency(monitor.currentLatencyMs)}`,
        `Ping medio: ${formatLatency(monitor.averageLatencyMs)}`,
        monitor.logsTrimmed ? `Observacao: exportacao contem somente as ultimas ${MAX_MONITOR_LOG_ENTRIES} linhas.` : '',
        '',
        'Logs:'
    ].filter(Boolean);

    const blob = new Blob([header.concat(monitor.logs.map((entry) => entry.message)).join('\r\n')], {
        type: 'text/plain;charset=utf-8'
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${sanitizeFileName(monitor.name)}-${timestamp}.log`;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function exportMonitorReport(id) {
    const monitor = monitorStore.get(id);
    if (!monitor) {
        return;
    }

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const report = buildMonitorNarrativeReport(monitor);
    const blob = new Blob([report], {
        type: 'text/plain;charset=utf-8'
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${sanitizeFileName(monitor.name)}-relatorio-${timestamp}.txt`;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function removeMonitorSession(id) {
    const monitor = monitorStore.get(id);
    if (!monitor) {
        return;
    }

    disconnectMonitorSource(monitor, true);
    monitorStore.delete(id);

    if (activeMonitorId === id) {
        activeMonitorId = null;
    }

    refreshMonitorUI();

    if (!monitorStore.size) {
        localStorage.removeItem(monitorStorageKey);
        return;
    }

    schedulePersistMonitors();
}

function clearAllMonitors() {
    if (!monitorStore.size) {
        return;
    }

    getMonitorEntries().forEach((monitor) => {
        disconnectMonitorSource(monitor, true);
    });

    monitorStore.clear();
    activeMonitorId = null;
    refreshMonitorUI();

    try {
        localStorage.removeItem(monitorStorageKey);
    } catch (error) {
        // Ignore persistence errors and keep the UI usable.
    }
}

function addMonitor(name, target, intervalMs, timeoutMs) {
    const monitor = createMonitorRecord({ name, target, intervalMs, timeoutMs, createdAt: Date.now() });
    monitorStore.set(monitor.id, monitor);
    activeMonitorFilter = 'all';
    activeMonitorId = monitor.id;
    refreshMonitorUI();
    schedulePersistMonitors();
    startMonitorSession(monitor);
}

function restorePersistedMonitors() {
    const { monitors, activeMonitorId: persistedActiveMonitorId } = readPersistedMonitorState();

    monitors.forEach((snapshot) => {
        if (!snapshot || typeof snapshot.name !== 'string' || typeof snapshot.target !== 'string') {
            return;
        }

        const monitor = createMonitorRecord(snapshot);
        monitorStore.set(monitor.id, monitor);
    });

    if (persistedActiveMonitorId && monitorStore.has(persistedActiveMonitorId)) {
        activeMonitorId = persistedActiveMonitorId;
    }

    refreshMonitorUI();

    getMonitorEntries().forEach((monitor) => {
        if (monitor.running) {
            startMonitorSession(monitor);
        }
    });
}

document.getElementById('theme-toggle').addEventListener('click', () => {
    const currentTheme = document.body.dataset.theme === 'dark' ? 'dark' : 'light';
    applyTheme(currentTheme === 'dark' ? 'light' : 'dark');
});

document.getElementById('clear-history').addEventListener('click', () => {
    localStorage.removeItem(historyKey);
    renderHistory();
});

document.getElementById('clear-monitors').addEventListener('click', () => {
    clearAllMonitors();
});

document.getElementById('monitor-filters').addEventListener('click', (event) => {
    const button = event.target.closest('[data-monitor-filter]');
    if (!button) {
        return;
    }

    activeMonitorFilter = button.dataset.monitorFilter;
    refreshMonitorUI();
    schedulePersistMonitors();
});

document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-monitor-action]');
    if (!trigger) {
        return;
    }

    const { monitorAction, monitorId } = trigger.dataset;

    if (monitorAction === 'focus') {
        if (!monitorStore.has(monitorId)) {
            return;
        }

        activeMonitorId = monitorId;
        refreshMonitorUI();
        schedulePersistMonitors();
        return;
    }

    const monitor = monitorStore.get(monitorId);
    if (!monitor) {
        return;
    }

    if (monitorAction === 'toggle') {
        if (monitor.running) {
            stopMonitorSession(monitor, 'Monitor pausado manualmente.');
        } else {
            activeMonitorId = monitorId;
            appendMonitorLog(monitor, 'info', { message: 'Retomando monitoramento.' });
            startMonitorSession(monitor);
        }
        return;
    }

    if (monitorAction === 'export') {
        exportMonitorLogs(monitorId);
        return;
    }

    if (monitorAction === 'report') {
        exportMonitorReport(monitorId);
        return;
    }

    if (monitorAction === 'remove') {
        removeMonitorSession(monitorId);
    }
});

document.getElementById('monitor-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const name = String(form.get('name') || '').trim();
    const target = String(form.get('target') || '').trim();
    const intervalSeconds = Number(form.get('intervalSeconds'));
    const timeoutMs = Number(form.get('timeoutMs'));

    if (!name || !target) {
        return;
    }

    addMonitor(name, target, intervalSeconds * 1000, timeoutMs);
    event.currentTarget.reset();
    event.currentTarget.elements.intervalSeconds.value = '5';
    event.currentTarget.elements.timeoutMs.value = '2000';
});

document.getElementById('ping-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const target = String(form.get('target') || '').trim();
    const count = String(form.get('count') || '').trim();
    const params = new URLSearchParams({ target, count });
    startStream('ping', `/api/v1/ping?${params}`, 'ping-output', 'Ping', `${target} (${count}x)`);
});

document.getElementById('traceroute-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const target = String(form.get('target') || '').trim();
    const params = new URLSearchParams({ target });
    startStream('traceroute', `/api/v1/traceroute?${params}`, 'traceroute-output', 'Traceroute', target);
});

document.getElementById('dns-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const hostname = String(form.get('hostname') || '').trim();
    const params = new URLSearchParams({ hostname });
    requestJson(`/api/v1/dns?${params}`, 'dns-output', 'DNS', hostname);
});

document.getElementById('geolocation-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const target = String(form.get('target') || '').trim();
    const params = new URLSearchParams({ target });
    requestJson(`/api/v1/geolocation?${params}`, 'geolocation-output', 'Geolocalizacao', target);
});

document.getElementById('whois-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const domain = String(form.get('domain') || '').trim();
    const params = new URLSearchParams({ domain });
    requestJson(`/api/v1/whois?${params}`, 'whois-output', 'Whois', domain);
});

document.getElementById('portscan-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const host = String(form.get('host') || '').trim();
    const timeout = Number(form.get('timeout'));
    const ports = String(form.get('ports') || '')
        .split(',')
        .map((value) => Number(value.trim()))
        .filter((value) => Number.isInteger(value));

    requestJson(
        '/api/v1/portscan',
        'portscan-output',
        'Port Scan',
        `${host} -> ${ports.join(', ')}`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ host, ports, timeout })
        }
    );
});

window.addEventListener('beforeunload', () => {
    if (monitorPersistTimer) {
        window.clearTimeout(monitorPersistTimer);
        monitorPersistTimer = null;
    }

    persistMonitors();
    activeStreams.forEach((source) => source.close());
    activeStreams.clear();
    getMonitorEntries().forEach((monitor) => disconnectMonitorSource(monitor, true));
});

['dns-output', 'geolocation-output', 'whois-output', 'portscan-output'].forEach((id) => {
    const element = document.getElementById(id);
    element.classList.add('json-placeholder');
    element.textContent = 'Aguardando execucao...';
});

applyTheme(resolveInitialTheme());
setTerminalPlaceholder('ping-output', 'Aguardando execucao...');
setTerminalPlaceholder('traceroute-output', 'Aguardando execucao...');
restorePersistedMonitors();
refreshMonitorUI();
renderHistory();
