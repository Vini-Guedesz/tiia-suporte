package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DnsLookupResponseDTO(
    @Schema(example = "openai.com")
    @JsonProperty("hostname")
    String hostname,
    @Schema(example = "[\"172.64.154.211\", \"104.18.33.45\"]")
    @JsonProperty("enderecos_ip")
    List<String> ipAddresses
) {}
