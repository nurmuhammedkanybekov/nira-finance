-- Requires the pgvector extension (https://github.com/pgvector/pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    type        VARCHAR(30)  NOT NULL, -- CHECKING, SAVINGS, INVESTMENT, CASH, CREDIT_CARD
    currency    VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    parent_id   BIGINT REFERENCES category(id) ON DELETE SET NULL,
    is_income   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE transaction (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    account_id      BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    category_id     BIGINT REFERENCES category(id) ON DELETE SET NULL,
    amount          NUMERIC(14,2) NOT NULL, -- negative = expense, positive = income
    currency        VARCHAR(3) NOT NULL DEFAULT 'EUR',
    description     VARCHAR(500),
    merchant        VARCHAR(255),
    occurred_on     DATE NOT NULL,
    source          VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL, CSV_IMPORT
    ai_categorized  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_user_date ON transaction(user_id, occurred_on);
CREATE INDEX idx_transaction_category ON transaction(category_id);

CREATE TABLE budget (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    category_id  BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    month        DATE NOT NULL, -- first day of month
    limit_amount NUMERIC(14,2) NOT NULL,
    UNIQUE(user_id, category_id, month)
);

CREATE TABLE holding (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    account_id  BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    ticker      VARCHAR(20) NOT NULL,
    shares      NUMERIC(18,6) NOT NULL,
    cost_basis  NUMERIC(14,2) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- RAG: one embedding row per "chunk" of financial context (e.g. a month's
-- worth of transactions summarized, or a single large transaction).
CREATE TABLE finance_embedding (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    source_type VARCHAR(30) NOT NULL, -- TRANSACTION, MONTHLY_SUMMARY, BUDGET
    source_id   BIGINT,
    content     TEXT NOT NULL,        -- the human-readable text that was embedded
    embedding   vector(1536) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_finance_embedding_user ON finance_embedding(user_id);
