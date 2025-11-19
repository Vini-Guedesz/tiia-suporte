package org.project.tiiasuporte.traceroute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.project.tiiasuporte.geolocalizacao.GeoService;
import org.project.tiiasuporte.exceptions.InvalidIpAddressException;
import org.project.tiiasuporte.exceptions.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.project.tiiasuporte.util.ValidationUtils;

@Service
public class TracerouteService {

    private static final Logger logger = LoggerFactory.getLogger(TracerouteService.class);

    // Pre-compile regex patterns for better performance
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "\\s*\\d+\\s+(?:\\S+\\s+){3}([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})|" +
        "\\s*\\d+\\s+.*?\\(([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\)|" +
        "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile("([0-9a-fA-F:]+:[0-9a-fA-F:]+)");
    private static final Pattern ASTERISK_LINE_PATTERN = Pattern.compile("\\s*\\d+\\s+\\*\\s+\\*\\s+\\*.*");
    private static final Pattern HOP_NUMBER_PATTERN = Pattern.compile("^\\d+\\s+.*");

    private final GeoService geoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService geoExecutor;

    @Value("${traceroute.command.windows}")
    private String tracerouteCommandWindows;

    @Value("${traceroute.command.linux}")
    private String tracerouteCommandLinux;

    @Value("${traceroute.default.maxHops}")
    private int defaultMaxHops;

    @Value("${traceroute.default.timeout}")
    private int defaultTimeout;

    @Autowired
    public TracerouteService(GeoService geoService) {
        this.geoService = geoService;
        this.geoExecutor = Executors.newFixedThreadPool(20, r -> {
            Thread t = new Thread(r);
            t.setName("traceroute-geo-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void cleanup() {
        if (geoExecutor != null) {
            geoExecutor.shutdownNow();
            logger.info("Traceroute ExecutorService encerrado");
        }
    }

    // New method to return raw traceroute output
    @Async
    public CompletableFuture<String> rawTraceroute(String host, int maxHops, int timeout) {
        StringBuilder result = new StringBuilder();

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            // Windows: tracert -4 (force IPv4) -h maxHops -w timeout(ms) host
            processBuilder = new ProcessBuilder(
                tracerouteCommandWindows,
                "-4",  // Force IPv4
                "-h", String.valueOf(maxHops),
                "-w", String.valueOf(timeout),
                host
            );
        } else {
            // Linux: traceroute -4 (force IPv4) -m maxHops -w timeout(seconds) host
            int timeoutSeconds = Math.max(1, timeout / 1000);
            processBuilder = new ProcessBuilder(
                tracerouteCommandLinux,
                "-4",  // Force IPv4
                "-m", String.valueOf(maxHops),
                "-w", String.valueOf(timeoutSeconds),
                host
            );
        }

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            // Captura erros se houver
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && errors.length() > 0) {
                result.append("\nErros:\n").append(errors);
            }

            logger.info("Traceroute raw para {} concluído com código {}", host, exitCode);
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao executar o traceroute raw para {}: {}", host, e.getMessage(), e);
            result.append("Erro ao executar o traceroute: ").append(e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return CompletableFuture.completedFuture(result.toString());
    }

    @Async
    public CompletableFuture<List<TracerouteHop>> traceroute(String host, int maxHops, int timeout) {
        long startTime = System.currentTimeMillis();

        if (!ValidationUtils.isValidIpOrHostname(host)) {
            logger.warn("Tentativa de traceroute com host inválido: {}", host);
            List<TracerouteHop> errorList = new ArrayList<>();
            TracerouteHop errorHop = new TracerouteHop();
            errorHop.setHostname("Host inválido.");
            errorList.add(errorHop);
            return CompletableFuture.completedFuture(errorList);
        }

        List<TracerouteHop> hops = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            // Windows: tracert -4 (force IPv4) -h maxHops -w timeout(ms) host
            processBuilder = new ProcessBuilder(
                tracerouteCommandWindows,
                "-4",  // Force IPv4
                "-h", String.valueOf(maxHops),
                "-w", String.valueOf(timeout),
                host
            );
        } else {
            // Linux: traceroute -4 (force IPv4) -m maxHops -w timeout(seconds) host
            int timeoutSeconds = Math.max(1, timeout / 1000);
            processBuilder = new ProcessBuilder(
                tracerouteCommandLinux,
                "-4",  // Force IPv4
                "-m", String.valueOf(maxHops),
                "-w", String.valueOf(timeoutSeconds),
                host
            );
        }

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            int hopNumber = 0;

            while ((line = reader.readLine()) != null) {
                // Skip lines that are just asterisks or empty (using pre-compiled pattern)
                if (line.trim().isEmpty() || ASTERISK_LINE_PATTERN.matcher(line).matches()) {
                    continue;
                }

                String ipAddress = null;

                // Try to find IPv4 address first (using pre-compiled pattern)
                Matcher ipv4Matcher = IPV4_PATTERN.matcher(line);
                if (ipv4Matcher.find()) {
                    ipAddress = ipv4Matcher.group(1) != null ? ipv4Matcher.group(1) :
                               (ipv4Matcher.group(2) != null ? ipv4Matcher.group(2) : ipv4Matcher.group(3));
                }

                // If no IPv4 found, try IPv6 (using pre-compiled pattern)
                if (ipAddress == null) {
                    Matcher ipv6Matcher = IPV6_PATTERN.matcher(line);
                    if (ipv6Matcher.find()) {
                        ipAddress = ipv6Matcher.group(1);
                    }
                }

                // If we found an IP address and the line starts with a number (using pre-compiled pattern)
                if (ipAddress != null && HOP_NUMBER_PATTERN.matcher(line.trim()).matches()) {
                    hopNumber++;
                    String hostname = "Unknown";

                    TracerouteHop hop = new TracerouteHop(hopNumber, ipAddress, hostname);
                    hops.add(hop);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Traceroute para {} concluído com código não-zero: {}", host, exitCode);
            }

            // Paralelizar chamadas de geolocalização com executor dedicado
            if (!hops.isEmpty()) {
                List<CompletableFuture<Void>> geoFutures = new ArrayList<>(hops.size());

                for (TracerouteHop hop : hops) {
                    CompletableFuture<Void> geoFuture = CompletableFuture.runAsync(() -> {
                        // Only try geolocation for IPv4 addresses (ip-api.com doesn't support IPv6)
                        if (!ValidationUtils.isValidIpv4(hop.getIpAddress())) {
                            hop.setHostname("IPv6 - geolocation not supported");
                            logger.debug("Skipping geolocation for IPv6 address: {}", hop.getIpAddress());
                            return;
                        }

                        try {
                            String geoData = geoService.obterLocalizacao(hop.getIpAddress());
                            JsonNode root = objectMapper.readTree(geoData);
                            if (root.has("status") && root.get("status").asText().equals("success")) {
                                hop.setCountry(root.has("country") ? root.get("country").asText() : null);
                                hop.setCity(root.has("city") ? root.get("city").asText() : null);
                                hop.setLat(root.has("lat") ? root.get("lat").asDouble() : 0.0);
                                hop.setLon(root.has("lon") ? root.get("lon").asDouble() : 0.0);
                                hop.setHostname(root.has("query") ? root.get("query").asText() : "Unknown");
                                logger.debug("Geolocalização para IP {} no hop {}: sucesso", hop.getIpAddress(), hop.getHopNumber());
                            } else {
                                hop.setHostname(root.has("message") ? root.get("message").asText() : "Unknown");
                                logger.warn("Falha na geolocalização para IP {} no hop {}", hop.getIpAddress(), hop.getHopNumber());
                            }
                        } catch (InvalidIpAddressException | ExternalServiceException e) {
                            logger.error("Erro de serviço ao obter geolocalização para IP {} no hop {}: {}",
                                hop.getIpAddress(), hop.getHopNumber(), e.getMessage());
                            hop.setHostname("Erro ao obter geolocalização");
                        } catch (IOException e) {
                            logger.error("Erro ao processar dados de geolocalização para IP {} no hop {}: {}",
                                hop.getIpAddress(), hop.getHopNumber(), e.getMessage());
                            hop.setHostname("Erro ao processar geolocalização");
                        }
                    }, geoExecutor); // Use dedicated executor
                    geoFutures.add(geoFuture);
                }

                // Aguarda todas as geolocalizações completarem (com timeout de 30 segundos)
                try {
                    CompletableFuture.allOf(geoFutures.toArray(new CompletableFuture[0]))
                        .get(30, java.util.concurrent.TimeUnit.SECONDS);
                    logger.info("Geolocalização completada para {} hops", hops.size());
                } catch (TimeoutException e) {
                    logger.warn("Timeout ao aguardar geolocalizações para {} após 30s", host);
                } catch (Exception e) {
                    logger.error("Erro ao aguardar geolocalizações: {}", e.getMessage());
                }
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Traceroute para {} concluído em {}ms com {} saltos", host, totalDuration, hops.size());
        } catch (IOException | InterruptedException e) {
            logger.error("Erro ao executar o traceroute para {}: {}", host, e.getMessage(), e);
            List<TracerouteHop> errorList = new ArrayList<>();
            TracerouteHop errorHop = new TracerouteHop();
            errorHop.setHostname("Erro ao executar o traceroute: " + e.getMessage());
            errorList.add(errorHop);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return CompletableFuture.completedFuture(errorList);
        }

        return CompletableFuture.completedFuture(hops);
    }
}