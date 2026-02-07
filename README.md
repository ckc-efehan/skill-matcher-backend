# Skill Matcher Backend

![CI](https://github.com/ckc-efehan/skill-matcher-backend/actions/workflows/ci.yml/badge.svg)

## Tech Stack

- Kotlin 2.2.21
- Spring Boot 4.0.2
- Java 24
- PostgreSQL 18.1
- Liquibase (Datenbank-Migrationen)
- Gradle (Kotlin DSL)

## Voraussetzungen

- Java 24
- Docker & Docker Compose

## Lokale Entwicklung

### Datenbank starten

```bash
docker compose up -d
```

Startet eine PostgreSQL-Instanz auf `localhost:5432` mit der Datenbank `skillmatcher`.

### Anwendung starten

```bash
./gradlew bootRun
```

### Tests ausführen

```bash
./gradlew test
```

Tests nutzen Testcontainers — Docker muss laufen, aber keine manuelle Datenbank nötig.

### Code Coverage

```bash
./gradlew jacocoTestReport
```

Report unter `build/reports/jacoco/test/html/index.html`. Minimum: 80%.

### Linting

```bash
./gradlew ktlintCheck
```
