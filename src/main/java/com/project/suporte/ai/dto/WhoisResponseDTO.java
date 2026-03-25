package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record WhoisResponseDTO(
    @Schema(example = "openai.com")
    @JsonProperty("dominio")
    String dominio,
    @Schema(example = "MarkMonitor Inc.")
    @JsonProperty("registrador")
    String registrador,
    @JsonProperty("data_criacao")
    String dataCriacao,
    @JsonProperty("data_expiracao")
    String dataExpiracao,
    @JsonProperty("servidores_nome")
    String servidoresNome,
    @JsonProperty("status_dominio")
    String statusDominio
) {}
