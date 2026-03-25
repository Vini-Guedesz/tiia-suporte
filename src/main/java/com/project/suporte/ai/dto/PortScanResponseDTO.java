package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PortScanResponseDTO(
    @Schema(example = "scanme.nmap.org")
    @JsonProperty("host")
    String host,
    @Schema(example = "[80, 443]")
    @JsonProperty("portas_abertas")
    List<Integer> openPorts
) {}
