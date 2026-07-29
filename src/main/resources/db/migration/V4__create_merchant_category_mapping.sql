-- Table: merchant_category_mappings
CREATE TABLE merchant_category_mappings (
    id BIGSERIAL PRIMARY KEY,
    merchant_pattern VARCHAR(255) NOT NULL UNIQUE,
    category_id BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_mapping_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_merchant_mappings_pattern ON merchant_category_mappings(merchant_pattern);

-- Seed data for common merchants (using MANUAL source)
INSERT INTO merchant_category_mappings (merchant_pattern, category_id, source, created_at)
SELECT 'MAGNUM', id, 'MANUAL', NOW() FROM categories WHERE name = 'Groceries'
UNION ALL
SELECT 'SMALL', id, 'MANUAL', NOW() FROM categories WHERE name = 'Groceries'
UNION ALL
SELECT 'WOLT', id, 'MANUAL', NOW() FROM categories WHERE name = 'Groceries'
UNION ALL
SELECT 'GLOVO', id, 'MANUAL', NOW() FROM categories WHERE name = 'Groceries'
UNION ALL
SELECT 'ONAY', id, 'MANUAL', NOW() FROM categories WHERE name = 'Transport'
UNION ALL
SELECT 'YANDEX.GO', id, 'MANUAL', NOW() FROM categories WHERE name = 'Transport'
UNION ALL
SELECT 'YANDEX.TAXI', id, 'MANUAL', NOW() FROM categories WHERE name = 'Transport'
UNION ALL
SELECT 'UBER', id, 'MANUAL', NOW() FROM categories WHERE name = 'Transport'
UNION ALL
SELECT 'GOOGLE', id, 'MANUAL', NOW() FROM categories WHERE name = 'Subscriptions'
UNION ALL
SELECT 'APPLE.COM/BILL', id, 'MANUAL', NOW() FROM categories WHERE name = 'Subscriptions'
UNION ALL
SELECT 'NETFLIX', id, 'MANUAL', NOW() FROM categories WHERE name = 'Subscriptions'
UNION ALL
SELECT 'SPOTIFY', id, 'MANUAL', NOW() FROM categories WHERE name = 'Subscriptions'
UNION ALL
SELECT 'WORKOUTGYM', id, 'MANUAL', NOW() FROM categories WHERE name = 'Other'
UNION ALL
SELECT 'KCELL', id, 'MANUAL', NOW() FROM categories WHERE name = 'Communication'
UNION ALL
SELECT 'TELE2', id, 'MANUAL', NOW() FROM categories WHERE name = 'Communication'
UNION ALL
SELECT 'BEELINE', id, 'MANUAL', NOW() FROM categories WHERE name = 'Communication'
UNION ALL
SELECT 'ALTEL', id, 'MANUAL', NOW() FROM categories WHERE name = 'Communication'
UNION ALL
SELECT 'KAZAKHTELECOM', id, 'MANUAL', NOW() FROM categories WHERE name = 'Utilities'
UNION ALL
SELECT 'ALSECO', id, 'MANUAL', NOW() FROM categories WHERE name = 'Utilities'
UNION ALL
SELECT 'ASTANAAERC', id, 'MANUAL', NOW() FROM categories WHERE name = 'Utilities'
UNION ALL
SELECT 'KINO.KZ', id, 'MANUAL', NOW() FROM categories WHERE name = 'Entertainment'
UNION ALL
SELECT 'TICKETON', id, 'MANUAL', NOW() FROM categories WHERE name = 'Entertainment'
ON CONFLICT (merchant_pattern) DO NOTHING;
