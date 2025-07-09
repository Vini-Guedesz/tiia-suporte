package org.project.tiiasuporte.domain;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/domain")
@Tag(name = "Consulta de Domínios", description = "Endpoints para obter informações de domínios")
public class DomainController {

    private final DomainService domainService;

    @Autowired
    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @Operation(
        summary = "Obtém informações sobre um domínio",
        description = "Verifica se um domínio está publicado (possui um IP ativo) e consulta os dados do WHOIS para obter informações sobre o proprietário. Para domínios .br, a consulta é direcionada ao whois.registro.br.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Informações do domínio retornadas com sucesso.",
                content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(value = "Status: Publicado (Ativo)\nEndereço IP: 172.67.141.24\n\nInformações do WHOIS:\n[...dados do WHOIS...]\n")
                )
            )
        }
    )
    @GetMapping("/{domainName}")
    public String getDomainInfo(
        @Parameter(
            description = "O nome do domínio a ser consultado.",
            example = "google.com"
        ) 
        @PathVariable String domainName) {
        return domainService.getDomainInfo(domainName);
    }
}
