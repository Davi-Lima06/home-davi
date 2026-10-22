# Home Davi

Base em Java 21 e Spring Boot para o módulo `financial-control`, estruturada em arquitetura hexagonal.

## Estrutura

Cada módulo funcional fica em `br.com.davi.homedavi.<modulo>` e contém `domain`, `application` (ports/use cases) e `adapter` (entradas e saídas). O primeiro módulo é `finance`.

* REST: `POST` e `GET /api/v1/financial-control/transactions`
* Webhook Pluggy: `POST /api/v1/webhooks/pluggy` com cabeçalho `X-Webhook-Secret`
* MCP Streamable HTTP: `POST /mcp`

As integrações PostgreSQL ficam nos adapters JPA, preservando o domínio e os casos de uso independentes da infraestrutura.

## Conhecimento Datra

O módulo `datra` mantém uma base de conhecimento isolada no schema PostgreSQL `datra`.
Documentos enviados pela API começam como `PENDING_APPROVAL`; somente após `POST /api/v1/datra/documents/{id}/approve` eles ficam disponíveis ao perfil Datra pelo MCP. Assim, rascunhos, conversas e conteúdo não revisado nunca são recuperados pela IA.

* `POST /api/v1/datra/documents` — envia um documento para revisão;
* `POST /api/v1/datra/documents/{id}/approve` — aprova o documento;
* `GET /api/v1/datra/documents` — lista apenas documentos aprovados;
* MCP: `searchDatraKnowledge`, `readDatraDocument`, `listApprovedDatraDocuments`.

Antes de iniciar a aplicação, aplique `deploy/postgres/migrations/005-datra-knowledge-schema.sql` no banco `home_davi`.

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
