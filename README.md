# Survey API

API REST desenvolvida em Java 21 com Spring Boot e Hibernate para gerenciamento de pesquisas.

## Tecnologias

- **Java 21**
- **Spring Boot 3.2.0**
- **Hibernate/JPA**
- **MySQL**
- **Maven**

## Pré-requisitos

- Java 21 ou superior
- Maven 3.6+
- MySQL 8.0+

## Configuração

### 1. Banco de Dados

Crie um banco de dados MySQL:

```sql
CREATE DATABASE survey_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Ou deixe que a aplicação crie automaticamente (configurado no `application.properties`).

### 2. Configuração de Conexão

Edite o arquivo `src/main/resources/application.properties` e ajuste as credenciais do MySQL:

```properties
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

### 3. Executar a Aplicação

```bash
# Compilar e executar
mvn spring-boot:run

# Ou compilar primeiro
mvn clean install
java -jar target/survey-api-1.0.0.jar
```

A API estará disponível em: `http://localhost:8080`

### 4. Frontend local (CORS)

O arquivo `application.properties` expõe a propriedade `app.cors.allowed-origins`. Por padrão ela já libera `http://localhost:3000` (Create React App) e `http://localhost:5173` (Vite). Ajuste a lista separada por vírgulas caso use outra porta ou hostname.

### 5. Dados de exemplo para desenvolvimento

Enquanto `app.data.initialize=true` e o banco estiver vazio, a aplicação popula automaticamente uma pesquisa de exemplo com perguntas e opções. Defina o valor como `false` se não quiser carregar dados de demonstração ao subir a API.

### 6. Autenticação (Backoffice)

- A API expõe autenticação JWT para o painel administrativo.
- Endpoint de login: `POST /api/auth/login` com body:
  ```json
  {
    "username": "admin",
    "password": "admin"
  }
  ```
- Em ambiente local é criado automaticamente o usuário `admin/admin`. Altere as credenciais após o primeiro acesso.
- Após o login, utilize o token retornado no header `Authorization: Bearer <token>` para acessar rotas protegidas (CRUD de pesquisas, perguntas, opções e relatórios internos).
- Endpoints públicos (sem token): `GET /api/surveys/**`, `GET /api/questions/**`, `GET /api/options/**`, `POST /api/votes`, `GET /api/health`, documentação Swagger e Actuator.

### 7. Variáveis de ambiente (.env)

- Os valores sensíveis (banco de dados, JWT, CORS) são lidos de variáveis de ambiente, com defaults definidos em `application.properties`.
- Copie o arquivo `.env.example` para `.env` e ajuste os valores.
- Antes de rodar a aplicação localmente, execute `source .env` (ou configure seu IDE para carregar o arquivo automaticamente). Exemplo:
  ```bash
  source .env
  mvn spring-boot:run
  ```
- Em produção, defina as variáveis (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, etc.) no serviço/CI sem versioná-las no repositório.

### 8. Captura de sessões/respostas

- Cada voto gera uma sessão de resposta com metadados (IP, user-agent, dispositivo, SO, navegador, origem, localização e status concluído/abandono).
- Esses dados serão usados para os dashboards e métricas de audiência. O backend tenta inferir device/OS/browser a partir do `User-Agent`, mas o frontend pode enviar campos específicos no corpo do voto (`deviceType`, `operatingSystem`, `browser`, `source`, `country`, `state`, `city`, `status`, `startedAt`, `completedAt`).

### 9. Dashboards

- Após autenticação (role `ADMIN`), o endpoint `GET /api/dashboard/overview` retorna métricas globais:
  - Totais: pesquisas (ativas/inativas), respostas, respostas nos últimos 7/30 dias.
  - Taxas médias de conclusão/abandono e tempo médio de resposta.
  - Rankings: pesquisas mais respondidas, maior taxa de conclusão/abandono, recém-criadas e próximas do vencimento.
  - Use o token JWT no header `Authorization` para acessar.
- Para diagnósticos por pesquisa específica, utilize `GET /api/dashboard/surveys/{id}?from=2025-11-01T00:00:00&to=2025-11-20T23:59:59&includeDeleted=true`.
  - Retorna métricas da pesquisa (totais, taxas, tempo médio, pergunta com mais abandono, dispositivo predominante).
  - Estatísticas por pergunta/opção (contagens, percentuais).
  - Séries temporais (respostas por dia/hora) e distribuição da audiência (device/OS/browser/origem/geo).
- Para a audiência detalhada utilize `GET /api/dashboard/surveys/{id}/audience?from=...&to=...&includeDeleted=true`.
  - Entrega distribuições por dispositivo, SO, navegador, origem, país/estado/cidade.
  - Mostra horários/dias de pico, tempo médio até abandono, respondentes únicos x duplicados e possíveis indícios suspeitos.

## Privacidade (LGPD)

- O backend registra IP, user-agent e localização enviados pelo frontend em `ResponseSession`.
- Em produção, considere anonimizar/truncar IP (ex.: remover último octeto ou aplicar hash com salt) e definir retenção (ex.: 90 dias).
- Exiba consentimento no frontend antes de coletar dados de audiência e envie somente os campos autorizados.
- Consulte `src/main/resources/db/migration/README-lgpd.md` para recomendações adicionais.

### Audience/IP anon por perfil
- **dev** (`application-dev.properties`): `AUDIENCE_ENABLED=true`, `IP_ANONYMIZE=true` por padrão. Ajuste via env para testar ambos os cenários.
- **default/local** (`application.properties`): lê `AUDIENCE_ENABLED`/`IP_ANONYMIZE` do ambiente (defaults true). Use para validar a API contra MySQL local sem seeds obrigatórios.
- **prod** (`application-prod.properties`): exige `JWT_SECRET`, `DB_*`, `FRONTEND_ORIGINS`. Seeds desligados. Ajuste `AUDIENCE_ENABLED` e `IP_ANONYMIZE` conforme política; `RETENTION_DAYS` e `app.privacy.cleanup-cron` definem a limpeza de sessões.

### Métricas (Prometheus)
- Endpoint: `/api/actuator/prometheus` (proteja com auth/role).
- Principais métricas customizadas:
  - `request.validation.failures` (conta erros de validação/negócio).
  - `vote.duplicate.blocked` (bloqueios por janela antifraude).
  - `survey.operations{type=create|update|delete}`, `question.operations{...}`, `option.operations{...}`.
- Métricas padrão do Actuator/Micrometer (JVM, Hikari, HTTP server) também estão expostas.

## Guia rápido para o Backoffice (ADMIN)
- Login: `POST /api/auth/login` com `{ "username": "admin", "password": "admin" }` em dev; resposta contém `token`.
- Em qualquer rota interna, enviar `Authorization: Bearer <token>`.
- CRUD:
  - Surveys: `GET/POST/PUT/DELETE /api/surveys` (estrutura completa: `GET /api/surveys/{id}/structure`).
  - Questions: `GET/POST/PUT/DELETE /api/questions`.
  - Options: `GET/POST/PUT/DELETE /api/options`.
- Dashboards/analytics (ADMIN): `GET /api/dashboard/overview`, `GET /api/dashboard/surveys/{id}`, `GET /api/dashboard/surveys/{id}/audience`.
- Paginação/sort: `page`, `size` (máx 100), `sort`, `direction` em listagens.
- Correlation-id: propagar `X-Correlation-Id` para rastrear requisições; o backend gera se ausente.
- Exemplos de payload (criação):
  - Survey: `{"titulo":"Pesquisa X","ativo":true,"dataValidade":"2025-12-31T23:59:59"}`
  - Question: `{"surveyId":1,"texto":"Pergunta?","ordem":1,"ativo":true}`
  - Option: `{"questionId":10,"texto":"Opção","ativo":true}`

## Configuração de ambiente

- Perfis:
  - `dev`: H2 em memória, seeds automáticos, logs verbosos. Executar com `-Dspring-boot.run.profiles=dev`.
  - `prod`: MySQL, Flyway, seeds desativados, pool Hikari, DevTools/Livereload desligados. Ative com `SPRING_PROFILES_ACTIVE=prod`.
- Variáveis principais:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET` (mínimo 32 chars), `JWT_EXPIRATION`
  - `FRONTEND_ORIGINS` (lista separada por vírgula)
  - `AUDIENCE_ENABLED` (true/false), `IP_ANONYMIZE` (true/false), `RETENTION_DAYS`

## Deploy no Fly.io

- Requisitos: `flyctl` instalado e autenticado (`flyctl auth login`).
- Envie as secrets de produção (ex.: `cp .env.example .env.prod` e ajuste valores reais):
  ```bash
  flyctl secrets import < .env.prod
  ```
- Para build/deploy usando o Dockerfile do projeto com builder remoto:
  ```bash
  ./scripts/fly-deploy.sh
  # ou
  make fly-deploy ARGS="--strategy immediate"
  ```
- O script lê `app` e `primary_region` do `fly.toml`, gera um label da imagem com timestamp e aceita flags extras do `flyctl deploy` (ex.: `--build-arg`, `--vm-size`).

## Endpoints

### Health Check
- **GET** `/api/health` - Verifica se a API está funcionando

### Surveys (Pesquisas)

#### Listar todas as pesquisas
- **GET** `/api/surveys`
  - Query params (opcionais):
    - `ativo=true` - filtra apenas pesquisas ativas
    - `includeDeleted=true` - inclui também pesquisas soft-deletadas (ignora filtro `ativo`)
  - Resposta: Lista de pesquisas

#### Buscar pesquisa por ID
- **GET** `/api/surveys/{id}`
  - Resposta: Dados da pesquisa

#### Buscar estrutura completa da pesquisa (perguntas + opções)
- **GET** `/api/surveys/{id}/structure`
  - Query params (opcionais):
    - `includeInactiveOptions=false` - define se opções inativas também devem ser retornadas
    - `includeDeleted=false` - inclui pesquisas/perguntas/opções soft-deletadas
  - Resposta:
    ```json
    {
      "id": 1,
      "titulo": "Pesquisa de Satisfação",
      "ativo": true,
      "questions": [
        {
          "id": 10,
          "texto": "Qual sua idade?",
          "ordem": 1,
          "options": [
            {
              "id": 100,
              "texto": "Entre 18 e 25 anos",
              "ativo": true
            }
          ]
        }
      ]
    }
    ```

- **POST** `/api/surveys`
  - Body (JSON):
    ```json
    {
      "titulo": "Pesquisa de Satisfação",
      "descricao": "Texto explicando o objetivo da pesquisa",
      "ativo": true,
      "dataValidade": "2024-12-31T23:59:59"
    }
    ```
  - Resposta: Pesquisa criada (status 201)

#### Criar múltiplas pesquisas (em lote)
- **POST** `/api/surveys/batch`
  - Body (JSON): Array de pesquisas
    ```json
    [
      {
        "titulo": "Pesquisa de Satisfação",
        "descricao": "Primeira pesquisa",
        "ativo": true,
        "dataValidade": "2024-12-31T23:59:59"
      },
      {
        "titulo": "Pesquisa de Qualidade",
        "descricao": "Segunda pesquisa",
        "ativo": true,
        "dataValidade": "2024-12-31T23:59:59"
      }
    ]
    ```
  - Resposta: Lista de pesquisas criadas (status 201)
  - **Validações:**
    - Títulos únicos dentro da lista
    - Títulos únicos no banco de dados
    - Lista não pode estar vazia

#### Atualizar pesquisa
- **PUT** `/api/surveys/{id}`
  - Body (JSON): Mesmo formato do POST
  - Resposta: Pesquisa atualizada

#### Deletar pesquisa
- **DELETE** `/api/surveys/{id}`
  - Resposta: Status 204 (No Content)
  - Observações: remove automaticamente perguntas, opções e votos associados antes de excluir a pesquisa

### Modelo de Dados - Survey

```json
{
  "id": 1,
  "titulo": "Pesquisa de Satisfação",
  "descricao": "Texto explicando o objetivo da pesquisa",
  "ativo": true,
  "dataValidade": "2024-12-31T23:59:59",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Campos:**
- `id` (Long): ID único da pesquisa (gerado automaticamente)
- `titulo` (String): Título da pesquisa (obrigatório, 3-255 caracteres)
- `descricao` (String): Descrição/objetivo da pesquisa (opcional, até 1000 caracteres)
- `ativo` (Boolean): Status ativo/inativo (obrigatório)
- `dataValidade` (LocalDateTime): Data de validade da pesquisa (opcional)
- `createdAt` (LocalDateTime): Data de criação (gerado automaticamente)
- `updatedAt` (LocalDateTime): Data de última atualização (gerado automaticamente)

### Questions (Perguntas)

#### Listar todas as perguntas
- **GET** `/api/questions`
  - Query params (opcional): `?surveyId={id}` - filtra perguntas de uma pesquisa específica
  - Resposta: Lista de perguntas (ordenadas por ordem quando filtradas por surveyId)

#### Buscar pergunta por ID
- **GET** `/api/questions/{id}`
  - Resposta: Dados da pergunta

#### Criar nova pergunta
- **POST** `/api/questions`
  - Body (JSON):
    ```json
    {
      "texto": "Qual sua idade?",
      "ordem": 1,
      "surveyId": 1
    }
    ```
  - Resposta: Pergunta criada (status 201)

#### Criar múltiplas perguntas (em lote)
- **POST** `/api/questions/batch`
  - Body (JSON): Array de perguntas
    ```json
    [
      {
        "texto": "Qual sua idade?",
        "ordem": 1,
        "surveyId": 1
      },
      {
        "texto": "Qual sua cidade?",
        "ordem": 2,
        "surveyId": 1
      },
      {
        "texto": "Qual seu estado?",
        "ordem": 3,
        "surveyId": 1
      }
    ]
    ```
  - Resposta: Lista de perguntas criadas (status 201)
  - **Validações:**
    - Todas as perguntas devem pertencer à mesma pesquisa
    - Ordens únicas dentro da lista
    - Ordens únicas no banco de dados para a pesquisa
    - Lista não pode estar vazia

#### Atualizar pergunta
- **PUT** `/api/questions/{id}`
  - Body (JSON): Mesmo formato do POST
  - Resposta: Pergunta atualizada

#### Deletar pergunta
- **DELETE** `/api/questions/{id}`
  - Resposta: Status 204 (No Content)

### Modelo de Dados - Question

```json
{
  "id": 1,
  "texto": "Qual sua idade?",
  "ordem": 1,
  "surveyId": 1,
  "surveyTitulo": "Pesquisa de Satisfação",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Campos:**
- `id` (Long): ID único da pergunta (gerado automaticamente)
- `texto` (String): Texto da pergunta (obrigatório, 3-500 caracteres)
- `ordem` (Integer): Ordem da pergunta na pesquisa (obrigatório)
- `surveyId` (Long): ID da pesquisa à qual a pergunta pertence (obrigatório)
- `surveyTitulo` (String): Título da pesquisa (incluído na resposta)
- `createdAt` (LocalDateTime): Data de criação (gerado automaticamente)
- `updatedAt` (LocalDateTime): Data de última atualização (gerado automaticamente)

### Options (Opções de Resposta)

#### Listar todas as opções
- **GET** `/api/options`
  - Query params (opcionais):
    - `?questionId={id}` - filtra opções de uma pergunta específica
    - `?ativo=true` - filtra apenas opções ativas (pode ser combinado com questionId)
  - Resposta: Lista de opções

#### Buscar opção por ID
- **GET** `/api/options/{id}`
  - Resposta: Dados da opção

#### Criar nova opção
- **POST** `/api/options`
  - Body (JSON):
    ```json
    {
      "texto": "Entre 18 e 25 anos",
      "ativo": true,
      "questionId": 1
    }
    ```
  - Resposta: Opção criada (status 201)
  - **Validação:** Máximo de 5 opções ativas por pergunta

#### Criar múltiplas opções (em lote)
- **POST** `/api/options/batch`
  - Body (JSON): Array de opções
    ```json
    [
      {
        "texto": "Entre 0 e 20 anos",
        "ativo": true,
        "questionId": 1
      },
      {
        "texto": "Entre 21 e 40 anos",
        "ativo": true,
        "questionId": 1
      },
      {
        "texto": "Entre 41 e 60 anos",
        "ativo": true,
        "questionId": 1
      }
    ]
    ```
  - Resposta: Lista de opções criadas (status 201)
  - **Validações:**
    - Todas as opções devem pertencer à mesma pergunta
    - Máximo de 5 opções ativas por pergunta (considera opções existentes + novas)
    - Lista não pode estar vazia

#### Atualizar opção
- **PUT** `/api/options/{id}`
  - Body (JSON): Mesmo formato do POST
  - Resposta: Opção atualizada
  - **Validação:** Máximo de 5 opções ativas por pergunta

#### Deletar opção
- **DELETE** `/api/options/{id}`
  - Resposta: Status 204 (No Content)

### Modelo de Dados - Option

```json
{
  "id": 1,
  "texto": "Entre 18 e 25 anos",
  "ativo": true,
  "questionId": 1,
  "questionTexto": "Qual sua idade?",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Campos:**
- `id` (Long): ID único da opção (gerado automaticamente)
- `texto` (String): Texto da opção (obrigatório, 1-255 caracteres)
- `ativo` (Boolean): Status ativo/inativo (obrigatório)
- `questionId` (Long): ID da pergunta à qual a opção pertence (obrigatório)
- `questionTexto` (String): Texto da pergunta (incluído na resposta)
- `createdAt` (LocalDateTime): Data de criação (gerado automaticamente)
- `updatedAt` (LocalDateTime): Data de última atualização (gerado automaticamente)

**Regra de Negócio:**
- Máximo de 5 opções ativas por pergunta
- Ao tentar criar ou ativar uma opção quando já existem 5 ativas, retorna erro 400

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/survey/
│   │   ├── config/          # Configurações (CORS, etc)
│   │   ├── controller/      # Controllers REST
│   │   ├── service/         # Lógica de negócio
│   │   ├── repository/      # Repositórios JPA
│   │   ├── entity/          # Entidades JPA
│   │   ├── dto/             # Data Transfer Objects
│   │   └── exception/       # Tratamento de exceções
│   └── resources/
│       └── application.properties
└── test/
```

## CORS

A lista de origens permitidas é definida em `app.cors.allowed-origins` (arquivo `application.properties`). Por padrão já inclui `http://localhost:3000` (Create React App) e `http://localhost:5173` (Vite). Acrescente novas origens separadas por vírgula conforme necessário.

## Validações

A API valida automaticamente:

**Surveys:**
- Título obrigatório (mínimo 3, máximo 255 caracteres)
- Status ativo obrigatório
- Título único (não permite duplicatas)

**Questions:**
- Texto da pergunta obrigatório (mínimo 3, máximo 500 caracteres)
- Ordem obrigatória
- ID da pesquisa obrigatório
- Ordem única por pesquisa (não permite duas perguntas com a mesma ordem na mesma pesquisa)

**Options:**
- Texto da opção obrigatório (mínimo 1, máximo 255 caracteres)
- Status ativo obrigatório
- ID da pergunta obrigatório
- Máximo de 5 opções ativas por pergunta

## Tratamento de Erros

A API retorna erros padronizados no formato:

```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 400,
  "error": "Erro de validação",
  "message": "Dados inválidos",
  "errors": {
    "titulo": "Título é obrigatório"
  }
}
```

## Exemplos de Uso

### Criar uma pesquisa
```bash
curl -X POST http://localhost:8080/api/surveys \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Pesquisa de Satisfação",
    "ativo": true,
    "dataValidade": "2024-12-31T23:59:59"
  }'
```

### Criar múltiplas pesquisas de uma vez (em lote)
```bash
curl -X POST http://localhost:8080/api/surveys/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "titulo": "Pesquisa de Satisfação",
      "ativo": true,
      "dataValidade": "2024-12-31T23:59:59"
    },
    {
      "titulo": "Pesquisa de Qualidade",
      "ativo": true,
      "dataValidade": "2024-12-31T23:59:59"
    }
  ]'
```

### Listar todas as pesquisas
```bash
curl http://localhost:8080/api/surveys
```

### Listar apenas pesquisas ativas
```bash
curl http://localhost:8080/api/surveys?ativo=true
```

### Atualizar uma pesquisa
```bash
curl -X PUT http://localhost:8080/api/surveys/1 \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Pesquisa Atualizada",
    "ativo": false,
    "dataValidade": "2024-12-31T23:59:59"
  }'
```

### Deletar uma pesquisa
```bash
curl -X DELETE http://localhost:8080/api/surveys/1
```

### Criar uma pergunta
```bash
curl -X POST http://localhost:8080/api/questions \
  -H "Content-Type: application/json" \
  -d '{
    "texto": "Qual sua idade?",
    "ordem": 1,
    "surveyId": 1
  }'
```

### Criar múltiplas perguntas de uma vez (em lote)
```bash
curl -X POST http://localhost:8080/api/questions/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "texto": "Qual sua idade?",
      "ordem": 1,
      "surveyId": 1
    },
    {
      "texto": "Qual sua cidade?",
      "ordem": 2,
      "surveyId": 1
    },
    {
      "texto": "Qual seu estado?",
      "ordem": 3,
      "surveyId": 1
    }
  ]'
```

### Listar todas as perguntas
```bash
curl http://localhost:8080/api/questions
```

### Listar perguntas de uma pesquisa
```bash
curl http://localhost:8080/api/questions?surveyId=1
```

### Atualizar uma pergunta
```bash
curl -X PUT http://localhost:8080/api/questions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "texto": "Qual sua idade atual?",
    "ordem": 1,
    "surveyId": 1
  }'
```

### Deletar uma pergunta
```bash
curl -X DELETE http://localhost:8080/api/questions/1
```

### Criar uma opção de resposta
```bash
curl -X POST http://localhost:8080/api/options \
  -H "Content-Type: application/json" \
  -d '{
    "texto": "Entre 18 e 25 anos",
    "ativo": true,
    "questionId": 1
  }'
```

### Criar múltiplas opções de uma vez (em lote)
```bash
curl -X POST http://localhost:8080/api/options/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "texto": "Entre 0 e 20 anos",
      "ativo": true,
      "questionId": 1
    },
    {
      "texto": "Entre 21 e 40 anos",
      "ativo": true,
      "questionId": 1
    },
    {
      "texto": "Entre 41 e 60 anos",
      "ativo": true,
      "questionId": 1
    },
    {
      "texto": "Entre 61 e 80 anos",
      "ativo": true,
      "questionId": 1
    },
    {
      "texto": "Mais de 80 anos",
      "ativo": true,
      "questionId": 1
    }
  ]'
```

### Listar todas as opções
```bash
curl http://localhost:8080/api/options
```

### Listar opções de uma pergunta
```bash
curl http://localhost:8080/api/options?questionId=1
```

### Listar apenas opções ativas de uma pergunta
```bash
curl http://localhost:8080/api/options?questionId=1&ativo=true
```

### Atualizar uma opção
```bash
curl -X PUT http://localhost:8080/api/options/1 \
  -H "Content-Type: application/json" \
  -d '{
    "texto": "Entre 18 e 30 anos",
    "ativo": true,
    "questionId": 1
  }'
```

### Deletar uma opção
```bash
curl -X DELETE http://localhost:8080/api/options/1
```
### Admin - Usuários do Backoffice

Requer token JWT válido com role `ADMIN`.

- **GET** `/api/admin/users` – lista todos os usuários administrativos
- **GET** `/api/admin/users/{id}` – detalhes de um usuário
- **POST** `/api/admin/users`
  ```json
  {
    "username": "novo.admin",
    "password": "senhaSegura123",
    "role": "ADMIN"
  }
  ```
- **PUT** `/api/admin/users/{id}` – atualiza username/role
- **PATCH** `/api/admin/users/{id}/password`
  ```json
  {
    "newPassword": "novaSenha"
  }
  ```
- **DELETE** `/api/admin/users/{id}` – remove usuário

### Restaurar pesquisa (soft delete)
- **PATCH** `/api/surveys/{id}/restore` – reativa pesquisa, perguntas e opções marcadas com `deleted_at`
