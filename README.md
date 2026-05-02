# TIIA Suporte (Java + Spring Boot)

TIIA Suporte is a network diagnostics application with a Spring Boot API and a lightweight web panel. It provides operational tools for support teams, including streaming diagnostics and continuous target monitoring.

## ✨ Features

- 📡 Real-time network diagnostics (SSE):
  - Ping stream
  - Traceroute stream
  - Continuous ping monitor with latency and packet loss metrics
- 🌐 Utility endpoints:
  - DNS lookup
  - IP geolocation
  - Whois lookup
  - Controlled port scan
- 🧭 Operational web panel for manual usage
- 📘 OpenAPI + Swagger UI documentation

## 🛠️ Tech Stack

- Java 21
- Spring Boot
- Spring Web + Validation
- springdoc OpenAPI
- Maven

## 📦 Installation

```bash
# Clone repository
git clone https://github.com/Vini-Guedesz/tiia-suporte.git

# Enter project folder
cd tiia-suporte

# Run application
mvn spring-boot:run
```

The app will be available at:

- Panel: `http://localhost:8080/`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 🧩 Project Structure

```text
src/
 ├── main/
 │    ├── java/com/project/suporte/ai/
 │    │    ├── controller/      # HTTP and SSE endpoints
 │    │    ├── service/         # Business rules
 │    │    ├── dto/             # Request/response contracts
 │    │    ├── support/         # Helpers and integrations
 │    │    └── config/          # App configuration
 │    └── resources/
 │         └── static/          # Web panel
 └── test/
```

## 📌 Roadmap

- [ ] Add authentication mode for restricted environments
- [ ] Expand stress/load tests for SSE flows
- [ ] Add richer operational dashboards and uptime reports
- [ ] Add CI quality gates for API contract checks
