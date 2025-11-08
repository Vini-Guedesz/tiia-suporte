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

function showSkeletonLoading() {
    const resultsSection = document.getElementById('resultsSection');
    const resultsContent = document.getElementById('resultsContent');

    resultsSection.classList.remove('hidden');
    resultsContent.innerHTML = `
        <div class="space-y-3">
            <div class="skeleton skeleton-line w-full"></div>
            <div class="skeleton skeleton-line w-5/6"></div>
            <div class="skeleton skeleton-line w-4/6"></div>
            <div class="skeleton skeleton-text w-full"></div>
            <div class="skeleton skeleton-text w-3/4"></div>
            <div class="skeleton skeleton-text w-5/6"></div>
            <div class="skeleton skeleton-line w-full mt-4"></div>
            <div class="skeleton skeleton-line w-4/5"></div>
        </div>
    `;

    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideSkeletonLoading() {
    const resultsContent = document.getElementById('resultsContent');
    resultsContent.innerHTML = '<pre id="resultsText" class="text-sm text-gray-800 dark:text-gray-200 whitespace-pre-wrap" role="log" aria-live="polite"></pre>';
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
    const mapContainer = document.getElementById('mapContainer');
    resultsSection.classList.add('hidden');
    if (mapContainer) {
        mapContainer.classList.add('hidden');
    }
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

    // Show skeleton loading
    showSkeletonLoading();

    try {
        // Use the geo traceroute endpoint for map visualization
        const response = await fetch(`${API_BASE_URL}/traceroute/geo/${encodeURIComponent(host)}`);

        if (!response.ok) {
            throw new Error(`Erro: ${response.status} ${response.statusText}`);
        }

        const hops = await response.json();

        // Format text output
        let textOutput = `Traceroute para ${host}\n`;
        textOutput += `Total de saltos: ${hops.length}\n`;
        textOutput += `${'='.repeat(80)}\n\n`;

        hops.forEach(hop => {
            textOutput += `Salto ${hop.hopNumber}:\n`;
            textOutput += `  IP: ${hop.ipAddress || 'N/A'}\n`;
            textOutput += `  Hostname: ${hop.hostname || 'Unknown'}\n`;
            if (hop.city || hop.country) {
                textOutput += `  Localização: ${hop.city || 'N/A'}, ${hop.country || 'N/A'}\n`;
            }
            if (hop.lat && hop.lon) {
                textOutput += `  Coordenadas: ${hop.lat.toFixed(4)}, ${hop.lon.toFixed(4)}\n`;
            }
            textOutput += `\n`;
        });

        hideSkeletonLoading();
        showResults(textOutput, `Traceroute: ${host}`);

        // Render map if we have geolocation data
        const validHops = hops.filter(hop => hop.lat && hop.lon && hop.lat !== 0 && hop.lon !== 0);
        if (validHops.length > 0) {
            renderTracerouteMap(validHops);
        }

        showSuccess('Traceroute executado com sucesso!');
    } catch (error) {
        console.error('Error:', error);
        hideSkeletonLoading();
        showError(`Erro ao executar traceroute: ${error.message}`);
    } finally {
        hideLoading(button, originalText);
    }
}

// ==========================================
// Traceroute Map Rendering
// ==========================================

let tracerouteMap = null; // Store map instance

function renderTracerouteMap(hops) {
    const mapContainer = document.getElementById('mapContainer');
    const mapElement = document.getElementById('map');

    // Show map container
    mapContainer.classList.remove('hidden');

    // Clear previous map if exists
    if (tracerouteMap) {
        tracerouteMap.remove();
        tracerouteMap = null;
    }

    // Calculate center point (average of all coordinates)
    const avgLat = hops.reduce((sum, hop) => sum + hop.lat, 0) / hops.length;
    const avgLon = hops.reduce((sum, hop) => sum + hop.lon, 0) / hops.length;

    // Initialize map
    tracerouteMap = L.map('map').setView([avgLat, avgLon], 3);

    // Add tile layer (OpenStreetMap)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(tracerouteMap);

    // Create markers and collect coordinates for polyline
    const latLngs = [];
    const markers = [];

    hops.forEach((hop, index) => {
        const latLng = [hop.lat, hop.lon];
        latLngs.push(latLng);

        // Create custom icon with hop number
        const icon = L.divIcon({
            className: 'custom-marker',
            html: `<div style="background-color: ${getHopColor(index, hops.length)};
                   color: white;
                   border-radius: 50%;
                   width: 32px;
                   height: 32px;
                   display: flex;
                   align-items: center;
                   justify-content: center;
                   font-weight: bold;
                   font-size: 12px;
                   border: 2px solid white;
                   box-shadow: 0 2px 4px rgba(0,0,0,0.3);">
                   ${hop.hopNumber}
                   </div>`,
            iconSize: [32, 32],
            iconAnchor: [16, 16]
        });

        // Create marker
        const marker = L.marker(latLng, { icon: icon }).addTo(tracerouteMap);

        // Create popup content
        const popupContent = `
            <div style="min-width: 200px;">
                <h4 style="margin: 0 0 8px 0; font-weight: bold; color: #3b82f6;">Salto ${hop.hopNumber}</h4>
                <p style="margin: 4px 0;"><strong>IP:</strong> ${hop.ipAddress || 'N/A'}</p>
                <p style="margin: 4px 0;"><strong>Hostname:</strong> ${hop.hostname || 'Unknown'}</p>
                ${hop.city ? `<p style="margin: 4px 0;"><strong>Cidade:</strong> ${hop.city}</p>` : ''}
                ${hop.country ? `<p style="margin: 4px 0;"><strong>País:</strong> ${hop.country}</p>` : ''}
                <p style="margin: 4px 0;"><strong>Coordenadas:</strong> ${hop.lat.toFixed(4)}, ${hop.lon.toFixed(4)}</p>
            </div>
        `;

        marker.bindPopup(popupContent);
        markers.push(marker);
    });

    // Draw polyline connecting all hops
    if (latLngs.length > 1) {
        L.polyline(latLngs, {
            color: '#3b82f6',
            weight: 3,
            opacity: 0.7,
            smoothFactor: 1,
            dashArray: '10, 5' // Dashed line to show direction flow
        }).addTo(tracerouteMap);
    }

    // Fit map to show all markers
    if (latLngs.length > 0) {
        const bounds = L.latLngBounds(latLngs);
        tracerouteMap.fitBounds(bounds, { padding: [50, 50] });
    }

    // Open first and last marker popups briefly to show start and end
    setTimeout(() => {
        if (markers.length > 0) {
            markers[0].openPopup();
            setTimeout(() => {
                if (markers.length > 1) {
                    markers[markers.length - 1].openPopup();
                }
            }, 2000);
        }
    }, 500);
}

// Helper function to get color gradient from green (start) to red (end)
function getHopColor(index, total) {
    const ratio = index / Math.max(total - 1, 1);
    const r = Math.round(59 + ratio * (239 - 59)); // 59 to 239 (green to red)
    const g = Math.round(130 - ratio * (130 - 68)); // 130 to 68
    const b = Math.round(246 - ratio * (246 - 68)); // 246 to 68
    return `rgb(${r}, ${g}, ${b})`;
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
