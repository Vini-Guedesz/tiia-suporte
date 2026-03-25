package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record IpGeolocationResponseDTO(
    @Schema(example = "8.8.8.8")
    @JsonProperty("ip")
    String query,
    @Schema(example = "United States")
    @JsonProperty("pais")
    String country,
    @Schema(example = "California")
    @JsonProperty("regiao")
    String regionName,
    @Schema(example = "Mountain View")
    @JsonProperty("cidade")
    String city,
    @Schema(example = "Google LLC")
    @JsonProperty("provedor")
    String isp,
    @Schema(example = "Google LLC")
    @JsonProperty("organizacao")
    String org,
    @Schema(example = "America/Los_Angeles")
    @JsonProperty("fuso_horario")
    String timezone,
    @Schema(example = "37.4056")
    @JsonProperty("latitude")
    double latitude,
    @Schema(example = "-122.0775")
    @JsonProperty("longitude")
    double longitude
) {}
