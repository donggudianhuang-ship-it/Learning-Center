USE smart_learning;

CREATE TABLE IF NOT EXISTS ai_grading_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    subject VARCHAR(50),
    grade VARCHAR(50),
    total_score DECIMAL(7,2) DEFAULT 0,
    max_score DECIMAL(7,2) DEFAULT 0,
    accuracy_rate DECIMAL(7,2) DEFAULT 0,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    result_json LONGTEXT NOT NULL,
    original_file_name VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    INDEX idx_ai_grading_record_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
