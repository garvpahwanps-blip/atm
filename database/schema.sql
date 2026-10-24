-- MySQL 8.0.16+ / 8.4. Run as a database administrator, then grant the
-- application user SELECT, INSERT, UPDATE on secure_atm.* (no DDL or DELETE).
CREATE DATABASE IF NOT EXISTS secure_atm CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE secure_atm;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    account_number CHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    pin_hash VARCHAR(255) CHARACTER SET ascii NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_account_number UNIQUE (account_number),
    CONSTRAINT ck_account_number CHECK (account_number REGEXP '^[0-9]{12}$'),
    CONSTRAINT ck_failed_attempts CHECK (failed_attempts BETWEEN 0 AND 3)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    account_type VARCHAR(20) NOT NULL DEFAULT 'SAVINGS',
    CONSTRAINT uq_accounts_user UNIQUE (user_id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT ck_balance CHECK (balance >= 0),
    CONSTRAINT ck_account_type CHECK (account_type IN ('SAVINGS'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bank_transactions (
    transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    counterparty_account_number CHAR(12) CHARACTER SET ascii NULL,
    reference_number VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    balance_after DECIMAL(19,2) NOT NULL,
    notes VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT uq_transaction_reference UNIQUE (reference_number),
    CONSTRAINT ck_transaction_type CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_OUT','TRANSFER_IN')),
    CONSTRAINT ck_transaction_amount CHECK (amount > 0),
    CONSTRAINT ck_transaction_balance CHECK (balance_after >= 0),
    INDEX ix_transactions_recent (account_id, created_at DESC, transaction_id DESC)
) ENGINE=InnoDB;
