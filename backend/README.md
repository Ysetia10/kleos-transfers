# Backend

Spring Boot 3.x backend foundation for Kleos Transfers.

## Technology

- Java 21 and Gradle
- Spring Web, Validation, Data JPA, Actuator, and Lombok
- PostgreSQL and Flyway

## Run locally

Provide a reachable PostgreSQL database through the environment, then run from this directory:

```bash
gradle bootRun
```

Configuration uses environment-variable placeholders. See [`../.env.example`](../.env.example) for the shared local configuration convention. The only application endpoint at this foundation stage is `GET /api/health`.

There are intentionally no entities, repositories, migrations, business services, analytics, or prediction logic.
