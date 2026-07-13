-- Optimistic locking for credit accounts to prevent double-charge races
ALTER TABLE credit_accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Optimistic locking for payments to make confirmation idempotent under concurrency
ALTER TABLE payments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
