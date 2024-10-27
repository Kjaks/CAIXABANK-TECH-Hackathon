DROP DATABASE IF EXISTS bankingapp;

CREATE DATABASE bankingapp;

USE bankingapp;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  phone_number VARCHAR(255) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  hashedPassword VARCHAR(255) DEFAULT NULL
);

CREATE TABLE accounts (
    account_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(36) NOT NULL UNIQUE,
    balance DECIMAL(10, 2) DEFAULT 0.00 NOT NULL,
    pin VARCHAR(4) DEFAULT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    transaction_type ENUM('CASH_TRANSFER', 'CASH_WITHDRAWAL', 'CASH_DEPOSIT', 'SUBSCRIPTION', 'ASSET_PURCHASE', 'ASSET_SELL') NOT NULL,
    transaction_date BIGINT NOT NULL,
    source_account_number VARCHAR(36) NOT NULL,
    target_account_number VARCHAR(36) DEFAULT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE account_assets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    asset_symbol VARCHAR(10) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    purchase_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);