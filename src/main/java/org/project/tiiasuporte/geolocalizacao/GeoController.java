package org.project.tiiasuporte.geolocalizacao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/geolocalizacao")
@Tag(name = "Geolocalização", description = "Endpoints para obter dados de geolocalização de IPs")
public class GeoController {

    @Operation(
        summary = "Obtém a geolocalização de um endereço IP",
        description = "Consulta a API externa ip-api.com para retornar informações detalhadas de geolocalização de um endereço IP público.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Dados de geolocalização retornados com sucesso.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(value = "{\n  \"query\": \"8.8.8.8\",\n  \"status\": \"success\",\n  \"country\": \"United States\",\n  \"countryCode\": \"US\",\n  \"region\": \"VA\",\n  \"regionName\": \"Virginia\",\n  \"city\": \"Ashburn\",\n  \"zip\": \"20149\",\n  \"lat\": 39.0438,\n  \"lon\": -77.4874,\n  \"timezone\": \"America/New_York\",\n  \"isp\": \"Google LLC\",\n  \"org\": \"Google Public DNS\",\n  \"as\": \"AS15169 Google LLC\"}")
                )
            )
        }
    )
    @GetMapping("/{ip}")
    public String obterLocalizacao(
        @Parameter(
            description = "O endereço IP a ser localizado.",
            example = "8.8.8.8"
        ) 
        @PathVariable String ip) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://ip-api.com/json/" + ip;
        return restTemplate.getForObject(url, String.class);
    }
}
