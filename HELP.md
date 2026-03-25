# Quickstart

Use este arquivo como referencia curta. A documentacao principal do projeto esta em [README.md](README.md).

## Subir a aplicacao

```bash
mvn spring-boot:run
```

## Acessos locais

- Painel web: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Testes

```bash
mvn test
```

## Quando consultar cada documento

- [README.md](README.md): visao geral, setup, endpoints, exemplos e configuracao
- [docs/ARQUITETURA.md](docs/ARQUITETURA.md): arquitetura interna, classes e fluxos

## Problemas comuns

- Porta `8080` ocupada:
  pare a instancia antiga ou suba com `-Dserver.port=8081`
- Painel sem atualizar:
  faca um `Ctrl+F5` para limpar cache do navegador
