--liquibase formatted sql

--changeset dev:1
CREATE TABLE wallet (
    id UUID PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    version INTEGER NOT NULL DEFAULT 0
);
COMMENT ON TABLE wallet IS 'Кошельки пользователей';
COMMENT ON COLUMN wallet.version IS 'Версия для оптимистичной блокировки';
--rollback DROP TABLE wallet;

--changeset dev:2
INSERT INTO wallet (id, balance, currency, version)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 100000, 'RUB', 0),
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 50000, 'USD', 0);

-- rollback DELETE FROM wallet WHERE id IN (
--   'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
--   'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22'
-- );