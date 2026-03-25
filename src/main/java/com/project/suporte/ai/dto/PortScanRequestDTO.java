package com.project.suporte.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PortScanRequestDTO(
    @Schema(example = "scanme.nmap.org")
    @NotBlank(message = "O host não pode estar em branco.")
    String host,
    @NotNull(message = "A lista de portas não pode ser nula.")
    @Size(min = 1, max = 64, message = "Informe entre 1 e 64 portas por requisição.")
    List<@Min(value = 1, message = "As portas devem estar entre 1 e 65535.")
         @Max(value = 65535, message = "As portas devem estar entre 1 e 65535.") Integer> ports,
    @Schema(example = "800")
    @Min(value = 50, message = "O timeout mínimo é 50 ms.")
    @Max(value = 5000, message = "O timeout máximo é 5000 ms.")
    Integer timeout
) {}
