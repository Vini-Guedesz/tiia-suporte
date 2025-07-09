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
        return CompletableFuture.supplyAsync(() -> {
            String command;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                command = String.format("%s -h %d -w %d %s", tracerouteCommandWindows, maxHops, timeout, host);
            } else {
                command = String.format("%s -m %d -w %d %s", tracerouteCommandLinux, maxHops, timeout, host);
            }

            StringBuilder result = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                process.waitFor();
                logger.info("Traceroute raw output for {}:\n{}", host, result.toString());
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao executar o traceroute raw para {}: {}", host, e.getMessage(), e);
                result.append("Erro ao executar o traceroute: ").append(e.getMessage());
            }
            return result.toString();
        });
    }

    @Async
    public CompletableFuture<List<TracerouteHop>> traceroute(String host, int maxHops, int timeout) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtils.isValidIpOrHostname(host)) {
                logger.warn("Tentativa de traceroute com host inválido: {}", host);
                List<TracerouteHop> errorList = new ArrayList<>();
                TracerouteHop errorHop = new TracerouteHop();
                errorHop.setHostname("Host inválido.");
                errorList.add(errorHop);
                return errorList;
            }

            List<TracerouteHop> hops = new ArrayList<>();
            String command;

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                command = String.format("%s -h %d -w %d %s", tracerouteCommandWindows, maxHops, timeout, host);
            } else {
                command = String.format("%s -m %d -w %d %s", tracerouteCommandLinux, maxHops, timeout, host);
            }

            try {
                Process process = Runtime.getRuntime().exec(command);
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

                        try {
                            String geoData = geoService.obterLocalizacao(ipAddress);
                            JsonNode root = objectMapper.readTree(geoData);
                            if (root.has("status") && root.get("status").asText().equals("success")) {
                                hop.setCountry(root.has("country") ? root.get("country").asText() : null);
                                hop.setCity(root.has("city") ? root.get("city").asText() : null);
                                hop.setLat(root.has("lat") ? root.get("lat").asDouble() : 0.0);
                                hop.setLon(root.has("lon") ? root.get("lon").asDouble() : 0.0);
                                hop.setHostname(root.has("query") ? root.get("query").asText() : hostname);
                                logger.debug("Geolocalização para IP {} no hop {}: {}", ipAddress, hopNumber, geoData);
                            } else {
                                hop.setHostname(root.has("message") ? root.get("message").asText() : hostname);
                                logger.warn("Falha na geolocalização para IP {} no hop {}: {}", ipAddress, hopNumber, geoData);
                            }
                        } catch (InvalidIpAddressException | ExternalServiceException e) {
                            logger.error("Erro de serviço ao obter geolocalização para IP {} no hop {}: {}", ipAddress, hopNumber, e.getMessage(), e);
                            hop.setHostname("Erro ao obter geolocalização: " + e.getMessage());
                        } catch (IOException e) {
                            logger.error("Erro ao processar dados de geolocalização para IP {} no hop {}: {}", ipAddress, hopNumber, e.getMessage(), e);
                            hop.setHostname("Erro ao obter geolocalização");
                        }
                        hops.add(hop);
                    }
                }

                process.waitFor();
                logger.info("Traceroute para {} concluído com {} saltos.", host, hops.size());
            } catch (IOException | InterruptedException e) {
                logger.error("Erro ao executar o traceroute para {}: {}", host, e.getMessage(), e);
                List<TracerouteHop> errorList = new ArrayList<>();
                TracerouteHop errorHop = new TracerouteHop();
                errorHop.setHostname("Erro ao executar o traceroute: " + e.getMessage());
                errorList.add(errorHop);
                return errorList;
            }

            return hops;
        });
    }
}