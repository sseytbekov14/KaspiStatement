-- Insert additional categories for Milestone 5
INSERT INTO categories (name) VALUES
    ('Cash Withdrawal'),
    ('Utilities'),
    ('Subscriptions')
ON CONFLICT (name) DO NOTHING;
