# Arquitetura do suporte.ai

Este documento descreve como o sistema esta organizado internamente, quais sao as responsabilidades de cada camada e onde cada funcionalidade vive no codigo.

## Visao geral da arquitetura

O projeto segue uma divisao simples:

1. `controller`: recebe requisicoes HTTP e SSE
2. `service`: concentra regras de negocio e orquestracao
3. `support`: abstrai infraestrutura, validacao e acesso a recursos do sistema
4. `dto`: define contratos de entrada e saida
5. `exceptions`: padroniza os erros da API
6. `static`: entrega o painel operacional no navegador

No fluxo normal, a requisicao entra no controller, passa por validacao e chamadas aos services, que por sua vez usam classes de `support` para resolver DNS, abrir sockets, executar processos do sistema ou consultar servicos externos.

## Fluxos principais

### Ping e traceroute

1. O browser ou cliente HTTP abre um endpoint SSE.
2. `PingController` ou `TracerouteController` cria um `SseEmitter`.
3. `PingService` ou `TracerouteService` monta o comando do sistema operacional.
4. `CommandStreamingService` inicia o processo, le a saida linha a linha e publica eventos SSE.
5. Ao terminar, o stream envia `completed`, `error` ou `timeout`.

### Monitoramento continuo

1. `PingController` recebe `target`, `intervalMs` e `timeoutMs`.
2. `SseEmitterFactory.createContinuous()` abre um emitter sem timeout local.
3. `PingMonitorService` roda de forma assincrona no executor `diagnosticsExecutor`.
4. Cada iteracao executa um probe de ping unico.
5. `PingMonitorAccumulator` consolida tentativas, media, quedas e perda de pacote.
6. O backend envia eventos `sample` enquanto a conexao permanecer aberta.

### Consultas rapidas

- `DnsLookupService` resolve o alvo e aplica cache em memoria.
- `IpGeolocationService` resolve um IP publico e consulta o provedor HTTP configurado.
- `WhoisService` consulta whois por meio de `WhoisGateway` e aplica cache.
- `PortScanService` valida o alvo, usa `PortProbe` e varre somente as portas solicitadas.

### Frontend do painel

1. `index.html` define a estrutura principal da tela.
2. `app.js` orquestra formularios, fetch, EventSource, historico e monitores.
3. `app.css` define o layout, os temas e o comportamento visual.
4. O estado local do navegador guarda historico, tema e monitores persistidos.

## Catalogo de classes

### Bootstrap

- `src/main/java/com/project/suporte/ai/Application.java`
  Inicializa o Spring Boot e habilita execucao assincrona com `@EnableAsync`.

### Configuracao

- `src/main/java/com/project/suporte/ai/config/AppConfig.java`
  Registra o executor dedicado ao port scan.
- `src/main/java/com/project/suporte/ai/config/AsyncConfig.java`
  Registra o executor `diagnosticsExecutor` para tarefas assincronas como ping, traceroute e monitoramento.
- `src/main/java/com/project/suporte/ai/config/DiagnosticsProperties.java`
  Mapeia todas as propriedades `diagnostics.*` e valida seus limites.
- `src/main/java/com/project/suporte/ai/config/OpenApiConfig.java`
  Personaliza titulo, descricao e licenca da OpenAPI.
- `src/main/java/com/project/suporte/ai/config/RestTemplateConfig.java`
  Cria o `RestTemplate` com timeouts para geolocalizacao.

### Controllers

- `src/main/java/com/project/suporte/ai/controller/PingController.java`
  Expoe o ping SSE tradicional e o monitoramento continuo.
- `src/main/java/com/project/suporte/ai/controller/TracerouteController.java`
  Expoe o traceroute SSE.
- `src/main/java/com/project/suporte/ai/controller/DnsLookupController.java`
  Expoe a consulta DNS.
- `src/main/java/com/project/suporte/ai/controller/IpGeolocationController.java`
  Expoe a consulta de geolocalizacao.
- `src/main/java/com/project/suporte/ai/controller/WhoisController.java`
  Expoe a consulta whois.
- `src/main/java/com/project/suporte/ai/controller/PortScanController.java`
  Expoe a varredura de portas.
- `src/main/java/com/project/suporte/ai/controller/FaviconController.java`
  Responde `204 No Content` para `favicon.ico` e evita ruido nos logs.

### Services

- `src/main/java/com/project/suporte/ai/service/PingService.java`
  Normaliza o alvo, monta o comando `ping` e delega o streaming ao `CommandStreamingService`.
- `src/main/java/com/project/suporte/ai/service/TracerouteService.java`
  Normaliza o alvo, monta o comando `traceroute` ou equivalente no sistema e delega o streaming.
- `src/main/java/com/project/suporte/ai/service/CommandStreamingService.java`
  Executa um processo, acompanha stdout e transforma a execucao em eventos SSE estruturados.
- `src/main/java/com/project/suporte/ai/service/PingMonitorService.java`
  Executa probes continuos de ping e publica amostras consolidadas.
- `src/main/java/com/project/suporte/ai/service/PingMonitorAccumulator.java`
  Mantem o estado agregado do monitoramento: media, quedas, tentativas, falhas e perda.
- `src/main/java/com/project/suporte/ai/service/PingProbeResult.java`
  Record interno com o resultado de um probe unico.
- `src/main/java/com/project/suporte/ai/service/DnsLookupService.java`
  Resolve nomes e IPs, aplicando cache em memoria.
- `src/main/java/com/project/suporte/ai/service/IpGeolocationService.java`
  Resolve um IP publico e consulta o provedor externo configurado.
- `src/main/java/com/project/suporte/ai/service/WhoisService.java`
  Consulta dados whois, faz parsing dos campos relevantes e aplica cache.
- `src/main/java/com/project/suporte/ai/service/PortScanService.java`
  Valida alvo e portas, executa a sondagem de portas e retorna somente as abertas.

### Support

- `src/main/java/com/project/suporte/ai/support/TargetValidator.java`
  Normaliza host, IP ou URL, resolve enderecos e aplica regras de seguranca.
- `src/main/java/com/project/suporte/ai/support/SseEmitterFactory.java`
  Centraliza a criacao de emitters SSE curtos e continuos.
- `src/main/java/com/project/suporte/ai/support/ExpiringCache.java`
  Implementa cache em memoria com TTL por chave.
- `src/main/java/com/project/suporte/ai/support/ProcessLauncher.java`
  Interface para iniciar comandos do sistema.
- `src/main/java/com/project/suporte/ai/support/OperatingSystemProcessLauncher.java`
  Implementacao real de `ProcessLauncher` usando `ProcessBuilder`.
- `src/main/java/com/project/suporte/ai/support/AddressResolver.java`
  Interface para resolucao DNS.
- `src/main/java/com/project/suporte/ai/support/InetAddressResolver.java`
  Implementacao real de `AddressResolver` com `InetAddress`.
- `src/main/java/com/project/suporte/ai/support/PortProbe.java`
  Interface para teste de conectividade em porta TCP.
- `src/main/java/com/project/suporte/ai/support/SocketPortProbe.java`
  Implementacao de `PortProbe` usando `Socket`.
- `src/main/java/com/project/suporte/ai/support/WhoisGateway.java`
  Interface de baixo nivel para consulta whois.
- `src/main/java/com/project/suporte/ai/support/CommonsNetWhoisGateway.java`
  Implementacao de `WhoisGateway` baseada em Apache Commons Net.

### DTOs

- `src/main/java/com/project/suporte/ai/dto/CommandStreamEventDTO.java`
  Payload SSE para ping e traceroute.
- `src/main/java/com/project/suporte/ai/dto/PingMonitorEventDTO.java`
  Payload SSE do monitoramento continuo.
- `src/main/java/com/project/suporte/ai/dto/DnsLookupResponseDTO.java`
  Resposta JSON da consulta DNS.
- `src/main/java/com/project/suporte/ai/dto/IpGeolocationDTO.java`
  Contrato bruto recebido do provedor de geolocalizacao.
- `src/main/java/com/project/suporte/ai/dto/IpGeolocationResponseDTO.java`
  Resposta publica da API de geolocalizacao.
- `src/main/java/com/project/suporte/ai/dto/WhoisResponseDTO.java`
  Resposta publica da API de whois.
- `src/main/java/com/project/suporte/ai/dto/PortScanRequestDTO.java`
  Payload de entrada do port scan com validacoes Bean Validation.
- `src/main/java/com/project/suporte/ai/dto/PortScanResponseDTO.java`
  Resposta publica do port scan.

### Exceptions

- `src/main/java/com/project/suporte/ai/exceptions/ApiException.java`
  Excecao de dominio com `status`, `code` e mensagem.
- `src/main/java/com/project/suporte/ai/exceptions/ErrorResponse.java`
  Payload padrao de erro HTTP.
- `src/main/java/com/project/suporte/ai/exceptions/GlobalExceptionHandler.java`
  Traduz excecoes para JSON ou SSE, conforme o tipo de requisicao.

### Frontend

- `src/main/resources/static/index.html`
  Estrutura da interface: hero, historico, central de acompanhamento e ferramentas rapidas.
- `src/main/resources/static/app.css`
  Tema, responsividade, layout do dashboard e estilos dos estados do monitor.
- `src/main/resources/static/app.js`
  Logica completa do frontend: formularios, fetch, SSE, persistencia local, historico, filtros, exportacao e relatorio.

## Estado mantido no navegador

O frontend usa tres chaves principais em `localStorage`:

- `suporte-ai-history`: ultimas consultas executadas
- `suporte-ai-theme`: preferencia de tema
- `suporte-ai-monitors`: lista de monitores salvos e ultimo monitor selecionado

Os logs do acompanhamento nao sao persistidos no `localStorage`; apenas o estado resumido do monitor e salvo.

## Decisoes operacionais importantes

- Streams curtos usam timeout configuravel; monitoramento continuo usa emitter sem timeout local.
- O timeout global MVC assincrono esta desabilitado para nao matar o monitoramento continuo.
- Port scan e explicitamente limitado a alvos publicos para reduzir risco operacional.
- Erros SSE nao retornam JSON puro; eles saem como `event: error` em `text/event-stream`.
- DNS, geolocalizacao e whois usam cache curto em memoria para aliviar repeticoes.

## Pontos de extensao

Se quiser evoluir o sistema, os melhores pontos sao:

- trocar o provedor de geolocalizacao em `IpGeolocationService`
- adicionar novos diagnosticos criando um controller e um service dedicados
- substituir implementacoes de `ProcessLauncher`, `PortProbe`, `AddressResolver` ou `WhoisGateway`
- adicionar autenticacao e rate limiting na borda HTTP
- instrumentar metrics e tracing sobre os services

## Relacao com o README

Use [README.md](../README.md) como ponto de entrada para setup, execucao e exemplos de uso. Este documento existe para explicar a estrutura interna e facilitar manutencao do codigo.
