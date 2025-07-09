package org.project.tiiasuporte;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(
    info = @Info(
        title = "TIIA Suporte API",
        version = "1.0",
        description = "API de Ferramentas de Suporte Técnico de Rede"
    )
)
public class TiiaSuporteApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiiaSuporteApplication.class, args);
    }

}
