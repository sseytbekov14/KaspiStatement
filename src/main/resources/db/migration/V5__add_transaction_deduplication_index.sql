-- Add a unique constraint to prevent duplicate transactions from overlapping statements
ALTER TABLE transactions
    ADD CONSTRAINT uk_transaction_dedup UNIQUE (date, amount, merchant_details, operation_type);
