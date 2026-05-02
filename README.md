# TIIA Suporte (Java + Spring Boot)

TIIA Suporte e uma aplicacao de diagnostico de rede com API Spring Boot e painel web leve para operacao tecnica.

## Funcionalidades

- Diagnostico de rede em tempo real com SSE:
  - stream de ping
  - stream de traceroute
  - monitor continuo com latencia e perda de pacote
- Endpoints utilitarios:
  - consulta DNS
  - geolocalizacao de IP
  - whois de dominio
  - port scan controlado
- Painel operacional para uso manual
- Documentacao OpenAPI com Swagger UI

## Stack

- Java 21
- Spring Boot
- Spring Web + Validation
- springdoc OpenAPI
- Maven

## Instalacao

```bash
# Clonar repositorio
git clone https://github.com/Vini-Guedesz/tiia-suporte.git

# Entrar no projeto
cd tiia-suporte

# Subir aplicacao
mvn spring-boot:run
```

A aplicacao ficara disponivel em:

- Painel: `http://localhost:8080/`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Estrutura do projeto

```text
src/
 ├── main/
 │    ├── java/com/project/suporte/ai/
 │    │    ├── controller/      # Endpoints HTTP e SSE
 │    │    ├── service/         # Regras de negocio
 │    │    ├── dto/             # Contratos de entrada e saida
 │    │    ├── support/         # Integracoes e utilitarios
 │    │    └── config/          # Configuracoes da aplicacao
 │    └── resources/
 │         └── static/          # Painel web
 └── test/
```

## Roadmap

- [ ] Adicionar modo de autenticacao para ambientes restritos
- [ ] Expandir testes de carga dos fluxos SSE
- [ ] Adicionar dashboards operacionais mais ricos
- [ ] Adicionar gates de qualidade CI para contrato de API
