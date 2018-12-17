/* Replace with your SQL commands */

CREATE TABLE accounts (
       id SERIAL PRIMARY KEY,
       email CITEXT NOT NULL UNIQUE,
       username CITEXT NOT NULL UNIQUE,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX account_email ON accounts (email);

CREATE TRIGGER update_accounts_updated_at
BEFORE UPDATE
ON accounts
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
