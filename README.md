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

## Endpoints

### Health Check
- **GET** `/api/health` - Verifica se a API está funcionando

### Surveys (Pesquisas)

#### Listar todas as pesquisas
- **GET** `/api/surveys`
  - Query params (opcional): `?ativo=true` - filtra apenas pesquisas ativas
  - Resposta: Lista de pesquisas

#### Buscar pesquisa por ID
- **GET** `/api/surveys/{id}`
  - Resposta: Dados da pesquisa

#### Criar nova pesquisa
- **POST** `/api/surveys`
  - Body (JSON):
    ```json
    {
      "titulo": "Pesquisa de Satisfação",
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
        "ativo": true,
        "dataValidade": "2024-12-31T23:59:59"
      },
      {
        "titulo": "Pesquisa de Qualidade",
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

### Modelo de Dados - Survey

```json
{
  "id": 1,
  "titulo": "Pesquisa de Satisfação",
  "ativo": true,
  "dataValidade": "2024-12-31T23:59:59",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**Campos:**
- `id` (Long): ID único da pesquisa (gerado automaticamente)
- `titulo` (String): Título da pesquisa (obrigatório, 3-255 caracteres)
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

A API está configurada para aceitar requisições do frontend React em `http://localhost:3000`.

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

