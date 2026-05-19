-- V1: Initial Vektor Mail schema

CREATE TABLE IF NOT EXISTS domains (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL UNIQUE,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    dkim_private_key BYTEA,
    dkim_selector    VARCHAR(64),
    spf_policy       VARCHAR(255),
    dmarc_policy     VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS accounts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    domain_id     UUID NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    quota_bytes   BIGINT NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS account_roles (
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    role       VARCHAR(64) NOT NULL,
    PRIMARY KEY (account_id, role)
);

CREATE TABLE IF NOT EXISTS mailboxes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id   UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    path         VARCHAR(1024) NOT NULL,
    uid_validity BIGINT NOT NULL DEFAULT 1,
    uid_next     BIGINT NOT NULL DEFAULT 1,
    UNIQUE (account_id, path)
);

CREATE TABLE IF NOT EXISTS mailbox_flags (
    mailbox_id UUID NOT NULL REFERENCES mailboxes(id) ON DELETE CASCADE,
    flag       VARCHAR(64) NOT NULL,
    PRIMARY KEY (mailbox_id, flag)
);

CREATE TABLE IF NOT EXISTS messages (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id    VARCHAR(512),
    mailbox_id    UUID NOT NULL REFERENCES mailboxes(id) ON DELETE CASCADE,
    subject       VARCHAR(1024),
    from_address  VARCHAR(512),
    to_addresses  TEXT,
    received_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    size_bytes    BIGINT NOT NULL DEFAULT 0,
    storage_ref   VARCHAR(1024),
    encrypted_dek BYTEA,
    spam_score    NUMERIC(5,2),
    dkim_result   VARCHAR(32),
    spf_result    VARCHAR(32),
    uid           BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_messages_mailbox_id ON messages(mailbox_id);
CREATE INDEX IF NOT EXISTS idx_messages_received_at ON messages(received_at);

CREATE TABLE IF NOT EXISTS message_flags (
    message_id_fk UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    flag          VARCHAR(64) NOT NULL,
    PRIMARY KEY (message_id_fk, flag)
);

CREATE TABLE IF NOT EXISTS message_contents (
    message_id UUID PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
    content    BYTEA NOT NULL
);

CREATE TABLE IF NOT EXISTS attachments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id    UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    filename      VARCHAR(512),
    mime_type     VARCHAR(256),
    size_bytes    BIGINT NOT NULL DEFAULT 0,
    storage_ref   VARCHAR(1024),
    encrypted_dek BYTEA
);

CREATE TABLE IF NOT EXISTS aliases (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_address VARCHAR(255) NOT NULL,
    target_address VARCHAR(255) NOT NULL,
    domain_id      UUID NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS rules (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    priority   INTEGER NOT NULL DEFAULT 100,
    name       VARCHAR(255),
    condition  TEXT,
    action     TEXT,
    active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS blocklist_entries (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type      VARCHAR(10) NOT NULL CHECK (type IN ('IP','DOMAIN','CIDR')),
    value     VARCHAR(255) NOT NULL,
    reason    TEXT,
    added_by  UUID REFERENCES accounts(id) ON DELETE SET NULL,
    added_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_blocklist_value ON blocklist_entries(value);

CREATE TABLE IF NOT EXISTS greylist_entries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_ip      VARCHAR(45),
    sender_domain  VARCHAR(255),
    recipient_addr VARCHAR(255),
    first_seen     TIMESTAMP WITH TIME ZONE NOT NULL,
    passed_at      TIMESTAMP WITH TIME ZONE,
    retry_count    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pipeline_routes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    definition TEXT NOT NULL,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS plugin_configs (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id VARCHAR(255) NOT NULL UNIQUE,
    config    TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS backup_jobs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id   VARCHAR(255) NOT NULL,
    schedule    VARCHAR(128) NOT NULL,
    last_run_at TIMESTAMP WITH TIME ZONE,
    next_run_at TIMESTAMP WITH TIME ZONE,
    last_status VARCHAR(32) NOT NULL DEFAULT 'IDLE',
    last_error  TEXT,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sync_peers (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    url          VARCHAR(1024) NOT NULL,
    auth_token   VARCHAR(512),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at TIMESTAMP WITH TIME ZONE
);

-- Refresh tokens for JWT invalidation
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(512) NOT NULL UNIQUE,
    issued_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE
);
