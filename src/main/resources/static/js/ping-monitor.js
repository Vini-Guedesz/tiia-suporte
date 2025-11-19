// WebSocket connection
let stompClient = null;
let connected = false;
const targets = new Map();
const charts = new Map();
const API_BASE = '/api/ping-monitor';

// Dark mode
document.getElementById('darkModeToggle').addEventListener('click', () => {
    document.documentElement.classList.toggle('dark');
    localStorage.setItem('darkMode', document.documentElement.classList.contains('dark'));

    // Update charts colors
    charts.forEach(chart => {
        updateChartColors(chart);
    });
});

if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark');
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    connectWebSocket();
    loadExistingTargets();
    loadSummary();
    setInterval(loadSummary, 5000); // Update summary every 5 seconds
});

// WebSocket connection
function connectWebSocket() {
    const socket = new SockJS('/ws-ping-monitor');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, (frame) => {
        console.log('Connected: ' + frame);
        connected = true;
        updateConnectionStatus(true);

        stompClient.subscribe('/topic/ping-monitor', (message) => {
            const result = JSON.parse(message.body);
            updateTargetCard(result);
        });
    }, (error) => {
        console.error('WebSocket error:', error);
        connected = false;
        updateConnectionStatus(false);
        setTimeout(connectWebSocket, 5000);
    });
}

function updateConnectionStatus(isConnected) {
    const indicator = document.getElementById('statusIndicator');
    const text = document.getElementById('statusText');

    if (isConnected) {
        indicator.className = 'w-3 h-3 rounded-full bg-green-500 pulse-animation';
        text.textContent = 'Conectado';
    } else {
        indicator.className = 'w-3 h-3 rounded-full bg-red-500';
        text.textContent = 'Desconectado';
    }
}

// Load summary
async function loadSummary() {
    try {
        const response = await fetch(`${API_BASE}/summary`);
        const summary = await response.json();

        document.getElementById('summary-total').textContent = summary.total;
        document.getElementById('summary-online').textContent = summary.online;
        document.getElementById('summary-offline').textContent = summary.offline;
        document.getElementById('summary-paused').textContent = summary.paused;
    } catch (error) {
        console.error('Error loading summary:', error);
    }
}

// Load existing targets
async function loadExistingTargets() {
    try {
        const response = await fetch(`${API_BASE}/targets`);
        const existingTargets = await response.json();

        Object.entries(existingTargets).forEach(([id, target]) => {
            targets.set(id, target);
            createTargetCard(id, target);
        });

        updateEmptyState();
    } catch (error) {
        console.error('Error loading targets:', error);
    }
}

// Input validation
function isValidIP(ip) {
    const ipPattern = /^(\d{1,3}\.){3}\d{1,3}$/;
    if (!ipPattern.test(ip)) return false;

    const parts = ip.split('.');
    return parts.every(part => parseInt(part) >= 0 && parseInt(part) <= 255);
}

function isValidHostname(hostname) {
    const hostnamePattern = /^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$/;
    return hostnamePattern.test(hostname) || /^[a-zA-Z0-9\-]+$/.test(hostname);
}

// Add new target
async function addTarget() {
    const nameInput = document.getElementById('nameInput');
    const targetInput = document.getElementById('targetInput');
    const name = nameInput.value.trim();
    const target = targetInput.value.trim();

    if (!target) {
        showError('Por favor, insira um IP ou hostname válido');
        return;
    }

    // Validate IP or hostname
    if (!isValidIP(target) && !isValidHostname(target)) {
        showError('IP ou hostname inválido. Use formato válido como "192.168.1.1" ou "google.com"');
        targetInput.classList.add('border-red-500');
        setTimeout(() => targetInput.classList.remove('border-red-500'), 3000);
        return;
    }

    try {
        let url = `${API_BASE}/targets?target=${encodeURIComponent(target)}`;
        if (name) {
            url += `&name=${encodeURIComponent(name)}`;
        }

        const response = await fetch(url, {
            method: 'POST'
        });

        if (response.ok) {
            const newTarget = await response.json();
            targets.set(newTarget.id, newTarget);
            createTargetCard(newTarget.id, newTarget);
            nameInput.value = '';
            targetInput.value = '';
            updateEmptyState();
            loadSummary();
            showSuccess('Alvo adicionado com sucesso!');
        } else {
            showError('Erro ao adicionar alvo');
        }
    } catch (error) {
        console.error('Error adding target:', error);
        showError('Erro ao adicionar alvo');
    }
}

function showError(message) {
    // Simple toast notification
    const toast = document.createElement('div');
    toast.className = 'fixed bottom-4 right-4 bg-red-600 text-white px-6 py-3 rounded-lg shadow-lg fade-in z-50';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function showSuccess(message) {
    const toast = document.createElement('div');
    toast.className = 'fixed bottom-4 right-4 bg-green-600 text-white px-6 py-3 rounded-lg shadow-lg fade-in z-50';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

// Create target card
function createTargetCard(targetId, target) {
    const grid = document.getElementById('monitorsGrid');

    const card = document.createElement('div');
    card.id = `card-${targetId}`;
    card.className = 'bg-white dark:bg-gray-800 rounded-xl shadow-lg p-6 fade-in';
    card.dataset.targetId = targetId;
    card.dataset.name = (target.name || target.target).toLowerCase();
    card.dataset.target = target.target.toLowerCase();

    const displayName = target.name || target.target;
    const subtitle = target.name ? target.target : 'Aguardando dados...';

    card.innerHTML = `
        <div class="flex items-center justify-between mb-4">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-teal-100 dark:bg-teal-900 rounded-lg flex items-center justify-center">
                    <div class="w-3 h-3 rounded-full bg-gray-400" id="status-${targetId}"></div>
                </div>
                <div>
                    <h3 class="text-lg font-bold text-gray-800 dark:text-white">${displayName}</h3>
                    <p class="text-xs text-gray-500 dark:text-gray-400" id="subtitle-${targetId}">${subtitle}</p>
                </div>
            </div>
            <div class="flex space-x-2">
                <button onclick="showConfig('${targetId}')"
                    class="p-2 rounded-lg bg-blue-100 dark:bg-blue-900 hover:bg-blue-200 dark:hover:bg-blue-800 text-blue-600 dark:text-blue-300 transition-colors"
                    title="Configurações">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                </button>
                <button onclick="pauseTarget('${targetId}')" id="pause-btn-${targetId}"
                    class="p-2 rounded-lg bg-yellow-100 dark:bg-yellow-900 hover:bg-yellow-200 dark:hover:bg-yellow-800 text-yellow-600 dark:text-yellow-300 transition-colors"
                    title="Pausar">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                </button>
                <button onclick="removeTarget('${targetId}')"
                    class="p-2 rounded-lg bg-red-100 dark:bg-red-900 hover:bg-red-200 dark:hover:bg-red-800 text-red-600 dark:text-red-300 transition-colors"
                    title="Remover">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                    </svg>
                </button>
            </div>
        </div>

        <!-- Chart -->
        <div class="mb-4 bg-gray-50 dark:bg-gray-900 rounded-lg p-3">
            <canvas id="chart-${targetId}" height="120"></canvas>
        </div>

        <!-- Statistics -->
        <div class="grid grid-cols-2 gap-2 mb-3">
            <div class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
                <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Status</span>
                <p class="text-sm font-bold mt-1" id="status-text-${targetId}">-</p>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
                <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Latência</span>
                <p class="text-sm font-bold text-blue-600 dark:text-blue-400 mt-1" id="latency-${targetId}">- ms</p>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
                <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Uptime</span>
                <p class="text-sm font-bold text-green-600 dark:text-green-400 mt-1" id="uptime-${targetId}">-</p>
            </div>
            <div class="p-3 bg-gray-50 dark:bg-gray-900 rounded-lg">
                <span class="text-xs font-medium text-gray-600 dark:text-gray-400">Quedas</span>
                <p class="text-sm font-bold text-red-600 dark:text-red-400 mt-1" id="drops-${targetId}">0</p>
            </div>
        </div>

        <!-- Extended Stats (collapsible) -->
        <details class="cursor-pointer">
            <summary class="text-sm font-semibold text-teal-600 dark:text-teal-400 hover:underline">Ver estatísticas detalhadas</summary>
            <div class="mt-3 space-y-2">
                <div class="flex justify-between p-2 bg-gray-50 dark:bg-gray-900 rounded">
                    <span class="text-xs text-gray-600 dark:text-gray-400">Lat. Mín</span>
                    <span class="text-xs font-bold text-gray-800 dark:text-white" id="min-latency-${targetId}">- ms</span>
                </div>
                <div class="flex justify-between p-2 bg-gray-50 dark:bg-gray-900 rounded">
                    <span class="text-xs text-gray-600 dark:text-gray-400">Lat. Média</span>
                    <span class="text-xs font-bold text-gray-800 dark:text-white" id="avg-latency-${targetId}">- ms</span>
                </div>
                <div class="flex justify-between p-2 bg-gray-50 dark:bg-gray-900 rounded">
                    <span class="text-xs text-gray-600 dark:text-gray-400">Lat. Máx</span>
                    <span class="text-xs font-bold text-gray-800 dark:text-white" id="max-latency-${targetId}">- ms</span>
                </div>
                <div class="flex justify-between p-2 bg-gray-50 dark:bg-gray-900 rounded">
                    <span class="text-xs text-gray-600 dark:text-gray-400">Perda de Pacotes</span>
                    <span class="text-xs font-bold text-gray-800 dark:text-white" id="packet-loss-${targetId}">- %</span>
                </div>
            </div>
        </details>
    `;

    grid.appendChild(card);

    // Create chart
    createChart(targetId);
}

// Create chart for target
function createChart(targetId) {
    const ctx = document.getElementById(`chart-${targetId}`);
    if (!ctx) return;

    const isDark = document.documentElement.classList.contains('dark');

    const chart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Latência (ms)',
                data: [],
                borderColor: '#14b8a6',
                backgroundColor: 'rgba(20, 184, 166, 0.1)',
                borderWidth: 2,
                tension: 0.4,
                fill: true,
                pointRadius: 3,
                pointHoverRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                x: {
                    display: true,
                    grid: {
                        color: isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
                    },
                    ticks: {
                        color: isDark ? '#9ca3af' : '#6b7280',
                        maxTicksLimit: 8
                    }
                },
                y: {
                    display: true,
                    beginAtZero: true,
                    grid: {
                        color: isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
                    },
                    ticks: {
                        color: isDark ? '#9ca3af' : '#6b7280'
                    }
                }
            },
            interaction: {
                mode: 'nearest',
                axis: 'x',
                intersect: false
            }
        }
    });

    charts.set(targetId, chart);
}

function updateChartColors(chart) {
    const isDark = document.documentElement.classList.contains('dark');
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)';
    const tickColor = isDark ? '#9ca3af' : '#6b7280';

    chart.options.scales.x.grid.color = gridColor;
    chart.options.scales.x.ticks.color = tickColor;
    chart.options.scales.y.grid.color = gridColor;
    chart.options.scales.y.ticks.color = tickColor;
    chart.update();
}

// Update target card with new data
function updateTargetCard(result) {
    const targetId = result.targetId;
    const target = targets.get(targetId);

    if (!target) return;

    // Update target data
    target.currentlyOnline = result.online;

    // Update status indicator
    const statusIndicator = document.getElementById(`status-${targetId}`);
    const statusText = document.getElementById(`status-text-${targetId}`);

    if (statusIndicator && statusText) {
        if (result.online) {
            statusIndicator.className = 'w-3 h-3 rounded-full bg-green-500 pulse-animation';
            statusText.className = 'text-sm font-bold text-green-600 dark:text-green-400';
            statusText.textContent = 'Online';
        } else {
            statusIndicator.className = 'w-3 h-3 rounded-full bg-red-500';
            statusText.className = 'text-sm font-bold text-red-600 dark:text-red-400';
            statusText.textContent = 'Offline';
        }
    }

    // Update subtitle with timestamp
    const subtitleEl = document.getElementById(`subtitle-${targetId}`);
    if (subtitleEl && target.name) {
        const time = new Date(result.timestamp).toLocaleTimeString('pt-BR');
        subtitleEl.textContent = `${target.target} • ${time}`;
    }

    // Update metrics
    const latencyEl = document.getElementById(`latency-${targetId}`);
    if (latencyEl && result.latencyMs !== null) {
        latencyEl.textContent = `${result.latencyMs.toFixed(1)} ms`;
    }

    // Update statistics if available
    if (result.statistics) {
        const stats = result.statistics;

        const uptimeEl = document.getElementById(`uptime-${targetId}`);
        if (uptimeEl) {
            uptimeEl.textContent = `${stats.uptimePercentage.toFixed(1)}%`;
        }

        const dropsEl = document.getElementById(`drops-${targetId}`);
        if (dropsEl) {
            dropsEl.textContent = stats.connectionDrops;
        }

        const minLatEl = document.getElementById(`min-latency-${targetId}`);
        if (minLatEl) {
            minLatEl.textContent = stats.minLatency > 0 ? `${stats.minLatency.toFixed(1)} ms` : '- ms';
        }

        const avgLatEl = document.getElementById(`avg-latency-${targetId}`);
        if (avgLatEl) {
            avgLatEl.textContent = stats.avgLatency > 0 ? `${stats.avgLatency.toFixed(1)} ms` : '- ms';
        }

        const maxLatEl = document.getElementById(`max-latency-${targetId}`);
        if (maxLatEl) {
            maxLatEl.textContent = stats.maxLatency > 0 ? `${stats.maxLatency.toFixed(1)} ms` : '- ms';
        }

        const packetLossEl = document.getElementById(`packet-loss-${targetId}`);
        if (packetLossEl && result.packetLoss !== null) {
            packetLossEl.textContent = `${result.packetLoss.toFixed(1)}%`;
        }

        // Store for filtering/sorting
        target.statistics = stats;
    }

    // Update chart
    const chart = charts.get(targetId);
    if (chart && result.online && result.latencyMs !== null) {
        const time = new Date(result.timestamp).toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });

        chart.data.labels.push(time);
        chart.data.datasets[0].data.push(result.latencyMs);

        // Keep only last 20 data points
        if (chart.data.labels.length > 20) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }

        chart.update('none'); // Update without animation for performance
    }

    // Update summary after each update
    loadSummary();
}

// Show config modal
function showConfig(targetId) {
    const target = targets.get(targetId);
    if (!target) return;

    const config = target.config || { intervalSeconds: 2, packetCount: 2, latencyThresholdMs: 100 };

    const modal = document.createElement('div');
    modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 fade-in';
    modal.innerHTML = `
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl p-6 max-w-md w-full mx-4">
            <h3 class="text-xl font-bold text-gray-800 dark:text-white mb-4">Configurações - ${target.name || target.target}</h3>

            <div class="space-y-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Intervalo de Ping (segundos)
                    </label>
                    <input type="number" id="config-interval" value="${config.intervalSeconds}" min="1" max="60"
                        class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-teal-500 dark:bg-gray-700 dark:text-white">
                    <p class="text-xs text-gray-500 mt-1">Entre 1 e 60 segundos</p>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Número de Pacotes
                    </label>
                    <input type="number" id="config-packets" value="${config.packetCount}" min="1" max="10"
                        class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-teal-500 dark:bg-gray-700 dark:text-white">
                    <p class="text-xs text-gray-500 mt-1">Entre 1 e 10 pacotes</p>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Limite de Latência (ms)
                    </label>
                    <input type="number" id="config-threshold" value="${config.latencyThresholdMs}" min="1"
                        class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-teal-500 dark:bg-gray-700 dark:text-white">
                    <p class="text-xs text-gray-500 mt-1">Alerta quando latência exceder este valor</p>
                </div>
            </div>

            <div class="flex space-x-3 mt-6">
                <button onclick="saveConfig('${targetId}')"
                    class="flex-1 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-lg transition-all">
                    Salvar
                </button>
                <button onclick="this.closest('.fixed').remove()"
                    class="flex-1 px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white font-semibold rounded-lg transition-all">
                    Cancelar
                </button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
}

// Save config
async function saveConfig(targetId) {
    const interval = parseInt(document.getElementById('config-interval').value);
    const packets = parseInt(document.getElementById('config-packets').value);
    const threshold = parseFloat(document.getElementById('config-threshold').value);

    const config = {
        intervalSeconds: interval,
        packetCount: packets,
        latencyThresholdMs: threshold,
        enableAlerts: true
    };

    try {
        const response = await fetch(`${API_BASE}/targets/${targetId}/config`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });

        if (response.ok) {
            const target = targets.get(targetId);
            if (target) {
                target.config = config;
            }
            showSuccess('Configuração atualizada com sucesso!');
            document.querySelector('.fixed.inset-0').remove();
        } else {
            showError('Erro ao atualizar configuração');
        }
    } catch (error) {
        console.error('Error saving config:', error);
        showError('Erro ao atualizar configuração');
    }
}

// Pause target
async function pauseTarget(targetId) {
    const target = targets.get(targetId);
    const endpoint = target.active ? 'pause' : 'resume';

    try {
        const response = await fetch(`${API_BASE}/targets/${targetId}/${endpoint}`, {
            method: 'PUT'
        });

        if (response.ok) {
            target.active = !target.active;
            const btn = document.getElementById(`pause-btn-${targetId}`);

            if (target.active) {
                btn.innerHTML = `
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                `;
                btn.title = 'Pausar';
            } else {
                btn.innerHTML = `
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                `;
                btn.title = 'Retomar';
            }

            loadSummary();
        }
    } catch (error) {
        console.error('Error pausing/resuming target:', error);
    }
}

// Remove target
async function removeTarget(targetId) {
    if (!confirm('Deseja realmente remover este alvo de monitoramento?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/targets/${targetId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            const card = document.getElementById(`card-${targetId}`);
            if (card) {
                card.remove();
            }

            // Destroy chart
            const chart = charts.get(targetId);
            if (chart) {
                chart.destroy();
                charts.delete(targetId);
            }

            targets.delete(targetId);
            updateEmptyState();
            loadSummary();
            showSuccess('Alvo removido com sucesso!');
        }
    } catch (error) {
        console.error('Error removing target:', error);
        showError('Erro ao remover alvo');
    }
}

// Filters and sorting
function applyFilters() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;
    const sortBy = document.getElementById('sortBy').value;

    let filteredTargets = Array.from(targets.values());

    // Apply search filter
    if (searchTerm) {
        filteredTargets = filteredTargets.filter(target => {
            const name = (target.name || '').toLowerCase();
            const ip = target.target.toLowerCase();
            return name.includes(searchTerm) || ip.includes(searchTerm);
        });
    }

    // Apply status filter
    if (statusFilter !== 'all') {
        filteredTargets = filteredTargets.filter(target => {
            if (statusFilter === 'online') return target.currentlyOnline && target.active;
            if (statusFilter === 'offline') return !target.currentlyOnline && target.active;
            if (statusFilter === 'paused') return !target.active;
            return true;
        });
    }

    // Sort
    filteredTargets.sort((a, b) => {
        if (sortBy === 'name') {
            const nameA = (a.name || a.target).toLowerCase();
            const nameB = (b.name || b.target).toLowerCase();
            return nameA.localeCompare(nameB);
        }
        if (sortBy === 'latency') {
            const latA = a.statistics?.avgLatency || 0;
            const latB = b.statistics?.avgLatency || 0;
            return latB - latA;
        }
        if (sortBy === 'uptime') {
            const upA = a.statistics?.uptimePercentage || 0;
            const upB = b.statistics?.uptimePercentage || 0;
            return upB - upA;
        }
        if (sortBy === 'drops') {
            const dropA = a.statistics?.connectionDrops || 0;
            const dropB = b.statistics?.connectionDrops || 0;
            return dropB - dropA;
        }
        return 0;
    });

    // Hide all cards
    document.querySelectorAll('[id^="card-"]').forEach(card => {
        card.style.display = 'none';
    });

    // Show filtered and sorted cards
    const grid = document.getElementById('monitorsGrid');
    filteredTargets.forEach(target => {
        const card = document.getElementById(`card-${target.id}`);
        if (card) {
            card.style.display = 'block';
            grid.appendChild(card); // Re-append to reorder
        }
    });
}

function clearFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('statusFilter').value = 'all';
    document.getElementById('sortBy').value = 'name';
    applyFilters();
}

// Update empty state visibility
function updateEmptyState() {
    const emptyState = document.getElementById('emptyState');
    const grid = document.getElementById('monitorsGrid');

    if (targets.size === 0) {
        emptyState.classList.remove('hidden');
        grid.classList.add('hidden');
    } else {
        emptyState.classList.add('hidden');
        grid.classList.remove('hidden');
    }
}
