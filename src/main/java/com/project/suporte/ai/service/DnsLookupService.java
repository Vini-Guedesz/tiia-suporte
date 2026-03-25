package com.project.suporte.ai.service;

import com.project.suporte.ai.config.DiagnosticsProperties;
import com.project.suporte.ai.dto.DnsLookupResponseDTO;
import com.project.suporte.ai.support.ExpiringCache;
import com.project.suporte.ai.support.TargetValidator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DnsLookupService {

    private final TargetValidator targetValidator;
    private final ExpiringCache<String, DnsLookupResponseDTO> cache;

    public DnsLookupService(TargetValidator targetValidator, DiagnosticsProperties properties) {
        this.targetValidator = targetValidator;
        this.cache = new ExpiringCache<>(Duration.ofSeconds(properties.getCache().getDnsTtlSeconds()));
    }

    public DnsLookupResponseDTO lookup(String hostname) {
        String target = targetValidator.normalizeTarget(hostname);
        return cache.get(target, () -> {
            List<String> ipAddresses = targetValidator.resolveAddresses(target).stream()
                    .map(address -> address.getHostAddress())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            return new DnsLookupResponseDTO(target, ipAddresses);
        });
    }
}
