package com.project.suporte.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PingMonitorEvent")
public record PingMonitorEventDTO(
        @Schema(example = "sample") String type,
        @Schema(example = "8.8.8.8") String target,
        @Schema(example = "ONLINE") String status,
        boolean connected,
        @Schema(example = "23.0") Double currentLatencyMs,
        @Schema(example = "27.4") Double averageLatencyMs,
        @Schema(example = "15") long attempts,
        @Schema(example = "13") long successfulAttempts,
        @Schema(example = "2") long failedAttempts,
        @Schema(example = "1") long outages,
        @Schema(example = "13.33") double packetLossPercent,
        @Schema(example = "0") int consecutiveFailures,
        @Schema(example = "Ping OK em 23 ms.") String message,
        Instant timestamp
) {
}
