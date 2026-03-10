-- ----------------------------------------------------------------------------
-- Base sample data
-- ----------------------------------------------------------------------------
MERGE INTO clients (id, name, biz_no, type, status, tier)
KEY(id)
VALUES
    (11, 'ACME Tax Co.', '123-45-67890', 'CORPORATE', 'ACTIVE', 'VIP'),
    (12, 'Blue Ledger Ltd.', '234-56-78901', 'CORPORATE', 'ACTIVE', 'PREMIUM');

MERGE INTO work_items (id, client_id, biz_no, type, status, assignee, due_date, tags_json, memo, version, updated_at)
KEY(id)
VALUES
    (101, 11, '123-45-67890', 'FILING', 'TODO', 'kim', DATEADD('DAY', 7, CURRENT_DATE), '["vip","urgent"]', 'Sample seed item', 1, CURRENT_TIMESTAMP),
    (102, 12, '234-56-78901', 'BOOKKEEPING', 'IN_PROGRESS', 'lee', DATEADD('DAY', 5, CURRENT_DATE), '["monthly"]', 'Sample seed item 2', 1, CURRENT_TIMESTAMP);

-- Large seed data is loaded asynchronously after server startup.
