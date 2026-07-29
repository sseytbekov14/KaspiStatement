-- Table: categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Insert initial categories
INSERT INTO categories (name) VALUES
    ('Groceries'),
    ('Transport'),
    ('Communication'),
    ('Entertainment'),
    ('Transfers'),
    ('Other');

-- Table: statements
CREATE TABLE statements (
    id BIGSERIAL PRIMARY KEY,
    file_hash VARCHAR(64) NOT NULL UNIQUE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Table: transactions
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    statement_id BIGINT NOT NULL,
    date DATE NOT NULL,
    sign VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    merchant_details TEXT NOT NULL,
    category_id BIGINT,

    -- Foreign keys
    CONSTRAINT fk_transaction_statement
        FOREIGN KEY (statement_id)
        REFERENCES statements(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE SET NULL
);

-- Create indices for foreign keys and frequent lookups
CREATE INDEX idx_transactions_statement_id ON transactions(statement_id);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
