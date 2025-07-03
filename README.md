# TIIA Suporte API

API de Ferramentas de Suporte Técnico de Rede. Este projeto fornece endpoints para realizar diagnósticos de rede comuns, como consulta de domínios, geolocalização de IPs e traceroute.

## Tecnologias Utilizadas

*   **Java 21**
*   **Spring Boot 3.5.3**
*   **Maven**
*   **SpringDoc OpenAPI (Swagger)** para documentação da API.
*   **Commons Net** para consultas WHOIS.

## Como Executar o Projeto

### Pré-requisitos

*   JDK 21 ou superior instalado.
*   Apache Maven instalado.

### Passos

1.  **Clone o repositório:**
    ```bash
    git clone <URL_DO_REPOSITORIO>
    cd tiia-suporte
    ```

2.  **Execute a aplicação usando o Maven Wrapper:**
    *   No Windows:
        ```bash
        ./mvnw spring-boot:run
        ```
    *   No Linux/macOS:
        ```bash
        ./mvnw spring-boot:run
        ```

A aplicação estará disponível em `http://localhost:8080`.

## Documentação da API

A API é autodocumentada usando Swagger (OpenAPI). Após iniciar a aplicação, você pode acessar a interface do Swagger para visualizar e interagir com os endpoints:

*   **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Endpoints da API

A seguir estão os endpoints disponíveis na API:

### 1. Consulta de Domínios

*   **Endpoint:** `GET /api/domain/{domainName}`
*   **Descrição:** Verifica se um domínio possui um registro de IP ativo (está publicado) e retorna informações do WHOIS. Para domínios `.br`, a consulta é direcionada ao `whois.registro.br`.
*   **Parâmetros:**
    *   `domainName` (String): O nome do domínio a ser consultado (ex: `google.com`).
*   **Exemplo de Resposta:**
    ```text
    Status: Publicado (Ativo)
    Endereço IP: 142.250.218.142

    Informações do WHOIS:
    [...dados do WHOIS...]
    ```

### 2. Geolocalização de IP

*   **Endpoint:** `GET /api/geolocalizacao/{ip}`
*   **Descrição:** Retorna dados de geolocalização para um determinado endereço IP, utilizando a API externa `ip-api.com`.
*   **Parâmetros:**
    *   `ip` (String): O endereço IP a ser localizado (ex: `8.8.8.8`).
*   **Exemplo de Resposta:**
    ```json
    {
      "query": "8.8.8.8",
      "status": "success",
      "country": "United States",
      "countryCode": "US",
      "region": "VA",
      "regionName": "Virginia",
      "city": "Ashburn",
      "zip": "20149",
      "lat": 39.0438,
      "lon": -77.4874,
      "timezone": "America/New_York",
      "isp": "Google LLC",
      "org": "Google Public DNS",
      "as": "AS15169 Google LLC"
    }
    ```

### 3. Diagnóstico de Rede (Traceroute)

*   **Endpoint:** `GET /api/traceroute/{host}`
*   **Descrição:** Executa o comando `traceroute` (`tracert` no Windows) do sistema operacional para um host (domínio ou IP) e retorna a saída.
*   **Parâmetros:**
    *   `host` (String): O host para o qual o traceroute será executado (ex: `google.com`).
*   **Exemplo de Resposta:**
    ```text
    Rastreando a rota para google.com [142.250.218.142]
    ...
      1    <1 ms    <1 ms    <1 ms  192.168.1.1
      2     1 ms     1 ms     1 ms  [...gateway...]
    ...
    Rastreamento concluído.
    ```
