-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS knowledge_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_assistant;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
);

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#10B981',
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 笔记表
CREATE TABLE IF NOT EXISTS notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content_md TEXT,
    category_id BIGINT,
    difficulty_level VARCHAR(20) DEFAULT 'medium',
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 笔记标签关联表
CREATE TABLE IF NOT EXISTS note_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    FOREIGN KEY (note_id) REFERENCES notes(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id),
    UNIQUE KEY (note_id, tag_id)
);

-- 学习计划表
CREATE TABLE IF NOT EXISTS learning_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    goal VARCHAR(500),
    start_date DATE,
    end_date DATE,
    user_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    ai_generated TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 计划项表
CREATE TABLE IF NOT EXISTS plan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    note_id BIGINT,
    order_num INT DEFAULT 0,
    estimated_minutes INT DEFAULT 0,
    completed TINYINT DEFAULT 0,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    FOREIGN KEY (note_id) REFERENCES notes(id)
);

-- 学习记录表
CREATE TABLE IF NOT EXISTS learning_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT,
    duration_minutes INT DEFAULT 0,
    date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (note_id) REFERENCES notes(id)
);

-- 复习提醒表
CREATE TABLE IF NOT EXISTS review_reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    next_review_at DATE NOT NULL,
    interval_days INT DEFAULT 1,
    ease_factor DECIMAL(4,2) DEFAULT 2.5,
    repetitions INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (note_id) REFERENCES notes(id)
);

-- 知识关系表
CREATE TABLE IF NOT EXISTS knowledge_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    source VARCHAR(100) NOT NULL,
    target VARCHAR(100) NOT NULL,
    relation_type VARCHAR(50) DEFAULT 'related',
    weight DECIMAL(3,2) DEFAULT 0.5,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES notes(id)
);

-- 聊天记录表
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    note_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 插入默认分类数据
INSERT IGNORE INTO categories (id, name, color, user_id) VALUES
(1, '前端', '#2563EB', 1),
(2, '后端', '#7C3AED', 1),
(3, 'AI', '#10B981', 1),
(4, '基础', '#F59E0B', 1);

-- 插入默认标签数据
INSERT IGNORE INTO tags (id, name, user_id) VALUES
(1, 'Vue', 1),
(2, 'React', 1),
(3, 'Python', 1),
(4, 'TypeScript', 1),
(5, '机器学习', 1);

-- 兼容已有数据库：补充缺失字段
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，依赖 continue-on-error 跳过重复列错误
ALTER TABLE learning_plans ADD COLUMN goal VARCHAR(500);
ALTER TABLE learning_plans ADD COLUMN start_date DATE;
ALTER TABLE learning_plans ADD COLUMN end_date DATE;
ALTER TABLE learning_plans ADD COLUMN ai_generated TINYINT DEFAULT 0;
ALTER TABLE plan_items ADD COLUMN description TEXT;
ALTER TABLE plan_items ADD COLUMN note_id BIGINT;
ALTER TABLE plan_items ADD COLUMN estimated_minutes INT DEFAULT 0;
ALTER TABLE plan_items ADD COLUMN completed_at DATETIME;

-- AI 闪卡与测验
CREATE TABLE IF NOT EXISTS flashcards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    card_type VARCHAR(20) DEFAULT 'qa',
    options_json TEXT,
    next_review_at DATE DEFAULT (CURRENT_DATE),
    interval_days INT DEFAULT 1,
    ease_factor DECIMAL(4,2) DEFAULT 2.5,
    repetitions INT DEFAULT 0,
    last_quality INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (note_id) REFERENCES notes(id)
);

CREATE TABLE IF NOT EXISTS flashcard_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flashcard_id BIGINT NOT NULL,
    quality INT NOT NULL,
    correct TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (flashcard_id) REFERENCES flashcards(id)
);

-- 笔记历史版本与回收站快照
CREATE TABLE IF NOT EXISTS note_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_md LONGTEXT,
    category_id BIGINT,
    difficulty_level VARCHAR(20),
    version_no INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS note_trash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_md LONGTEXT,
    category_id BIGINT,
    difficulty_level VARCHAR(20),
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 文档导入解析工作流：解析完成后需由用户确认才会创建笔记
CREATE TABLE IF NOT EXISTS document_workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(40),
    file_size BIGINT DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    draft_json LONGTEXT,
    error_message VARCHAR(500),
    note_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (note_id) REFERENCES notes(id),
    INDEX idx_document_workflows_user_status (user_id, status)
);
