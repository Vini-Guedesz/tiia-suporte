# suporte.ai

`suporte.ai` e uma aplicacao Spring Boot para diagnostico de rede e apoio a times de suporte tecnico. O projeto combina uma API HTTP com streaming SSE, um painel web estatico para operacao manual e documentacao OpenAPI via Swagger.

## Visao geral

O sistema foi pensado para cenarios como:

- validar conectividade com `ping` e `traceroute`
- consultar DNS, geolocalizacao e whois
- fazer varredura controlada de portas em alvos publicos
- acompanhar links em tempo real com perda de pacote, quedas e media de latencia
- exportar logs e gerar relatorios textuais do monitoramento

## Principais recursos

- Central de acompanhamento com multiplos monitores simultaneos
- Consultas rapidas para `ping`, `traceroute`, `dns`, `geolocation`, `whois` e `portscan`
- Eventos SSE estruturados para `ping`, `traceroute` e monitoramento continuo
- Validacao de alvo para host, IP e URL
- Cache em memoria para DNS, geolocalizacao e whois
- Respostas de erro padronizadas em JSON ou SSE
- Tema claro/escuro, historico local e persistencia de monitores no navegador
- Swagger UI para exploracao manual da API

## Stack e requisitos

- Java 21
- Maven 3.9+
- Spring Boot 3.5.4
- Spring Web
- Spring Validation
- springdoc OpenAPI UI
- Commons Net para whois
- Navegador com suporte a `EventSource` para usar o painel

## Como executar

1. Instale Java 21 e Maven.
2. Na raiz do projeto, execute:

```bash
mvn spring-boot:run
```

3. Acesse a aplicacao:

- Painel web: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Se a porta `8080` estiver ocupada, altere `server.port` em `src/main/resources/application.properties` ou suba a aplicacao com outra porta:

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=8081"
```

## Como usar o painel

### Central de acompanhamento

Use a area superior do painel para monitoramento continuo. Cada monitor aceita:

- `Nome do monitor`: apelido operacional, por exemplo `Cliente Matriz`
- `Alvo`: IP, host ou URL
- `Intervalo`: frequencia entre os probes
- `Timeout`: limite maximo de espera por resposta

Ao criar um monitor, a interface passa a mostrar:

- status atual do link
- ping atual
- ping medio
- numero de quedas
- perda de pacote
- contagem de tentativas, sucessos e falhas
- log continuo do acompanhamento

Tambem e possivel:

- manter varios monitores ao mesmo tempo
- pausar e retomar um monitor
- exportar os logs em `.log`
- gerar um relatorio textual em `.txt`
- salvar os monitores no `localStorage`

### Consultas rapidas

A area inferior do painel contem ferramentas sob demanda:

- `Ping`: streaming SSE com numero configuravel de tentativas
- `Traceroute`: streaming SSE do comando do sistema
- `DNS`: resolucao de enderecos IP
- `Geolocalizacao`: geolocalizacao de IP publico ou dominio
- `Whois`: consulta de registrador, datas e status de dominio
- `Port Scan`: varredura controlada em portas especificas

O bloco `Ultimas consultas` guarda localmente as ultimas execucoes do usuario.

## Endpoints da API

| Metodo | Endpoint | Finalidade | Retorno |
| --- | --- | --- | --- |
| `GET` | `/api/v1/ping?target=8.8.8.8&count=4` | Ping com SSE | `text/event-stream` |
| `GET` | `/api/v1/ping/monitor?target=cliente.exemplo.com.br&intervalMs=5000&timeoutMs=2000` | Monitoramento continuo | `text/event-stream` |
| `GET` | `/api/v1/traceroute?target=1.1.1.1` | Traceroute com SSE | `text/event-stream` |
| `GET` | `/api/v1/dns?hostname=openai.com` | Resolucao DNS | JSON |
| `GET` | `/api/v1/geolocation?target=8.8.8.8` | Geolocalizacao de IP publico | JSON |
| `GET` | `/api/v1/whois?domain=openai.com` | Consulta whois | JSON |
| `POST` | `/api/v1/portscan` | Varredura controlada de portas | JSON |

Rotas legadas com `PathVariable` continuam ativas por compatibilidade, por exemplo:

- `/api/v1/ping/{host}`
- `/api/v1/traceroute/{host}`
- `/api/v1/dns/{hostname}`
- `/api/v1/geolocation/{ipAddress}`
- `/api/v1/whois/{domain}`

## Exemplos de uso

### Ping SSE

```bash
curl -N "http://localhost:8080/api/v1/ping?target=8.8.8.8&count=4"
```

### Traceroute SSE

```bash
curl -N "http://localhost:8080/api/v1/traceroute?target=1.1.1.1"
```

### Monitoramento continuo

```bash
curl -N "http://localhost:8080/api/v1/ping/monitor?target=8.8.8.8&intervalMs=5000&timeoutMs=2000"
```

### Port scan

```bash
curl -X POST "http://localhost:8080/api/v1/portscan" \
  -H "Content-Type: application/json" \
  -d "{\"host\":\"scanme.nmap.org\",\"ports\":[80,443],\"timeout\":800}"
```

## Contratos principais

### Evento SSE de comandos

Usado por `ping` e `traceroute`.

```json
{
  "type": "output",
  "operation": "ping",
  "target": "8.8.8.8",
  "message": "Resposta de 8.8.8.8: bytes=32 tempo=19ms TTL=119",
  "exitCode": null,
  "finished": false,
  "timestamp": "2026-03-25T22:10:00Z"
}
```

Eventos esperados:

- `started`
- `output`
- `completed`
- `error`
- `timeout`

### Evento SSE do monitoramento continuo

```json
{
  "type": "sample",
  "target": "8.8.8.8",
  "status": "ONLINE",
  "connected": true,
  "currentLatencyMs": 23.0,
  "averageLatencyMs": 27.4,
  "attempts": 15,
  "successfulAttempts": 13,
  "failedAttempts": 2,
  "outages": 1,
  "packetLossPercent": 13.33,
  "consecutiveFailures": 0,
  "message": "Ping OK em 23 ms.",
  "timestamp": "2026-03-25T22:10:05Z"
}
```

Eventos esperados:

- `started`
- `sample`
- `completed`
- `error`

### Resposta de erro JSON

```json
{
  "timestamp": "2026-03-25T19:43:41",
  "status": 400,
  "error": "Bad Request",
  "code": "validation_error",
  "message": "Os dados enviados sao invalidos.",
  "path": "/api/v1/portscan",
  "fieldErrors": {
    "ports": "Informe entre 1 e 64 portas por requisicao."
  }
}
```

Se a requisicao for SSE, o mesmo erro e devolvido como `event: error` em `text/event-stream`.

## Estrutura do projeto

| Caminho | Responsabilidade |
| --- | --- |
| `src/main/java/com/project/suporte/ai/Application.java` | Bootstrap da aplicacao |
| `src/main/java/com/project/suporte/ai/config` | Beans, executores, propriedades e OpenAPI |
| `src/main/java/com/project/suporte/ai/controller` | Endpoints HTTP e SSE |
| `src/main/java/com/project/suporte/ai/service` | Regras de negocio e orquestracao |
| `src/main/java/com/project/suporte/ai/support` | Integracoes, validadores e utilitarios de baixo nivel |
| `src/main/java/com/project/suporte/ai/dto` | Contratos de entrada e saida |
| `src/main/java/com/project/suporte/ai/exceptions` | Erros de dominio e tratamento global |
| `src/main/resources/static` | Painel web estatico |
| `src/test/java/com/project/suporte/ai` | Testes automatizados |

Para a documentacao tecnica detalhada das classes e fluxos internos, veja [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Configuracao

As propriedades principais ficam em `src/main/resources/application.properties`.

| Propriedade | Padrao | Uso |
| --- | --- | --- |
| `server.port` | `8080` | Porta HTTP da aplicacao |
| `spring.mvc.async.request-timeout` | `-1` | Timeout global de requisicoes assincronas |
| `diagnostics.sse.timeout-ms` | `180000` | Timeout padrao dos streams curtos |
| `diagnostics.async.core-pool-size` | `4` | Pool base do executor de diagnosticos |
| `diagnostics.async.max-pool-size` | `12` | Pool maximo do executor de diagnosticos |
| `diagnostics.async.queue-capacity` | `100` | Fila do executor de diagnosticos |
| `diagnostics.portscan.default-timeout-ms` | `500` | Timeout padrao do port scan |
| `diagnostics.portscan.max-timeout-ms` | `5000` | Timeout maximo aceito no port scan |
| `diagnostics.portscan.max-ports` | `64` | Numero maximo de portas por requisicao |
| `diagnostics.portscan.thread-pool-size` | `32` | Pool dedicado ao port scan |
| `diagnostics.geolocation.base-url` | `http://ip-api.com/json` | Provedor de geolocalizacao |
| `diagnostics.geolocation.connect-timeout-ms` | `2000` | Timeout de conexao HTTP para geolocalizacao |
| `diagnostics.geolocation.read-timeout-ms` | `3000` | Timeout de leitura HTTP para geolocalizacao |
| `diagnostics.cache.dns-ttl-seconds` | `300` | TTL do cache DNS |
| `diagnostics.cache.geolocation-ttl-seconds` | `300` | TTL do cache de geolocalizacao |
| `diagnostics.cache.whois-ttl-seconds` | `600` | TTL do cache whois |
| `diagnostics.whois.connect-timeout-ms` | `2000` | Timeout de conexao whois |
| `diagnostics.whois.read-timeout-ms` | `3000` | Timeout de leitura whois |

## Regras e limitacoes operacionais

- `ping` e `traceroute` dependem dos comandos disponiveis no sistema operacional.
- O monitoramento continuo depende de uma conexao SSE aberta entre navegador e servidor.
- Geolocalizacao exige IP publico ou dominio resolvivel para IP publico.
- O port scan bloqueia `localhost`, loopback, redes privadas, link-local, multicast, CGNAT e IPv6 unique local.
- Historico, tema e monitores salvos ficam no `localStorage` do navegador.
- Os logs mostrados na interface sao limitados para evitar crescimento infinito; a exportacao usa somente as linhas retidas em memoria.

## Testes

Para rodar a suite automatizada:

```bash
mvn test
```

Os testes cobrem controladores, validacao, cache, handlers, monitoramento e servicos principais sem depender do painel manual.

## Documentos relacionados

- [docs/ARQUITETURA.md](docs/ARQUITETURA.md): visao tecnica do backend, frontend e fluxos internos
- [HELP.md](HELP.md): quickstart curto para subir e acessar a aplicacao
