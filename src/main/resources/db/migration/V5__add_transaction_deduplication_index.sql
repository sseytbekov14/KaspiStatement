-- Clean up existing duplicate transactions before applying unique constraint
DELETE FROM transactions
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
        ROW_NUMBER() OVER( PARTITION BY date, amount, merchant_details, operation_type ORDER BY id ) as row_num
        FROM transactions
    ) t
    WHERE t.row_num > 1
);

-- Add a unique constraint to prevent duplicate transactions from overlapping statements
ALTER TABLE transactions
    ADD CONSTRAINT uk_transaction_dedup UNIQUE (date, amount, merchant_details, operation_type);
