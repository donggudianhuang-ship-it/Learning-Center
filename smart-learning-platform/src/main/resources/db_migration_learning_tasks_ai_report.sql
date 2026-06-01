USE smart_learning;

CREATE TABLE IF NOT EXISTS learning_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_key VARCHAR(120) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    knowledge_id BIGINT,
    knowledge_name VARCHAR(100),
    question_ids TEXT,
    action_route VARCHAR(50),
    priority INT DEFAULT 1,
    estimated_minutes INT DEFAULT 15,
    progress DECIMAL(5,2) DEFAULT 0,
    target_progress DECIMAL(5,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    deadline DATE,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_point(id),
    UNIQUE KEY uk_learning_task_user_key (user_id, task_key),
    INDEX idx_learning_task_user_status (user_id, status, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'ai_grading_record' AND column_name = 'ocr_text'),
    'SELECT 1',
    'ALTER TABLE ai_grading_record ADD COLUMN ocr_text LONGTEXT NULL AFTER result_json'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'ai_grading_record' AND column_name = 'status'),
    'SELECT 1',
    'ALTER TABLE ai_grading_record ADD COLUMN status VARCHAR(20) DEFAULT ''SUCCESS'' AFTER original_file_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'ai_grading_record' AND column_name = 'error_message'),
    'SELECT 1',
    'ALTER TABLE ai_grading_record ADD COLUMN error_message TEXT NULL AFTER status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'ai_grading_record' AND column_name = 'updated_at'),
    'SELECT 1',
    'ALTER TABLE ai_grading_record ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_grading_record
SET status = COALESCE(status, 'SUCCESS')
WHERE status IS NULL;
