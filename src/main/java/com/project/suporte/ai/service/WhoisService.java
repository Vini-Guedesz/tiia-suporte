package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.WhoisResponseDTO;
import com.project.suporte.ai.exceptions.ApiException;
import com.project.suporte.ai.support.ExpiringCache;
import com.project.suporte.ai.support.TargetValidator;
import com.project.suporte.ai.support.WhoisGateway;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhoisService {

    private final TargetValidator targetValidator;
    private final WhoisGateway whoisGateway;
    private final ExpiringCache<String, WhoisResponseDTO> cache;

    public WhoisService(TargetValidator targetValidator, WhoisGateway whoisGateway, DiagnosticsProperties properties) {
        this.targetValidator = targetValidator;
        this.whoisGateway = whoisGateway;
        this.cache = new ExpiringCache<>(Duration.ofSeconds(properties.getCache().getWhoisTtlSeconds()));
    }

    public WhoisResponseDTO lookup(String domain) {
        String normalizedDomain = targetValidator.normalizeDomain(domain);
        return cache.get(normalizedDomain, () -> executeLookup(normalizedDomain));
    }

    private WhoisResponseDTO executeLookup(String domain) {
        String whoisServer = resolveWhoisServer(domain);
        try {
            String whoisData = whoisGateway.query(whoisServer, domain);

            if (whoisData == null || whoisData.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "whois_unavailable", "O servidor whois retornou uma resposta vazia.");
            }

            if (containsNotFound(whoisData)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "domain_not_found", "Domínio inválido ou não encontrado.");
            }

            return parseWhoisData(domain, whoisData);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "whois_failed", "Erro ao consultar o servidor whois.", exception);
        }
    }

    private String resolveWhoisServer(String domain) {
        String tld = domain.substring(domain.lastIndexOf(".") + 1);
        switch (tld) {
            case "com":
            case "net":
                return "whois.verisign-grs.com";
            case "org":
                return "whois.pir.org";
            case "br":
                return "whois.registro.br";
            default:
                return discoverReferralServer(tld);
        }
    }

    private String discoverReferralServer(String tld) {
        try {
            String response = whoisGateway.query("whois.iana.org", tld);
            String referral = extractFirstMatch(response, "refer:\\s*(.*)");
            if (referral == null || referral.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "unsupported_tld", "Não foi possível determinar o servidor whois para o TLD informado.");
            }
            return referral.trim();
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "whois_referral_failed", "Não foi possível descobrir o servidor whois responsável pelo TLD informado.", exception);
        }
    }

    private boolean containsNotFound(String whoisData) {
        String normalized = whoisData.toUpperCase();
        return normalized.contains("NO MATCH FOR")
                || normalized.contains("NOT FOUND")
                || normalized.contains("NO ENTRIES FOUND")
                || normalized.contains("DOMAIN NOT FOUND");
    }

    private WhoisResponseDTO parseWhoisData(String domain, String whoisData) {
        String registrador = "N/A";
        String dataCriacao = "N/A";
        String dataExpiracao = "N/A";
        String servidoresNome = "N/A";
        String statusDominio = "N/A";

        if (domain.endsWith(".br")) {
            registrador = firstNonBlank(
                    extractFirstMatch(whoisData, "owner:\\s*(.*)"),
                    extractFirstMatch(whoisData, "nic-hdl-br:\\s*(.*)")
            );
            dataCriacao = extractFirstMatch(whoisData, "created:\\s*(.*)");
            dataExpiracao = extractFirstMatch(whoisData, "expires:\\s*(.*)");
            servidoresNome = joinOrDefault(extractAllValues(whoisData, "nserver:\\s*(.*)"));
            statusDominio = joinOrDefault(extractAllValues(whoisData, "status:\\s*(.*)"));
        } else {
            registrador = firstNonBlank(
                    extractFirstMatch(whoisData, "Registrar:\\s*(.*)"),
                    extractFirstMatch(whoisData, "Sponsoring Registrar:\\s*(.*)")
            );
            dataCriacao = firstNonBlank(
                    extractFirstMatch(whoisData, "Creation Date:\\s*(.*)"),
                    extractFirstMatch(whoisData, "Created On:\\s*(.*)")
            );
            dataExpiracao = firstNonBlank(
                    extractFirstMatch(whoisData, "Registry Expiry Date:\\s*(.*)"),
                    extractFirstMatch(whoisData, "Registrar Registration Expiration Date:\\s*(.*)"),
                    extractFirstMatch(whoisData, "Expires On:\\s*(.*)")
            );
            servidoresNome = joinOrDefault(extractAllValues(whoisData, "Name Server:\\s*(.*)"));
            statusDominio = joinOrDefault(extractAllValues(whoisData, "Domain Status:\\s*(.*)"));
        }

        return new WhoisResponseDTO(
                domain,
                registrador != null ? registrador.trim() : "N/A",
                dataCriacao != null ? dataCriacao.trim() : "N/A",
                dataExpiracao != null ? dataExpiracao.trim() : "N/A",
                servidoresNome,
                statusDominio
        );
    }

    private String extractFirstMatch(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private List<String> extractAllValues(String text, String regex) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(matcher.group(1).trim());
        }
        return values;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "N/A";
    }

    private String joinOrDefault(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "N/A";
        }
        return String.join(", ", values);
    }
}
