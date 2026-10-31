# Virtual Queue API

REST API for a virtual queue at a public service center (ЦОН). A visitor takes an electronic ticket for a service and can check the position in the queue and the estimated waiting time. An operator calls the next visitor to a window, starts and completes the service, or marks the visitor as a no-show.

## Tech Stack

- Java 21
- Spring Boot 4.1.2, Spring Web, Spring Data JPA (Hibernate), Spring Data Redis
- PostgreSQL 18
- Redis 8
- Flyway (database migrations and seed data)
- Lombok, Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- Gradle
- Docker and Docker Compose

## Requirements

- Docker with Docker Compose v2 (Docker Desktop includes both)
- Internet connection on the first start (Docker pulls the images)
- Postman (to run the collection)

No local Java, Gradle, PostgreSQL or Redis installation is needed. Everything runs in Docker.

## Running the Application

The `.env` file must be in the repository root next to `docker-compose.yml` (see [Environment Variables](#environment-variables)). Ports `8080`, `5432` and `6379` must be free on the host.

From the repository root:

```bash
docker compose up
```

On the first start Docker pulls three images and starts three containers:

| Service | Image | Host port | Purpose |
|---|---|---|---|
| `backend` | `banurr/webmidterm` | `8080` | the REST API |
| `database` | `postgres:18` | `5432` | PostgreSQL |
| `redis` | `redis:8-alpine` | `6379` | waiting queue and ticket counters |

The application is ready when the `backend` log shows `Started WebmidtermApplication`. The API is then available at `http://localhost:8080`, for example `http://localhost:8080/api/services`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

To run in the background and follow the application log:

```bash
docker compose up -d
docker compose logs -f backend
```

Stop the application:

```bash
docker compose down
```

### Building the image from source

`docker-compose.yml` uses the prebuilt image `banurr/webmidterm`. To build the image from the `Dockerfile` (multi-stage build with Gradle, JDK 21 for building and JRE 21 for running) and use it instead:

```bash
docker build -t banurr/webmidterm .
docker compose up -d
```

## Environment Variables

Docker Compose reads the variables from the `.env` file in the repository root. If the file is missing, create it with this content:

```
POSTGRES_DB=postgres
POSTGRES_HOST=database
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
POSTGRES_USER=postgres
REDIS_HOST=redis
REDIS_PASSWORD=redis
REDIS_PORT=6379
```

| Variable | Value | Description |
|---|---|---|
| `POSTGRES_DB` | `postgres` | Database name |
| `POSTGRES_HOST` | `database` | Hostname of the PostgreSQL container (its service name in `docker-compose.yml`) |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_USER` | `postgres` | Database user |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `redis` | Hostname of the Redis container (its service name in `docker-compose.yml`) |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | `redis` | Redis password |

Do not change `POSTGRES_PORT` and `REDIS_PORT`. The same variables set the port published on the host and the port the backend uses inside the Docker network.

## Database Setup and Import

The database schema and the initial data are imported automatically. When the application starts, Flyway runs the migrations against the PostgreSQL container, so no manual import is needed.

Database settings (from `.env`):

| Setting | Value |
|---|---|
| Host (from the host machine) | `localhost:5432` |
| Database | `postgres` |
| User | `postgres` |
| Password | `postgres` |

To look into the database:

```bash
docker compose exec database psql -U postgres -d postgres -c "SELECT * FROM services;"
```

### Migrations

Files are located in `src/main/resources/db/migration`:

| File | Purpose |
|---|---|
| `V1__create_tables.sql` | Creates `services` and `tickets`, the foreign key, the status check constraint and two indexes |
| `V2__seed_data.sql` | Inserts 4 services |

Seed data (`services`):

| id | code | name | meaning | avg_minutes |
|---|---|---|---|---|
| 1 | A | Справки и выписки | Certificates and extracts | 10 |
| 2 | B | Регистрация недвижимости | Property registration | 25 |
| 3 | C | Удостоверения личности и паспорта | ID cards and passports | 15 |
| 4 | D | Регистрация бизнеса | Business registration | 30 |

### Redis

Redis holds the live queue. It is protected by `REDIS_PASSWORD` and uses append-only persistence (volume `redis_data`).

| Key | Type | Purpose |
|---|---|---|
| `queue:waiting` | sorted set | ids of waiting tickets, the score is the ticket id, so the oldest ticket is called first |
| `queue:counter:<code>:<date>` | counter | daily counter per service, used for ticket numbers, expires after 2 days |

### Resetting the database

Flyway runs each migration only once, and the data is kept in Docker volumes (`postgres_data`, `redis_data`). To get a clean database with the original seed data and an empty queue, remove the volumes and start again:

```bash
docker compose down -v
docker compose up
```

Hibernate only validates the schema; Flyway owns it.

## Data Model

**ServiceType** (`services`)

| Field | Type | Notes |
|---|---|---|
| id | bigint | primary key |
| code | varchar(1) | unique, required, prefix of the ticket number (A, B, C, D) |
| name | varchar(150) | required |
| avg_minutes | integer | required, average service time in minutes, must be > 0 |

**Ticket** (`tickets`)

| Field | Type | Notes |
|---|---|---|
| id | bigint | primary key |
| ticket_number | varchar(10) | required, for example `A-001` |
| service_id | bigint | foreign key to `services`, required |
| status | varchar(20) | `WAITING`, `CALLED`, `IN_SERVICE`, `DONE`, `CANCELLED` or `NO_SHOW`, default `WAITING` |
| window_number | integer | window that called the ticket, nullable |
| created_at | timestamp | set when the ticket is created |
| called_at | timestamp | set by call-next, nullable |
| started_at | timestamp | set when the service starts, nullable |
| finished_at | timestamp | set when the ticket is completed, cancelled or marked as no-show, nullable |

One `ServiceType` has many `Ticket` entries (one-to-many). A ticket cannot exist without a service.

### Ticket lifecycle

```
WAITING --call-next--> CALLED --start--> IN_SERVICE --complete--> DONE
WAITING --cancel-----> CANCELLED
CALLED  --no-show----> NO_SHOW
```

### Business rules

- The ticket number is the service code plus a daily counter per service: `A-001`, `A-002`, `B-001`, and so on. The counter starts again every day.
- There is one queue for all services (no branches). Tickets are called in the order they were taken.
- `position` is the place in the queue (1 means the next one to be called). `waitMinutes` is `(position - 1)` multiplied by the average service time of the ticket's service. Both are `null` unless the ticket is `WAITING`.
- `windowNumber` is set by call-next and is `null` before that.
- Only a `WAITING` ticket can be cancelled. Start and no-show work only for a `CALLED` ticket, complete only for an `IN_SERVICE` ticket. Any other status gives 409.

## API Endpoints

### Services

| Method | URL | Description |
|---|---|---|
| GET | `/api/services` | List all services |

### Tickets (client)

| Method | URL | Description |
|---|---|---|
| POST | `/api/tickets` | Take a ticket for a service |
| GET | `/api/tickets/{id}` | Get a ticket with its position and estimated waiting time |
| DELETE | `/api/tickets/{id}` | Cancel a waiting ticket, returns the cancelled ticket |

POST body (`serviceId` is required):

```json
{
  "serviceId": 1
}
```

### Queue (operator)

| Method | URL | Description |
|---|---|---|
| GET | `/api/board` | Tickets that are being called or served, and the number of waiting tickets |
| POST | `/api/call-next` | Call the next waiting ticket to a window |
| POST | `/api/tickets/{id}/start` | `CALLED` to `IN_SERVICE` |
| POST | `/api/tickets/{id}/complete` | `IN_SERVICE` to `DONE` |
| POST | `/api/tickets/{id}/no-show` | `CALLED` to `NO_SHOW` |

Call-next body (`windowNumber` is required and must be positive):

```json
{
  "windowNumber": 3
}
```

### Example responses

Service:

```json
{
  "id": 1,
  "code": "A",
  "name": "Справки и выписки",
  "avgMinutes": 10
}
```

Ticket just taken, one ticket is ahead in the queue (`POST /api/tickets`, 201):

```json
{
  "id": 2,
  "number": "A-002",
  "service": "Справки и выписки",
  "status": "WAITING",
  "position": 2,
  "waitMinutes": 10,
  "windowNumber": null
}
```

Ticket after `POST /api/call-next` with `windowNumber` 3:

```json
{
  "id": 1,
  "number": "A-001",
  "service": "Справки и выписки",
  "status": "CALLED",
  "position": null,
  "waitMinutes": null,
  "windowNumber": 3
}
```

Board (`GET /api/board`):

```json
{
  "serving": [
    {
      "id": 1,
      "number": "A-001",
      "service": "Справки и выписки",
      "status": "CALLED",
      "position": null,
      "waitMinutes": null,
      "windowNumber": 3
    }
  ],
  "waiting": 1
}
```

Error:

```json
{
  "status": 404,
  "message": "Queue is empty"
}
```

### Status codes

| Code | When |
|---|---|
| 200 | successful GET, cancel, call-next, start, complete or no-show |
| 201 | ticket created |
| 400 | invalid request data: missing `serviceId` or `windowNumber`, `windowNumber` is not positive, malformed JSON, wrong field type, non-numeric id in the URL |
| 404 | service or ticket not found, or the queue is empty on call-next |
| 409 | the ticket has the wrong status for the action, or it cannot be cancelled |

## Postman Collection

The collection is in the repository root: `queue-api.postman_collection.json`.

1. Start the application with `docker compose up` on a fresh database.
2. In Postman choose Import and select the file. No environment is needed, all URLs use `http://localhost:8080`.
3. Run the requests one by one.

It contains two folders:

- **Client**: Get services, Take ticket, Get ticket, Cancel ticket.
- **Operator**: Get board, Call next, Start service, Complete service, Mark no-show.

The requests depend on each other, so run them one by one in the order below rather than with the Collection Runner. All ticket URLs in the collection use id `1`, and Call next uses window `3`. On a fresh database the first ticket gets id 1.

Full service cycle:

1. Get services
2. Take ticket (creates ticket 1)
3. Get ticket
4. Get board
5. Call next
6. Start service
7. Complete service

Cancel and no-show:

1. Take ticket (creates ticket 2), then Cancel ticket with `2` in the URL instead of `1`.
2. Take ticket (creates ticket 3), then Call next, then Mark no-show with `3` in the URL instead of `1`.

To start over, reset the database with `docker compose down -v`.

## Project Structure

```
Dockerfile
docker-compose.yml
.env
queue-api.postman_collection.json
build.gradle
src/main/java/kz/kbtu/webmidterm
  controller   REST controllers
  service      business logic and the Redis queue
  repository   Spring Data JPA repositories
  entity       JPA entities
  dto          request and response objects
  enums        TicketStatus
  exception    NotFoundException, ConflictException and the global exception handler
src/main/resources
  application.yaml
  db/migration  Flyway migrations (schema and seed data)
```