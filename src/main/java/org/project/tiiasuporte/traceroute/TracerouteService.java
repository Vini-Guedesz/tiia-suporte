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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.project.tiiasuporte.util.ValidationUtils;

@Service
public class TracerouteService {

    private static final Logger logger = LoggerFactory.getLogger(TracerouteService.class);

    private final GeoService geoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    }

    // New method to return raw traceroute output
    @Async
    public CompletableFuture<String> rawTraceroute(String host, int maxHops, int timeout) {
        StringBuilder result = new StringBuilder();

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (os.contains("win")) {
            // Windows: tracert -h maxHops -w timeout(ms) host
            processBuilder = new ProcessBuilder(
                tracerouteCommandWindows,
                "-h", String.valueOf(maxHops),
                "-w", String.valueOf(timeout),
                host
            );
        } else {
            // Linux: traceroute -m maxHops -w timeout(seconds) host
            int timeoutSeconds = Math.max(1, timeout / 1000);
            processBuilder = new ProcessBuilder(
                tracerouteCommandLinux,
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
            // Windows: tracert -h maxHops -w timeout(ms) host
            processBuilder = new ProcessBuilder(
                tracerouteCommandWindows,
                "-h", String.valueOf(maxHops),
                "-w", String.valueOf(timeout),
                host
            );
        } else {
            // Linux: traceroute -m maxHops -w timeout(seconds) host
            int timeoutSeconds = Math.max(1, timeout / 1000);
            processBuilder = new ProcessBuilder(
                tracerouteCommandLinux,
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
            Pattern linePattern = Pattern.compile("\\s*\\d+\\s+(?:\\S+\\s+){3}([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})|\\s*\\d+\\s+.*?\\(([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\)");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = linePattern.matcher(line);
                if (matcher.find()) {
                    hopNumber++;
                    String ipAddress = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    String hostname = "Unknown";

                    TracerouteHop hop = new TracerouteHop(hopNumber, ipAddress, hostname);
                    hops.add(hop);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Traceroute para {} concluído com código não-zero: {}", host, exitCode);
            }

            // Paralelizar chamadas de geolocalização
            if (!hops.isEmpty()) {
                List<CompletableFuture<Void>> geoFutures = new ArrayList<>();

                for (TracerouteHop hop : hops) {
                    CompletableFuture<Void> geoFuture = CompletableFuture.runAsync(() -> {
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
                    });
                    geoFutures.add(geoFuture);
                }

                // Aguarda todas as geolocalizações completarem (com timeout de 30 segundos)
                try {
                    CompletableFuture.allOf(geoFutures.toArray(new CompletableFuture[0]))
                        .get(30, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("Timeout ou erro ao aguardar geolocalizações: {}", e.getMessage());
                }
            }

            logger.info("Traceroute para {} concluído com {} saltos.", host, hops.size());
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