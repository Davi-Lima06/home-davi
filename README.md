# Home Davi

Base em Java 21 e Spring Boot para o módulo `financial-control`, estruturada em arquitetura hexagonal.

## Estrutura

Cada módulo funcional fica em `br.com.davi.homedavi.<modulo>` e contém `domain`, `application` (ports/use cases) e `adapter` (entradas e saídas). O primeiro módulo é `finance`.

* REST: `POST` e `GET /api/v1/financial-control/transactions`
* Webhook Pluggy: `POST /api/v1/webhooks/pluggy` com cabeçalho `X-Webhook-Secret`
* MCP Streamable HTTP: `POST /mcp`

As integrações PostgreSQL ficam nos adapters JPA, preservando o domínio e os casos de uso independentes da infraestrutura.

## Executar

```bash
export PLUGGY_WEBHOOK_SECRET="uma-chave-segura"
./mvnw spring-boot:run
```

Para validar: `./mvnw test`.

## Docker

```bash
docker build -f src/main/docker/Dockerfile.jvm -t home-davi:local .
docker run --rm --env-file .env -p 8080:8080 home-davi:local
```

## Webhook Pluggy e PostgreSQL

O endpoint `POST /api/v1/webhooks/pluggy` valida o header `X-Webhook-Secret`, grava o payload bruto em `integration.webhook_inbox` e cria uma mensagem em `integration.hermes_outbox` na mesma operação atômica. Reentregas do mesmo `eventId` são ignoradas com segurança.

No container da aplicação, configure estas variáveis de ambiente:

```bash
DATABASE_URL=jdbc:postgresql://home-davi-postgres:5432/home_davi
DATABASE_USERNAME=home_davi
DATABASE_PASSWORD=<senha-do-arquivo-.env-do-servidor>
PLUGGY_WEBHOOK_SECRET=<segredo-configurado-no-webhook-da-Pluggy>
```

O container precisa participar da rede Docker `home-davi-internal` criada pelo stack PostgreSQL.
