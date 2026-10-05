# PostgreSQL do Home Davi

Este stack cria o banco `home_davi` com volume nomeado `home_davi_postgres_data` e a rede Docker privada `home-davi-internal`.

O banco é publicado apenas no loopback do CT (`127.0.0.1:5432`), portanto não fica acessível pela LAN ou internet. O container da aplicação deve entrar nessa mesma rede e usar:

`jdbc:postgresql://home-davi-postgres:5432/home_davi`

Para desenvolvimento remoto, crie um túnel: `ssh -L 5432:127.0.0.1:5432 root@192.168.2.185`.

As migrations incrementais que devem ser executadas depois da criação do volume ficam em `migrations/`. Por exemplo, `002-finance-schema.sql` cria o schema financeiro.

Defina `POSTGRES_PASSWORD` em `.env` no servidor antes de iniciar.
