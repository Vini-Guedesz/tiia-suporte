// ==========================================
// TIIA Suporte - Network Tools Frontend
// ==========================================

const API_BASE_URL = '/api/v1';

// ==========================================
// Dark Mode
// ==========================================

function initDarkMode() {
    const darkModeToggle = document.getElementById('darkModeToggle');
    const htmlElement = document.documentElement;

    // Check saved preference or system preference
    const savedTheme = localStorage.getItem('theme');
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
        htmlElement.classList.add('dark');
    }

    darkModeToggle.addEventListener('click', () => {
        htmlElement.classList.toggle('dark');
        const isDark = htmlElement.classList.contains('dark');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    initDarkMode();
});

// ==========================================
// Utility Functions
// ==========================================

function showLoading(button) {
    const originalText = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `
        <div class="flex items-center justify-center space-x-2">
            <div class="loading"></div>
            <span>Carregando...</span>
        </div>
    `;
    return originalText;
}

function hideLoading(button, originalText) {
    button.disabled = false;
    button.innerHTML = originalText;
}

function showResults(data, title = 'Resultados') {
    const resultsSection = document.getElementById('resultsSection');
    const resultsText = document.getElementById('resultsText');

    resultsSection.classList.remove('hidden');
    resultsSection.classList.add('fade-in');

    // Format JSON if it's an object
    if (typeof data === 'object') {
        resultsText.textContent = JSON.stringify(data, null, 2);
    } else {
        resultsText.textContent = data;
    }

    // Scroll to results
    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function showError(message) {
    showNotification(message, 'error');
}

function showSuccess(message) {
    showNotification(message, 'success');
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `fixed top-20 right-4 z-50 px-6 py-4 rounded-lg shadow-lg fade-in ${
        type === 'error' ? 'bg-red-500' :
        type === 'success' ? 'bg-green-500' :
        'bg-blue-500'
    } text-white max-w-md`;

    notification.innerHTML = `
        <div class="flex items-center space-x-3">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                ${type === 'error' ?
                    '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>' :
                type === 'success' ?
                    '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>' :
                    '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>'
                }
            </svg>
            <p class="font-medium">${message}</p>
        </div>
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(100%)';
        notification.style.transition = 'all 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 4000);
}

function closeResults() {
    const resultsSection = document.getElementById('resultsSection');
    resultsSection.classList.add('hidden');
}

// ==========================================
// Input Validation
// ==========================================

function validateHostname(hostname) {
    const hostnameRegex = /^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$/;
    return hostnameRegex.test(hostname);
}

function validateIP(ip) {
    // IPv4
    const ipv4Regex = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    if (ipv4Regex.test(ip)) return true;

    // IPv6 (simplified)
    const ipv6Regex = /^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$/;
    return ipv6Regex.test(ip);
}

function validateIPOrHostname(input) {
    return validateIP(input) || validateHostname(input);
}

// ==========================================
// API Functions
// ==========================================

async function lookupDomain() {
    const input = document.getElementById('domainInput');
    const domain = input.value.trim();

    if (!domain) {
        showError('Por favor, insira um domínio');
        input.focus();
        return;
    }

    if (!validateHostname(domain)) {
        showError('Formato de domínio inválido');
        input.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        const response = await fetch(`${API_BASE_URL}/domain/${encodeURIComponent(domain)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.text();
        showResults(data, `Domain Lookup: ${domain}`);
        showSuccess('Consulta de domínio realizada com sucesso!');
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao consultar domínio: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

async function dnsLookup() {
    const input = document.getElementById('dnsInput');
    const host = input.value.trim();

    if (!host) {
        showError('Por favor, insira um host');
        input.focus();
        return;
    }

    if (!validateHostname(host)) {
        showError('Formato de host inválido');
        input.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        const response = await fetch(`${API_BASE_URL}/dnslookup/${encodeURIComponent(host)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        showResults(data, `DNS Lookup: ${host}`);

        if (data.error) {
            showError(data.error);
        } else {
            showSuccess('Consulta DNS realizada com sucesso!');
        }
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao consultar DNS: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

async function geoLookup() {
    const input = document.getElementById('geoInput');
    const ip = input.value.trim();

    if (!ip) {
        showError('Por favor, insira um endereço IP');
        input.focus();
        return;
    }

    if (!validateIP(ip)) {
        showError('Formato de IP inválido');
        input.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        const response = await fetch(`${API_BASE_URL}/geolocalizacao/${encodeURIComponent(ip)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        showResults(data, `Geolocalização: ${ip}`);

        if (data.status === 'fail') {
            showError(data.message || 'Erro ao obter geolocalização');
        } else {
            showSuccess('Geolocalização obtida com sucesso!');
        }
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao obter geolocalização: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

async function ping() {
    const input = document.getElementById('pingInput');
    const host = input.value.trim();

    if (!host) {
        showError('Por favor, insira um host');
        input.focus();
        return;
    }

    if (!validateIPOrHostname(host)) {
        showError('Formato de host/IP inválido');
        input.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        const response = await fetch(`${API_BASE_URL}/ping/${encodeURIComponent(host)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.text();
        showResults(data, `Ping: ${host}`);
        showSuccess('Ping executado com sucesso!');
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao executar ping: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

async function portScan() {
    const hostInput = document.getElementById('portHost');
    const portListInput = document.getElementById('portList');

    const host = hostInput.value.trim();
    const portList = portListInput.value.trim();

    if (!host) {
        showError('Por favor, insira um host');
        hostInput.focus();
        return;
    }

    if (!portList) {
        showError('Por favor, insira as portas (ex: 80,443,22)');
        portListInput.focus();
        return;
    }

    if (!validateIPOrHostname(host)) {
        showError('Formato de host/IP inválido');
        hostInput.focus();
        return;
    }

    // Validate port list
    const ports = portList.split(',').map(p => p.trim());
    const validPorts = ports.every(p => {
        const num = parseInt(p);
        return !isNaN(num) && num >= 1 && num <= 65535;
    });

    if (!validPorts) {
        showError('Portas inválidas. Use números de 1 a 65535 separados por vírgula');
        portListInput.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        const url = `${API_BASE_URL}/portscan/${encodeURIComponent(host)}?ports=${portList}&timeout=2000`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();

        // Format port scan results
        const formattedResult = {
            host: host,
            totalPortsScanned: ports.length,
            openPorts: data.length,
            closedPorts: ports.length - data.length,
            openPortsList: data
        };

        showResults(formattedResult, `Port Scan: ${host}`);

        if (data.length > 0) {
            showSuccess(`${data.length} porta(s) aberta(s) encontrada(s)!`);
        } else {
            showError('Nenhuma porta aberta encontrada');
        }
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao escanear portas: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

async function traceroute() {
    const input = document.getElementById('traceInput');
    const host = input.value.trim();

    if (!host) {
        showError('Por favor, insira um host');
        input.focus();
        return;
    }

    if (!validateIPOrHostname(host)) {
        showError('Formato de host/IP inválido');
        input.focus();
        return;
    }

    const button = event.target;
    const originalText = showLoading(button);

    try {
        // Use the raw traceroute endpoint for simpler output
        const response = await fetch(`${API_BASE_URL}/traceroute/${encodeURIComponent(host)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const data = await response.text();
        showResults(data, `Traceroute: ${host}`);
        showSuccess('Traceroute executado com sucesso!');
    } catch (error) {
        console.error('Error:', error);
        showError(`Erro ao executar traceroute: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

// ==========================================
// Keyboard Shortcuts
// ==========================================

document.addEventListener('keydown', (e) => {
    // Enter key to submit when input is focused
    if (e.key === 'Enter') {
        const activeElement = document.activeElement;

        if (activeElement.id === 'domainInput') lookupDomain();
        else if (activeElement.id === 'dnsInput') dnsLookup();
        else if (activeElement.id === 'geoInput') geoLookup();
        else if (activeElement.id === 'pingInput') ping();
        else if (activeElement.id === 'portList') portScan();
        else if (activeElement.id === 'traceInput') traceroute();
    }

    // ESC to close results
    if (e.key === 'Escape') {
        closeResults();
    }
});

// ==========================================
// Console Welcome Message
// ==========================================

console.log('%c🌐 TIIA Suporte', 'font-size: 20px; font-weight: bold; color: #3b82f6;');
console.log('%cFerramentas de Diagnóstico de Rede', 'font-size: 14px; color: #6b7280;');
console.log('%cAPI Docs: /swagger-ui.html', 'font-size: 12px; color: #10b981;');

// ==========================================
// Enhanced Features
// ==========================================

// Debounce utility
const debounce = (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
};

// Copy to Clipboard
function copyToClipboard() {
    const text = document.getElementById('resultsText').textContent;
    navigator.clipboard.writeText(text).then(() => {
        showSuccess('Resultado copiado para área de transferência!');
    }).catch(err => {
        showError('Erro ao copiar: ' + err.message);
    });
}

// Export as JSON
function exportAsJSON() {
    const text = document.getElementById('resultsText').textContent;
    let data;
    try {
        data = JSON.parse(text);
    } catch {
        data = { result: text };
    }
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    downloadFile(blob, 'resultado.json');
}

// Export as Text
function exportAsText() {
    const text = document.getElementById('resultsText').textContent;
    const blob = new Blob([text], { type: 'text/plain' });
    downloadFile(blob, 'resultado.txt');
}

// Download file helper
function downloadFile(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showSuccess(`Arquivo ${filename} baixado!`);
}

// History Management
function saveToHistory(tool, query, result) {
    const history = JSON.parse(localStorage.getItem('queryHistory') || '[]');
    history.unshift({
        tool,
        query,
        result: typeof result === 'string' ? result.substring(0, 200) : JSON.stringify(result).substring(0, 200),
        timestamp: Date.now()
    });
    localStorage.setItem('queryHistory', JSON.stringify(history.slice(0, 50)));
}

function loadHistory() {
    return JSON.parse(localStorage.getItem('queryHistory') || '[]');
}

// Favorites Management
function addToFavorites(tool, query) {
    const favorites = JSON.parse(localStorage.getItem('favorites') || '[]');
    const exists = favorites.some(f => f.tool === tool && f.query === query);

    if (!exists) {
        favorites.push({ tool, query, timestamp: Date.now() });
        localStorage.setItem('favorites', JSON.stringify(favorites));
        showSuccess('Adicionado aos favoritos!');
    } else {
        showError('Já está nos favoritos!');
    }
}

function loadFavorites() {
    return JSON.parse(localStorage.getItem('favorites') || '[]');
}

// Offline/Online Detection
window.addEventListener('offline', () => {
    showError('Conexão perdida. Verifique sua internet.');
    document.body.classList.add('offline');
});

window.addEventListener('online', () => {
    showSuccess('Conexão restaurada!');
    document.body.classList.remove('offline');
});

// Visual Validation Feedback
function addValidationFeedback(inputId, validatorFunc) {
    const input = document.getElementById(inputId);
    if (!input) return;

    const debouncedValidation = debounce(() => {
        const isValid = validatorFunc(input.value.trim());

        if (input.value.trim() === '') {
            input.classList.remove('border-red-500', 'border-green-500');
            input.classList.add('border-gray-300', 'dark:border-gray-600');
        } else if (isValid) {
            input.classList.remove('border-red-500', 'border-gray-300');
            input.classList.add('border-green-500');
        } else {
            input.classList.remove('border-green-500', 'border-gray-300');
            input.classList.add('border-red-500');
        }
    }, 300);

    input.addEventListener('input', debouncedValidation);
}

// Initialize validation feedback on page load
document.addEventListener('DOMContentLoaded', () => {
    addValidationFeedback('domainInput', validateHostname);
    addValidationFeedback('dnsInput', validateHostname);
    addValidationFeedback('geoInput', validateIP);
    addValidationFeedback('pingInput', validateIPOrHostname);
    addValidationFeedback('portHost', validateIPOrHostname);
    addValidationFeedback('traceInput', validateIPOrHostname);
});
