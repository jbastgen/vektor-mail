# Vektor Mail Server — Architecture Specification

## Overview

Vektor Mail is a production-grade, plugin-driven mail server targeting private users and companies.
It is built on Java 21 and Spring Boot 3.x, with a modular architecture based on PF4J plugins,
an Apache Camel processing pipeline, and a React-based admin and webmail interface.

---

## Technology Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 21 (records, sealed classes, virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`) |
| Framework | Spring Boot 3.x |
| Build | Maven 3.9+ (multi-module, parent POM) |
| Plugin system | PF4J 3.x |
| Processing pipeline | Apache Camel 4.x |
| Protocol servers | Netty 4.x (SMTP, IMAP, POP3) |
| Database | PostgreSQL 16 (production), H2 (embedded/dev) |
| ORM | Spring Data JPA + Hibernate 6 |
| Migration | Flyway |
| Frontend | React 18 + TypeScript, bundled with Vite |
| UI components | Ant Design (antd) |
| Pipeline visual editor | React Flow |
| State management | Zustand |
| Data fetching | TanStack Query (React Query) |
| API documentation | Springdoc OpenAPI 3 |
| Authentication | Spring Security + JWT (RS256) |
| Encryption at rest | AES-256-GCM via JCA |
| Password hashing | Argon2id |
| Monitoring | Spring Actuator + Micrometer + Prometheus |
| Containers | Docker + Docker Compose |
| Kubernetes | Helm Charts |
| Testing | JUnit 5, Mockito, Testcontainers |
| Code quality | Checkstyle, SpotBugs, JaCoCo |

---

## Multi-Module Maven Project Structure

```
vektor-mail/
├── pom.xml                          # Parent POM (dependency management, version alignment)
│
├── vektor-core/                     # Core domain: models, events, plugin SPI, shared utilities
├── vektor-protocol-smtp/            # Netty-based SMTP server (MTA port 25 + MSA port 587/465)
├── vektor-protocol-imap/            # Netty-based IMAP4rev2 server (port 143/993)
├── vektor-protocol-pop3/            # Netty-based POP3 server (port 110/995)
│
├── vektor-pipeline/                 # Apache Camel engine, route management, route persistence
│
├── vektor-storage-api/              # Storage plugin SPI (interfaces only, no impl)
├── vektor-storage-db/               # PostgreSQL storage plugin (PF4J)
├── vektor-storage-maildir/          # Maildir format storage plugin (PF4J)
├── vektor-storage-mbox/             # Mbox format storage plugin (PF4J)
│
├── vektor-filter-blocklist/         # IP/domain blocklist filter plugin (PF4J)
├── vektor-filter-greylist/          # Greylisting filter plugin (PF4J)
├── vektor-filter-spam/              # Spam detection plugin (PF4J) — integrates RSpamd or SpamAssassin
│
├── vektor-rules/                    # Server-side mail rules engine plugin (PF4J)
├── vektor-backup/                   # Backup scheduler plugin (PF4J)
├── vektor-sync/                     # Multi-instance synchronisation plugin (PF4J)
├── vektor-search/                   # Optional Lucene indexing plugin (PF4J)
│
├── vektor-security/                 # TLS utilities, JWT issuance/validation, AES-256-GCM helpers
├── vektor-admin-api/                # Admin REST API (Spring MVC + Spring Security)
├── vektor-webmail-api/              # Webmail REST API (Spring MVC + Spring Security)
├── vektor-frontend/                 # React 18 + TypeScript SPA (admin + webmail)
│
├── vektor-launcher/                 # Main Spring Boot application entry point
└── vektor-distribution/             # Packaging: Docker, Docker Compose, Helm charts
    ├── docker/
    │   ├── Dockerfile
    │   └── docker-compose.yml
    └── helm/
        └── vektor-mail/
            ├── Chart.yaml
            ├── values.yaml
            └── templates/
```

### Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `vektor-core` | Domain models, Spring Events, PF4J extension interfaces, shared DTOs |
| `vektor-protocol-smtp` | SMTP dialogue, STARTTLS, AUTH, relaying, envelope processing |
| `vektor-protocol-imap` | IMAP4rev2 command handling, folder operations, IDLE support |
| `vektor-protocol-pop3` | POP3 command handling, message retrieval, deletion |
| `vektor-pipeline` | Camel context bootstrap, route loader, route persistence, Camel REST DSL for pipeline API |
| `vektor-storage-api` | `StoragePlugin` interface, `MessageStore`, `AttachmentStore`, `MailboxStore` interfaces |
| `vektor-storage-db` | JPA entities + repositories, stores all content in PostgreSQL BYTEA/TEXT columns |
| `vektor-storage-maildir` | One file per message in `~/Maildir/` directory structure |
| `vektor-storage-mbox` | One mbox file per mailbox |
| `vektor-filter-*` | Stateless filter steps that return ACCEPT / REJECT / DEFER decisions |
| `vektor-rules` | Sieve-inspired rule engine: conditions on headers/size/flags → actions (move, copy, discard, forward) |
| `vektor-backup` | Spring `@Scheduled` jobs, pluggable backends (local, S3-compatible, SFTP) |
| `vektor-sync` | Camel HTTP/HTTPS routes to push `MailEvent` payloads to peer instances |
| `vektor-search` | Lucene 9 index over message headers + body; exposes `SearchService` bean |
| `vektor-security` | JSSE TLS helpers, JWT RS256 token lifecycle, AES-256-GCM `MessageCipher` |
| `vektor-admin-api` | REST endpoints for server administration; role `ROLE_ADMIN` or `ROLE_DOMAIN_ADMIN` |
| `vektor-webmail-api` | REST endpoints for per-user mail access; role `ROLE_USER` |
| `vektor-frontend` | Vite build → `vektor-launcher/src/main/resources/static/` via Maven resource plugin |
| `vektor-launcher` | `@SpringBootApplication`, assembles all modules, owns `application.yml` |
| `vektor-distribution` | Docker image, Compose stack, Helm chart |

---

## PF4J Plugin Extension Points

All interfaces live in `com.vektor.mail.core.plugin` inside `vektor-core`.

```java
// Base for all plugins — PF4J Plugin subclass
public abstract class VektorPlugin extends Plugin {
    public abstract String getPluginId();
    public abstract String getDisplayName();
    public abstract JsonSchema getConfigSchema(); // rendered in admin UI
}

// Storage backend
public interface StoragePlugin {
    void storeMessage(StorageContext ctx, MimeMessage message) throws StorageException;
    Optional<MimeMessage> loadMessage(String messageId) throws StorageException;
    void deleteMessage(String messageId) throws StorageException;
    List<MessageSummary> listMessages(String mailboxId, SearchCriteria criteria);
}

// Processing filter (spam, blocklist, greylist, DKIM, …)
public interface FilterPlugin {
    FilterDecision evaluate(MailContext ctx); // ACCEPT | REJECT | DEFER | CONTINUE
    int getPriority(); // lower = runs first
}

// Server-side rules
public interface RulesPlugin {
    void applyRules(MailContext ctx, List<Rule> rules);
}

// Search/indexing
public interface IndexPlugin {
    void index(MessageSummary summary);
    List<String> search(String query, String accountId);
    void delete(String messageId);
}

// Backup strategy
public interface BackupPlugin {
    void backup(BackupContext ctx) throws BackupException;
    void restore(RestoreContext ctx) throws BackupException;
    BackupStatus status();
}

// Multi-instance sync
public interface SyncPlugin {
    void publishEvent(MailEvent event);
    void registerPeer(SyncPeer peer);
}

// Authentication provider (LDAP, OAuth, local DB, …)
public interface AuthPlugin {
    Optional<UserDetails> authenticate(String username, String password);
    boolean supports(AuthMethod method);
}
```

Plugins are JAR files placed in `${vektor.plugins.dir}` (default: `./plugins/`).
The admin UI allows uploading new plugin JARs, enabling/disabling plugins, and editing their JSON config.
PF4J handles classloader isolation so plugins cannot break the core.

---

## Mail Processing Pipeline

### Architecture

The pipeline is driven by Apache Camel 4.x. Each processing step is a `Processor` that
either delegates to a `FilterPlugin` or performs a built-in operation.

Route definitions are stored as YAML in the `pipeline_routes` database table.
On startup, `VektorCamelContext` loads all enabled routes. On save via the admin UI,
routes are updated live using `CamelContext.addRoutes()` / `RouteController.stopRoute()`.

### Incoming Mail Flow (default route)

```
[SMTP Server / Netty]
       │  on SMTP DATA complete
       ▼
 Camel direct:mail.incoming
       │
       ├─ 1. IP Blocklist Check          FilterPlugin (vektor-filter-blocklist)
       │       → REJECT → 550 response to SMTP
       │
       ├─ 2. Greylist Check              FilterPlugin (vektor-filter-greylist)
       │       → DEFER → 451 response; retry expected
       │
       ├─ 3. Domain Blocklist Check      FilterPlugin (vektor-filter-blocklist)
       │       → REJECT → 550 response
       │
       ├─ 4. DKIM / SPF / DMARC Verify  built-in CamelProcessor (dnsjava lookup)
       │       → annotates MailContext with verification result
       │
       ├─ 5. Spam Scoring                FilterPlugin (vektor-filter-spam)
       │       → adds X-Vektor-Spam-Score header; optionally REJECT above threshold
       │
       ├─ 6. Server-side Rules           RulesPlugin (vektor-rules)
       │       → moves / copies / discards / forwards per account rules
       │
       ├─ 7. Encryption                  built-in CamelProcessor
       │       → encrypts message content with AES-256-GCM DEK
       │       → wraps DEK with master key; stores encrypted DEK in Message entity
       │
       ├─ 8. Storage                     StoragePlugin (selected per domain/config)
       │       → persists MimeMessage + metadata
       │
       └─ 9. Post-storage events
              ├─ Spring Event: MailStoredEvent
              ├─ Camel direct:mail.index   (if IndexPlugin enabled)
              └─ Camel direct:mail.sync    (if SyncPlugin enabled)
```

### Outgoing Mail Flow

```
[Webmail API or SMTP submission port 587]
       │
       ▼
 Camel direct:mail.outgoing
       │
       ├─ 1. Auth & Rate Limit check
       ├─ 2. DKIM Sign (Ed25519)
       ├─ 3. Store in Sent folder (StoragePlugin)
       └─ 4. SMTP relay / direct delivery (Camel smtp: component)
```

### Route Configuration Format (YAML stored in DB)

```yaml
- id: incoming-default
  from: direct:mail.incoming
  steps:
    - filter: ip-blocklist
    - filter: greylist
    - filter: domain-blocklist
    - processor: dkim-spf-dmarc-verifier
    - filter: spam-scorer
    - processor: rules-engine
    - processor: message-encryptor
    - processor: storage-adapter
    - processor: post-storage-events
```

Steps reference plugin IDs. The React Flow pipeline editor renders this YAML as a
directed graph and saves changes back as YAML via `PUT /api/admin/pipeline/{id}`.

---

## Core Domain Model

All JPA entities in `vektor-core`, package `com.vektor.mail.core.model`.

```
Domain
  id              UUID PK
  name            VARCHAR(255) UNIQUE  -- e.g. "example.com"
  active          BOOLEAN
  dkimPrivateKey  BYTEA encrypted       -- AES-256-GCM
  dkimSelector    VARCHAR(64)
  spfPolicy       VARCHAR(255)
  dmarcPolicy     VARCHAR(255)
  createdAt       TIMESTAMP

Account
  id              UUID PK
  email           VARCHAR(255) UNIQUE
  passwordHash    VARCHAR(255)         -- Argon2id
  domainId        UUID FK → Domain
  roles           VARCHAR[]            -- ADMIN, DOMAIN_ADMIN, USER
  quotaBytes      BIGINT
  active          BOOLEAN
  createdAt       TIMESTAMP

Mailbox
  id              UUID PK
  accountId       UUID FK → Account
  name            VARCHAR(255)         -- INBOX, Sent, Drafts, Trash, custom
  path            VARCHAR(1024)        -- full IMAP path
  flags           VARCHAR[]            -- \Noinferiors, \Noselect, …
  uidValidity     BIGINT
  uidNext         BIGINT

Alias
  id              UUID PK
  sourceAddress   VARCHAR(255)
  targetAddress   VARCHAR(255)
  domainId        UUID FK → Domain
  active          BOOLEAN

Message
  id              UUID PK
  messageId       VARCHAR(512)         -- RFC 5322 Message-ID header
  mailboxId       UUID FK → Mailbox
  subject         VARCHAR(1024)
  fromAddress     VARCHAR(512)
  toAddresses     TEXT                 -- JSON array
  receivedAt      TIMESTAMP
  sizeBytes       BIGINT
  flags           VARCHAR[]            -- \Seen, \Answered, \Flagged, \Deleted, \Draft
  storageRef      VARCHAR(1024)        -- backend-specific reference (path, DB id, etc.)
  encryptedDek    BYTEA                -- AES DEK wrapped with master key
  spamScore       DECIMAL(5,2)
  dkimResult      VARCHAR(32)
  spfResult       VARCHAR(32)

Attachment
  id              UUID PK
  messageId       UUID FK → Message
  filename        VARCHAR(512)
  mimeType        VARCHAR(256)
  sizeBytes       BIGINT
  storageRef      VARCHAR(1024)
  encryptedDek    BYTEA

Rule
  id              UUID PK
  accountId       UUID FK → Account
  priority        INTEGER
  name            VARCHAR(255)
  condition       JSONB                -- {"field":"from","op":"contains","value":"@spam.com"}
  action          JSONB                -- {"type":"move","target":"Junk"}
  active          BOOLEAN

BlocklistEntry
  id              UUID PK
  type            VARCHAR(10)          -- IP | DOMAIN | CIDR
  value           VARCHAR(255)
  reason          TEXT
  addedBy         UUID FK → Account
  addedAt         TIMESTAMP
  expiresAt       TIMESTAMP            -- NULL = permanent

GreylistEntry
  id              UUID PK
  senderIp        INET
  senderDomain    VARCHAR(255)
  recipientAddr   VARCHAR(255)
  firstSeen       TIMESTAMP
  passedAt        TIMESTAMP            -- NULL = not yet passed
  retryCount      INTEGER

PipelineRoute
  id              UUID PK
  name            VARCHAR(255)
  definition      TEXT                 -- YAML route definition
  enabled         BOOLEAN
  updatedAt       TIMESTAMP

PluginConfig
  id              UUID PK
  pluginId        VARCHAR(255)
  config          JSONB
  updatedAt       TIMESTAMP

BackupJob
  id              UUID PK
  pluginId        VARCHAR(255)
  schedule        VARCHAR(128)         -- cron expression
  lastRunAt       TIMESTAMP
  nextRunAt       TIMESTAMP
  lastStatus      VARCHAR(32)          -- SUCCESS | FAILURE | RUNNING
  lastError       TEXT

SyncPeer
  id              UUID PK
  name            VARCHAR(255)
  url             VARCHAR(1024)        -- https://peer.example.com/api/sync
  authToken       VARCHAR(512)         -- encrypted bearer token
  active          BOOLEAN
  lastSyncAt      TIMESTAMP
```

---

## REST API Reference

Base path: `/api`. All endpoints return JSON. OpenAPI 3 spec at `/api/docs`.

### Authentication (`/api/auth`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/login` | Authenticate; returns JWT access token + refresh cookie |
| POST | `/auth/refresh` | Renew access token using httpOnly refresh cookie |
| POST | `/auth/logout` | Invalidate refresh token |

### Admin API (`/api/admin`) — requires `ROLE_ADMIN` or `ROLE_DOMAIN_ADMIN`

| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/admin/domains` | List / create domains |
| GET/PUT/DELETE | `/admin/domains/{id}` | Read / update / delete domain |
| GET/POST | `/admin/accounts` | List / create accounts |
| GET/PUT/DELETE | `/admin/accounts/{id}` | Read / update / delete account |
| GET/POST | `/admin/aliases` | List / create aliases |
| GET/PUT/DELETE | `/admin/aliases/{id}` | Read / update / delete alias |
| GET/POST | `/admin/mailboxes` | List / create mailboxes |
| GET/PUT/DELETE | `/admin/mailboxes/{id}` | Read / update / delete mailbox |
| GET | `/admin/plugins` | List installed plugins with status |
| POST | `/admin/plugins` | Upload plugin JAR (multipart) |
| PUT | `/admin/plugins/{id}/enable` | Enable plugin |
| PUT | `/admin/plugins/{id}/disable` | Disable plugin |
| GET/PUT | `/admin/plugins/{id}/config` | Read / save plugin JSON config |
| DELETE | `/admin/plugins/{id}` | Remove plugin |
| GET/POST | `/admin/pipeline` | List / create pipeline routes |
| GET/PUT/DELETE | `/admin/pipeline/{id}` | Read / update (hot-apply) / delete route |
| GET/POST | `/admin/blocklist` | List / add blocklist entries |
| DELETE | `/admin/blocklist/{id}` | Remove entry |
| GET | `/admin/greylist` | List greylist entries |
| DELETE | `/admin/greylist/{id}` | Remove entry |
| GET | `/admin/backup/jobs` | List backup jobs |
| POST | `/admin/backup/jobs` | Create backup job |
| PUT | `/admin/backup/jobs/{id}` | Update job |
| POST | `/admin/backup/jobs/{id}/run` | Trigger immediate backup |
| GET | `/admin/sync/peers` | List sync peers |
| POST | `/admin/sync/peers` | Add peer |
| DELETE | `/admin/sync/peers/{id}` | Remove peer |
| GET/PUT | `/admin/server/config` | Read / save server configuration |
| GET | `/admin/monitoring/health` | Health check |
| GET | `/admin/monitoring/metrics` | Prometheus metrics |

### Webmail API (`/api/webmail`) — requires `ROLE_USER`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/webmail/folders` | IMAP folder tree |
| POST | `/webmail/folders` | Create folder |
| DELETE | `/webmail/folders/{id}` | Delete folder |
| GET | `/webmail/messages` | List messages (paginated, filterable) |
| GET | `/webmail/messages/{id}` | Read message (decrypts on the fly) |
| PATCH | `/webmail/messages/{id}` | Update flags (read, starred, …) |
| DELETE | `/webmail/messages/{id}` | Move to Trash / expunge |
| GET | `/webmail/messages/{id}/attachments/{aid}` | Download attachment |
| POST | `/webmail/compose` | Send message |
| POST | `/webmail/compose/draft` | Save draft |
| GET/POST/PUT/DELETE | `/webmail/rules` | Manage server-side rules |
| GET/PUT | `/webmail/settings` | Per-user preferences |
| GET | `/webmail/search?q=` | Full-text search (via IndexPlugin if enabled) |

### Sync API (`/api/sync`) — mTLS secured, peer-to-peer

| Method | Path | Description |
|--------|------|-------------|
| POST | `/sync/events` | Receive MailEvent from peer |
| GET | `/sync/catchup?since=` | Pull events missed since timestamp |

---

## Security Design

### Transport Security
- TLS 1.3 enforced on all listener ports (SMTP 25/587/465, IMAP 143/993, POP3 110/995, HTTPS 443)
- Certificates loaded from PKCS12 keystore; path/password configured in `vektor.tls.*`
- Optional: Let's Encrypt ACME as a future AuthPlugin extension

### API Authentication
- JWT RS256 tokens; public/private key pair in `vektor.security.jwt.*`
- Access tokens: 15-minute TTL
- Refresh tokens: 30-day TTL, stored in httpOnly + Secure cookie
- Token revocation: refresh token ID stored in DB; checked on refresh

### Role-Based Access Control
| Role | Access |
|------|--------|
| `ROLE_ADMIN` | Full access to all admin endpoints |
| `ROLE_DOMAIN_ADMIN` | Admin endpoints scoped to own domain only |
| `ROLE_USER` | Webmail API for own account only |

### Encryption at Rest
```
Per-message DEK generation:
  1. Generate random 256-bit AES key (DEK)
  2. Encrypt message content with AES-256-GCM (random IV, authenticated)
  3. Wrap DEK with server Master Key using AES-256-GCM
  4. Store encrypted DEK alongside message record in DB
  5. On read: unwrap DEK → decrypt content on the fly

Master Key:
  - Loaded from env var VEKTOR_MASTER_KEY (base64-encoded 256-bit key)
  - In Kubernetes: mounted from Secret
  - Future: HSM or external KMS via AuthPlugin
```

### Mail Authentication
- **DKIM**: Ed25519 signing of outgoing mail; public key published in DNS
- **SPF**: DNS TXT lookup on incoming SMTP; result annotated in headers
- **DMARC**: DNS TXT lookup; policy enforced based on domain DMARC record
- **DANE/TLSA**: Optional future extension

---

## Multi-Instance Synchronisation

### Design
- Each instance runs a `SyncPlugin` that publishes and consumes `MailEvent` payloads
- Sync peers configured in admin UI (`SyncPeer` records)
- Communication: HTTPS POST to `/api/sync/events` with bearer token auth, mTLS optional

### Event Types
```java
sealed interface MailEvent permits
    MessageStoredEvent,
    MessageFlagsUpdatedEvent,
    MessageDeletedEvent,
    MailboxCreatedEvent,
    MailboxDeletedEvent,
    AccountUpdatedEvent {}
```

### Consistency Model
- **Eventual consistency** — no distributed transaction coordinator
- Each event carries a `vectorTimestamp` (per-account logical clock)
- On conflict (same message, diverging flag states): last-write-wins on `vectorTimestamp`
- Missing events recovered via `GET /api/sync/catchup?since=<timestamp>` (periodic Camel timer)

---

## Backup Design

### Schedule
Configured per `BackupJob` record with cron expression (e.g., `0 2 * * *` for daily at 02:00).
Spring `@Scheduled` triggers the active `BackupPlugin`.

### Backup Content
1. PostgreSQL dump (`pg_dump`) or Spring Data export for embedded DB
2. Mail storage directory (Maildir / Mbox paths)
3. Plugin JARs + `PluginConfig` records
4. TLS certificates + DKIM keys (encrypted)
5. Pipeline route definitions

### Backup Backends (BackupPlugin implementations)
| Backend | Description |
|---------|-------------|
| `LocalBackupPlugin` | Writes encrypted archive to a local path |
| `S3BackupPlugin` | Uploads to any S3-compatible endpoint (AWS S3, MinIO) |
| `SftpBackupPlugin` | Transfers via SFTP to remote server |

### Encryption
- Full backup archive encrypted with AES-256-GCM using master key before writing
- Archive format: `vektor-backup-{date}.tar.gz.enc`

### Retention
- Configurable keep count per job (e.g., keep last 7 daily, 4 weekly, 12 monthly)
- Expired backups auto-deleted by the plugin after successful new backup

---

## Frontend (vektor-frontend)

### Structure
```
vektor-frontend/
├── src/
│   ├── admin/           # Admin SPA
│   │   ├── pages/       # Domains, Accounts, Plugins, Pipeline, ...
│   │   └── components/
│   │       └── PipelineEditor.tsx   # React Flow visual editor
│   ├── webmail/         # Webmail SPA
│   │   ├── pages/       # Inbox, Compose, Settings, ...
│   │   └── components/
│   ├── shared/          # Shared components, hooks, API clients
│   └── main.tsx         # Entry point, React Router setup
├── vite.config.ts
└── package.json
```

### Pipeline Visual Editor
- Built with **React Flow**
- Nodes represent pipeline steps (Filter, Processor, Router)
- Edges define message flow
- Node properties panel: configure plugin ID, thresholds, actions
- Save serializes to YAML and calls `PUT /api/admin/pipeline/{id}`
- Load deserializes YAML to React Flow node/edge graph

### Build Integration
Maven `frontend-maven-plugin` in `vektor-frontend/pom.xml`:
1. Installs Node.js + npm
2. Runs `npm install` + `vite build`
3. Copies `dist/` to `vektor-launcher/src/main/resources/static/`

In production, a reverse-proxy (Nginx or Ingress) can serve static assets directly.

---

## Deployment

### Docker Compose (Primary)

```yaml
# docker-compose.yml
services:
  vektor-mail:
    image: vektor/vektor-mail:latest
    ports:
      - "25:25"     # SMTP
      - "587:587"   # SMTP submission
      - "465:465"   # SMTPS
      - "143:143"   # IMAP
      - "993:993"   # IMAPS
      - "110:110"   # POP3
      - "995:995"   # POP3S
      - "8080:8080" # HTTP (admin + webmail)
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/vektor
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      VEKTOR_MASTER_KEY: ${VEKTOR_MASTER_KEY}
      VEKTOR_TLS_KEYSTORE_PATH: /certs/keystore.p12
    volumes:
      - mail-data:/var/vektor/mail
      - ./plugins:/var/vektor/plugins
      - ./certs:/certs:ro
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: vektor
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mail-data:
  postgres-data:
```

Optional `--profile monitoring` adds Prometheus + Grafana containers.

### Kubernetes / Helm Chart

```
vektor-distribution/helm/vektor-mail/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── deployment.yaml          # vektor-mail pods
    ├── service.yaml             # ClusterIP for HTTP; LoadBalancer for mail ports
    ├── ingress.yaml             # HTTPS for admin/webmail UI
    ├── configmap.yaml           # application.yml config
    ├── secret.yaml              # master key, DB credentials, JWT keys
    ├── pvc.yaml                 # mail storage volume
    └── hpa.yaml                 # HorizontalPodAutoscaler
```

Key `values.yaml` knobs:
```yaml
replicaCount: 2
image.tag: latest
storage.size: 50Gi
smtp.loadBalancerIP: ""
tls.existingSecret: vektor-tls
masterKey.existingSecret: vektor-secrets
database.external: false          # true = use external PostgreSQL
```

---

## Configuration Reference

`application.yml` (loaded by `vektor-launcher`):

```yaml
vektor:
  plugins:
    dir: ${VEKTOR_PLUGINS_DIR:./plugins}
  tls:
    keystore-path: ${VEKTOR_TLS_KEYSTORE_PATH:./certs/keystore.p12}
    keystore-password: ${VEKTOR_TLS_KEYSTORE_PASSWORD}
  security:
    master-key: ${VEKTOR_MASTER_KEY}
    jwt:
      private-key: ${VEKTOR_JWT_PRIVATE_KEY}
      public-key: ${VEKTOR_JWT_PUBLIC_KEY}
  smtp:
    port: 25
    submission-port: 587
    smtps-port: 465
    hostname: ${VEKTOR_HOSTNAME:localhost}
  imap:
    port: 143
    imaps-port: 993
  pop3:
    port: 110
    pop3s-port: 995
  storage:
    active-plugin: ${VEKTOR_STORAGE_PLUGIN:vektor-storage-db}
  backup:
    enabled: true
  sync:
    enabled: false
```

---

## Testing Strategy

| Layer | Approach |
|-------|---------|
| Unit | JUnit 5 + Mockito; one test class per service/plugin |
| Integration | Testcontainers (PostgreSQL, mock SMTP) per module |
| Protocol | RFC compliance tests using standard IMAP/SMTP client libraries |
| Pipeline | Camel test DSL (`CamelTestSupport`) for route unit testing |
| Frontend | Vitest + React Testing Library for components |
| End-to-end | Docker Compose test stack; `swaks` for SMTP, `imaptest` for IMAP |
| Security | OWASP dependency-check, SpotBugs Security plugin |

---

## Non-Functional Requirements

| Requirement | Target |
|-------------|--------|
| Throughput | ≥ 100 messages/second per instance (virtual threads) |
| Latency | SMTP DATA acceptance < 500 ms (excluding spam scan) |
| Availability | ≥ 99.9% with multi-instance deployment |
| Storage | Configurable per-account quota, enforced at SMTP DATA |
| Scalability | Horizontal via Kubernetes; storage must be on shared PVC or external |
| Observability | Prometheus metrics, structured JSON logs (Logback), Actuator health |
| Compliance | RFC 5321 (SMTP), RFC 9051 (IMAP4rev2), RFC 1939 (POP3) |
| Upgrade path | Flyway migrations; zero-downtime rolling upgrade on K8s |

---

## Open / Future Extensions

- **ACME / Let's Encrypt** plugin for automatic TLS certificate renewal
- **LDAP / Active Directory** AuthPlugin
- **CalDAV / CardDAV** server module
- **S/MIME and PGP** encryption plugin (end-to-end)
- **Milter protocol** adapter for external content filters
- **Push notifications** (IMAP PUSH / Jmap) for mobile clients
- **Multi-tenancy** enhancements with per-domain billing
