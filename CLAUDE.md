# Vektor Mail Server — Claude Code Context

Full architecture specification: [Specifications.md](./Specifications.md)

## Project Summary

Java 21 + Spring Boot 3.x mail server with PF4J plugins, Apache Camel pipeline,
React 18 + TypeScript frontend. Multi-module Maven project.

## Build Commands

```bash
# Full build (all modules, skipping tests)
mvn install -DskipTests

# Full build with tests
mvn verify

# Build single module
mvn install -pl vektor-core -am

# Run the application (dev mode with H2)
mvn spring-boot:run -pl vektor-launcher -Dspring.profiles.active=dev

# Frontend only
cd vektor-frontend && npm install && npm run build

# Docker Compose
docker compose up -d

# Helm install (Kubernetes)
helm install vektor ./vektor-distribution/helm/vektor-mail -f my-values.yaml
```

## Module Map

| Module | Purpose |
|--------|---------|
| `vektor-core` | Domain models, events, PF4J plugin interfaces |
| `vektor-protocol-smtp` | SMTP server (Netty) |
| `vektor-protocol-imap` | IMAP server (Netty) |
| `vektor-protocol-pop3` | POP3 server (Netty) |
| `vektor-pipeline` | Apache Camel routes + route management |
| `vektor-storage-api` | Storage SPI (interfaces only) |
| `vektor-storage-db` | PostgreSQL storage plugin |
| `vektor-storage-maildir` | Maildir storage plugin |
| `vektor-storage-mbox` | Mbox storage plugin |
| `vektor-filter-blocklist` | IP/domain blocklist plugin |
| `vektor-filter-greylist` | Greylisting plugin |
| `vektor-filter-spam` | Spam detection plugin |
| `vektor-rules` | Server-side rules engine plugin |
| `vektor-backup` | Backup scheduler plugin |
| `vektor-sync` | Multi-instance sync plugin |
| `vektor-search` | Optional Lucene indexing plugin |
| `vektor-security` | TLS, JWT, AES-256-GCM utilities |
| `vektor-admin-api` | Admin REST API |
| `vektor-webmail-api` | Webmail REST API |
| `vektor-frontend` | React 18 + TypeScript SPA |
| `vektor-launcher` | Spring Boot entry point |
| `vektor-distribution` | Docker, Compose, Helm |

## Key Conventions

- **Plugin SPI**: All extension interfaces in `com.vektor.mail.core.plugin` (vektor-core)
- **Plugins**: PF4J JARs placed in `${vektor.plugins.dir}` (default: `./plugins/`)
- **Events**: Spring `ApplicationEvent` subclasses for in-process events
- **Pipeline**: Apache Camel routes defined in YAML, stored in `pipeline_routes` DB table
- **Encryption**: AES-256-GCM for at-rest content; master key via env `VEKTOR_MASTER_KEY`
- **Auth**: JWT RS256; access token 15 min; refresh token 30 days (httpOnly cookie)
- **DB migrations**: Flyway in `vektor-launcher/src/main/resources/db/migration/`
- **Tests**: JUnit 5 + Testcontainers; Camel test DSL for pipeline tests

## Environment Variables (required for production)

```
VEKTOR_MASTER_KEY          Base64 AES-256 key for at-rest encryption
VEKTOR_TLS_KEYSTORE_PATH   Path to PKCS12 keystore
VEKTOR_TLS_KEYSTORE_PASSWORD
VEKTOR_JWT_PRIVATE_KEY     PEM RS256 private key
VEKTOR_JWT_PUBLIC_KEY      PEM RS256 public key
SPRING_DATASOURCE_URL      jdbc:postgresql://host:5432/vektor
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

## API Endpoints (summary)

- `POST /api/auth/login` — get JWT
- `/api/admin/**` — admin operations (ROLE_ADMIN or ROLE_DOMAIN_ADMIN)
- `/api/webmail/**` — user mail access (ROLE_USER)
- `/api/sync/**` — peer sync (mTLS)
- `/api/docs` — OpenAPI 3 spec
- `/actuator/health` — health check
- `/actuator/prometheus` — metrics
