package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CommandStreamEvent")
public record CommandStreamEventDTO(
        @Schema(example = "output") String type,
        @Schema(example = "ping") String operation,
        @Schema(example = "scanme.nmap.org") String target,
        @Schema(example = "Resposta de 8.8.8.8: bytes=32 tempo=19ms TTL=119") String message,
        @Schema(example = "0") Integer exitCode,
        boolean finished,
        Instant timestamp
) {
}
