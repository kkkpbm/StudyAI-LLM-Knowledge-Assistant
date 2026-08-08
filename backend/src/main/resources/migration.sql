-- =====================================================
-- Database Migration Script
-- Run this ONCE to update existing tables to match schema.sql
-- =====================================================

-- 9. chat_history: 新建聊天记录表
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    note_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 1. learning_records: rename old columns to match entity
ALTER TABLE learning_records
    CHANGE COLUMN duration_minutes duration_minutes INT DEFAULT 0;
-- (This is a no-op; keep old column name, entity maps via @TableField)
-- If you want to migrate to new schema, run these instead:
-- ALTER TABLE learning_records CHANGE COLUMN duration_minutes duration_seconds INT DEFAULT 0;
-- ALTER TABLE learning_records CHANGE COLUMN date recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP;

-- 2. notes: add missing columns
ALTER TABLE notes
    ADD COLUMN IF NOT EXISTS content_plain TEXT AFTER content_md,
    ADD COLUMN IF NOT EXISTS summary VARCHAR(500) AFTER content_plain,
    ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0 AFTER difficulty_level;

-- 3. categories: add missing columns
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS description VARCHAR(200) AFTER name,
    ADD COLUMN IF NOT EXISTS parent_id BIGINT DEFAULT 0 AFTER color,
    ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0 AFTER parent_id;

-- 4. tags: add missing color column
ALTER TABLE tags
    ADD COLUMN IF NOT EXISTS color VARCHAR(20) DEFAULT '#2563EB' AFTER name;

-- 5. learning_plans: add missing columns
ALTER TABLE learning_plans
    ADD COLUMN IF NOT EXISTS goal VARCHAR(500) AFTER description,
    ADD COLUMN IF NOT EXISTS start_date DATE AFTER goal,
    ADD COLUMN IF NOT EXISTS end_date DATE AFTER start_date,
    ADD COLUMN IF NOT EXISTS ai_generated TINYINT DEFAULT 0 AFTER end_date;

-- 6. review_reminders: rename repetitions to match entity
ALTER TABLE review_reminders
    CHANGE COLUMN IF EXISTS repetitions repetition_count INT DEFAULT 0;

-- 7. knowledge_relations: drop and recreate with correct columns (table is unused, safe to drop)
DROP TABLE IF EXISTS knowledge_relations;
CREATE TABLE IF NOT EXISTS knowledge_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_note_id BIGINT NOT NULL,
    target_note_id BIGINT NOT NULL,
    relation_type VARCHAR(50) DEFAULT 'related',
    weight DOUBLE DEFAULT 0.5,
    ai_generated TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_note_id) REFERENCES notes(id),
    FOREIGN KEY (target_note_id) REFERENCES notes(id)
);

-- 8. plan_items: drop and recreate with correct columns (table is unused)
DROP TABLE IF EXISTS plan_items;
CREATE TABLE IF NOT EXISTS plan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    note_id BIGINT,
    order_num INT DEFAULT 0,
    estimated_minutes INT DEFAULT 30,
    completed TINYINT DEFAULT 0,
    completed_at DATETIME,
    FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    FOREIGN KEY (note_id) REFERENCES notes(id)
);
