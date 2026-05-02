# TIIA Suporte

API e painel web para diagnostico de rede e apoio a times de suporte tecnico.

## Status

- Ativo
- Versao atual: backend Spring Boot com API HTTP + SSE

## Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Validation
- springdoc OpenAPI (Swagger)
- Maven

## Funcionalidades

- Ping e traceroute com streaming SSE
- Monitoramento continuo de alvo com metricas de latencia e perda
- DNS lookup
- Geolocalizacao de IP publico
- Whois de dominio
- Port scan controlado
- Painel web estatico para operacao manual

## Como executar

### Local

```bash
mvn spring-boot:run
```

Acessos:

- Painel: `http://localhost:8080/`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

### Porta alternativa

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8081"
```

## Endpoints principais

- `GET /api/v1/ping?target=8.8.8.8&count=4`
- `GET /api/v1/ping/monitor?target=example.com&intervalMs=5000&timeoutMs=2000`
- `GET /api/v1/traceroute?target=1.1.1.1`
- `GET /api/v1/dns?hostname=openai.com`
- `GET /api/v1/geolocation?target=8.8.8.8`
- `GET /api/v1/whois?domain=openai.com`
- `POST /api/v1/portscan`

## Estrutura

- `src/main/java/.../controller`: endpoints
- `src/main/java/.../service`: regras de negocio
- `src/main/java/.../dto`: contratos de entrada e saida
- `src/main/resources/static`: painel web
- `docs/ARQUITETURA.md`: visao tecnica detalhada

## Testes

```bash
mvn test
```

## Roadmap

- Expandir testes de carga para fluxos SSE
- Adicionar autenticacao opcional para operacao restrita
- Publicar dashboard com metricas operacionais

## Autor

Desenvolvido por [Vinicius Guedes](https://github.com/Vini-Guedesz).
